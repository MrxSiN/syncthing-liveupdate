package my.MrxSiN.syncthingliveupdate;

import android.content.pm.ApplicationInfo;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads DexKit's native library into a host process.
 *
 * DexKit does not load its own library; the caller has to. In an ordinary app
 * {@code System.loadLibrary("dexkit")} would do it, but the module runs inside
 * someone else's process, and that call searches the *host's* library path, where
 * the module's library has no reason to be. The library is therefore loaded by
 * absolute path out of the module's own installation, which
 * {@link ModuleRuntime#moduleApplicationInfo()} is the only way to find from
 * inside a host.
 *
 * This requires the library to exist as a file, which is why the module packages
 * its JNI libraries the legacy way — see the {@code jniLibs} block in
 * app/build.gradle.kts. With the modern packaging the library stays compressed
 * inside the APK and no path can reach it.
 *
 * A failure here is not fatal: DexKit is the first of two resolvers, and
 * {@link ReflectionResolver} answers on its own.
 */
final class DexKitLibrary {

    private static final String NAME = "dexkit";
    private static final String FILE = "lib" + NAME + ".so";

    private static final AtomicBoolean attempted = new AtomicBoolean(false);
    private static volatile boolean loaded;

    private DexKitLibrary() {
    }

    /** Loads the library once per process. Returns whether it is available. */
    static boolean load() {
        if (attempted.compareAndSet(false, true)) {
            loaded = loadFromModule() || loadFromHostPath();
        }
        return loaded;
    }

    private static boolean loadFromModule() {
        ApplicationInfo module = ModuleRuntime.moduleApplicationInfo();
        if (module == null || module.nativeLibraryDir == null) {
            return false;
        }
        File library = new File(module.nativeLibraryDir, FILE);
        if (!library.isFile()) {
            /*
             * Usually this is not a packaging problem but a stale path: the
             * framework hands out the ApplicationInfo it recorded when it loaded
             * the module, and installing an update replaces that whole directory.
             * Until the framework restarts, the module is pointed at an install
             * that no longer exists. PromotionStatus reports the same condition in
             * the terms that matter to the user.
             */
            ModuleRuntime.log("DexKit library is not at " + library
                    + "; name lookups will be used instead");
            return false;
        }
        try {
            System.load(library.getAbsolutePath());
            return true;
        } catch (RuntimeException | LinkageError unusable) {
            ModuleRuntime.log("DexKit library at " + library + " could not be loaded",
                    unusable);
            return false;
        }
    }

    /**
     * The ordinary lookup, which only succeeds where the module itself is the
     * process — in a host it is expected to fail, and is tried in case a framework
     * put the module's libraries on the host's path.
     */
    private static boolean loadFromHostPath() {
        try {
            System.loadLibrary(NAME);
            return true;
        } catch (RuntimeException | LinkageError unavailable) {
            ModuleRuntime.log("DexKit library unavailable: " + Reflect.describe(unavailable));
            return false;
        }
    }
}
