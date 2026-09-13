package my.MrxSiN.syncthingliveupdate;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedModule;

/**
 * Modern Xposed API entry point.
 *
 * It wires independent pieces: {@link PromotionPolicyPatch} lets the host
 * post promoted notifications at all, {@link SyncProgressTracker} follows the
 * sync figures the host publishes, and {@link LiveUpdatePromoter} rewrites the
 * host's own persistent notification into a Live Update while a transfer runs,
 * styled by {@link ExpressiveAppearance}. In SystemUI, {@link WavyProgressTrack}
 * draws its progress bar as a wave, {@link ProgressBarMotion} animates it between
 * updates and {@link StatusBarChipColors} colours its chip.
 */
public final class ModuleMain extends XposedModule {

    private static final String MODULE_VERSION = "1.1.0";

    private final SyncProgressTracker tracker = new SyncProgressTracker();
    private final LiveUpdatePromoter promoter =
            new LiveUpdatePromoter(tracker, new ExpressiveAppearance());
    private final ProgressBarMotion progressMotion = new ProgressBarMotion();
    private final WavyProgressTrack wavyTrack = new WavyProgressTrack();
    private final StatusBarChipColors chipColors = new StatusBarChipColors();
    private final AtomicBoolean promotionPatched = new AtomicBoolean(false);
    private final AtomicBoolean systemUiStyled = new AtomicBoolean(false);

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
        String packageName = param.getPackageName();
        if (!processName.isEmpty() && !packageName.equals(processName)) {
            return;
        }
        if (HostApp.PACKAGE.equals(packageName)) {
            if (tracker.install(param.getClassLoader())) {
                promoter.install();
            }
        } else if (SystemUi.PACKAGE.equals(packageName)
                && systemUiStyled.compareAndSet(false, true)) {
            progressMotion.install(param.getClassLoader());
            wavyTrack.install(param.getClassLoader());
            chipColors.install(param.getClassLoader());
        }
    }

    private void patchPromotionPolicy(ClassLoader classLoader) {
        if (promotionPatched.compareAndSet(false, true)) {
            PromotionPolicyPatch.install(classLoader);
        }
    }
}
