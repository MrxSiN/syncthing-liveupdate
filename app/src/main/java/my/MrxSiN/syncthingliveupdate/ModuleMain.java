package my.MrxSiN.syncthingliveupdate;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedModule;

/**
 * Modern Xposed API entry point.
 *
 * It wires three independent pieces: {@link PromotionPolicyPatch} lets the host
 * post promoted notifications at all, {@link SyncProgressTracker} follows the
 * sync figures the host publishes, and {@link LiveUpdatePromoter} rewrites the
 * host's own persistent notification into a Live Update while a transfer runs.
 */
public final class ModuleMain extends XposedModule {

    private static final String MODULE_VERSION = "1.0.0";

    private final SyncProgressTracker tracker = new SyncProgressTracker();
    private final LiveUpdatePromoter promoter = new LiveUpdatePromoter(tracker);
    private final AtomicBoolean promotionPatched = new AtomicBoolean(false);

    private volatile String processName = "";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        ModuleRuntime.attach(this);
        processName = param.getProcessName();
        ModuleRuntime.log("Syncthing Live Update v" + MODULE_VERSION
                + " loaded in " + processName
                + ", framework=" + getFrameworkName()
                + " " + getFrameworkVersion()
                + ", api=" + getApiVersion());
        if (param.isSystemServer()) {
            patchPromotionPolicy(getClass().getClassLoader());
        }
    }

    /**
     * Frameworks that load the module early enough hand over the system server
     * class loader here instead of through {@link #onModuleLoaded}.
     */
    @Override
    public void onSystemServerStarting(SystemServerStartingParam param) {
        ModuleRuntime.attach(this);
        patchPromotionPolicy(param.getClassLoader());
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!HostApp.PACKAGE.equals(param.getPackageName())) {
            return;
        }
        if (!processName.isEmpty() && !HostApp.PACKAGE.equals(processName)) {
            return;
        }
        if (tracker.install(param.getClassLoader())) {
            promoter.install();
        }
    }

    private void patchPromotionPolicy(ClassLoader classLoader) {
        if (promotionPatched.compareAndSet(false, true)) {
            PromotionPolicyPatch.install(classLoader);
        }
    }
}
