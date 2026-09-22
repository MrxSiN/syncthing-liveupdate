package my.MrxSiN.syncthingliveupdate;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

/**
 * Gives the host's status bar chip Syncthing's colours inside SystemUI.
 *
 * Android 17 always paints a notification chip in the system surface colour and at
 * most tints its text. The chip is instead filled with the primary colour and its
 * content drawn in the matching on-primary colour, the same pair the progress
 * tracker uses, so the chip and the notification read as one Live Update. Chips of
 * every other app keep the system colours.
 */
final class StatusBarChipColors {

    private static final String HOST_KEY_PART = "|" + HostApp.PACKAGE + "|";

    private final Map<SyncthingPalette, Object> colors = new ConcurrentHashMap<>();

    private Class<?> customColors;

    /** Installs the hook. Returns false when the chip model is not present. */
    boolean install(ClassLoader systemUiClassLoader) {
        Class<?> chip = Reflect.findClass(systemUiClassLoader, SystemUi.ACTIVE_CHIP);
        Class<?> colorsModel = Reflect.findClass(systemUiClassLoader, SystemUi.CHIP_COLORS);
        customColors = Reflect.findClass(systemUiClassLoader, SystemUi.CUSTOM_CHIP_COLORS);

        int hooks = 0;
        if (chip != null && colorsModel != null && customColors != null) {
            for (Constructor<?> constructor : chip.getDeclaredConstructors()) {
                /*
                 * The index is captured per constructor. SystemUI may expose several
                 * constructors that take a colours model, and nothing says they agree
                 * on where it sits, so a single shared index would let one hook
                 * rewrite the wrong argument of another constructor.
                 */
                int index = List.of(constructor.getParameterTypes()).indexOf(colorsModel);
                if (index >= 0
                        && ModuleRuntime.hook(constructor, chain -> intercept(chain, index))
                        != null) {
                    hooks++;
                }
            }
        }
        if (hooks == 0) {
            ModuleRuntime.log("Chip colours unavailable; the chip keeps the system colours");
            return false;
        }
        ModuleRuntime.log("Colouring the host status bar chip in " + SystemUi.PACKAGE);
        return true;
    }

    private Object intercept(XposedInterface.Chain chain, int index) throws Throwable {
        Object[] args = chain.getArgs().toArray();
        if (index >= args.length || !belongsToHost(args)) {
            return chain.proceed();
        }
        Object hostColors = colorsFor(currentPalette());
        if (hostColors == null) {
            return chain.proceed();
        }
        args[index] = hostColors;
        return chain.proceed(args);
    }

    private static boolean belongsToHost(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String key && key.contains(HOST_KEY_PART)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The chip colours for {@code palette}. SystemUI builds them without a
     * constructor, so the instance is allocated and its two values written directly.
     */
    private Object colorsFor(SyncthingPalette palette) {
        return colors.computeIfAbsent(palette, key -> {
            Object instance = Reflect.allocate(customColors);
            boolean written = instance != null
                    && Reflect.writeField(instance, SystemUi.CUSTOM_BACKGROUND_FIELD, key.primary())
                    && Reflect.writeField(instance, SystemUi.CUSTOM_TEXT_FIELD, key.onPrimary());
            return written ? instance : null;
        });
    }

    private static SyncthingPalette currentPalette() {
        return Reflect.currentApplication() instanceof Context context
                ? SyncthingPalette.of(context)
                : SyncthingPalette.DARK;
    }
}
