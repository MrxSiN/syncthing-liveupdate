package my.MrxSiN.syncthingliveupdate;

import android.os.SystemClock;

import java.lang.reflect.Method;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodDataList;

/**
 * Finds a method by what it looks like rather than by what it is called.
 *
 * DexKit indexes the dex files already loaded in the process and can search them
 * by shape: which class declares the method, what it takes, and which string
 * constants its body references. The last of those is the useful one here,
 * because the host is an ordinary open-source app that gets refactored — a method
 * that was renamed, or moved to another class, still references the same
 * constants, and is still the only method that does.
 *
 * Two searches are made per query, narrowest first. The narrow one asks for the
 * exact class, name and signature and is therefore certain; the wide one drops
 * the name and the class and asks only for the signature and the constants, which
 * is what survives a rename. A wide search that matches more than one method is
 * treated as no match at all, because guessing between candidates in someone
 * else's process is worse than doing nothing.
 */
final class DexKitResolver implements MemberResolver {

    private final DexKitBridge bridge;
    private final ClassLoader classLoader;

    private DexKitResolver(DexKitBridge bridge, ClassLoader classLoader) {
        this.bridge = bridge;
        this.classLoader = classLoader;
    }

    /**
     * Opens an index over the dex files {@code classLoader} loaded, or returns
     * {@code null} when DexKit is unavailable in this process.
     */
    static DexKitResolver open(ClassLoader classLoader) {
        if (!DexKitLibrary.load()) {
            return null;
        }
        try {
            long startedAt = SystemClock.uptimeMillis();
            DexKitBridge bridge = DexKitBridge.create(classLoader, true);
            if (bridge == null || !bridge.isValid()) {
                ModuleRuntime.log("DexKit index unavailable; falling back to name lookups");
                return null;
            }
            /*
             * Indexing someone else's process costs them time at startup, so what it
             * cost is reported rather than assumed. SystemUI is the process where
             * this number matters.
             */
            ModuleRuntime.log("DexKit indexed " + bridge.getDexNum() + " dex file(s) in "
                    + (SystemClock.uptimeMillis() - startedAt) + "ms");
            return new DexKitResolver(bridge, classLoader);
        } catch (RuntimeException | LinkageError unavailable) {
            ModuleRuntime.log("DexKit could not be loaded; falling back to name lookups",
                    unavailable);
            return null;
        }
    }

    @Override
    public Method method(MemberQuery query) {
        Method exact = search(MethodMatcher.create()
                .declaredClass(query.ownerName())
                .name(query.name())
                .paramTypes(query.parameterTypeNames()));
        if (exact != null) {
            return exact;
        }
        if (query.usingStrings().isEmpty()) {
            return null;
        }
        Method renamed = search(MethodMatcher.create()
                .paramTypes(query.parameterTypeNames())
                .usingStrings(query.usingStrings()));
        if (renamed != null) {
            ModuleRuntime.log("Resolved " + query.describe()
                    + " by its constants instead: " + renamed);
        }
        return renamed;
    }

    /** The one method the matcher describes, or {@code null} when it is not unique. */
    private Method search(MethodMatcher matcher) {
        try {
            MethodDataList found = bridge.findMethod(FindMethod.create().matcher(matcher));
            if (found.size() != 1) {
                return null;
            }
            return found.get(0).getMethodInstance(classLoader);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ModuleRuntime.log("DexKit search failed: " + Reflect.describe(failure));
            return null;
        }
    }

    @Override
    public void close() {
        try {
            bridge.close();
        } catch (RuntimeException | LinkageError ignored) {
            ModuleRuntime.log("DexKit index could not be closed: " + Reflect.describe(ignored));
        }
    }
}
