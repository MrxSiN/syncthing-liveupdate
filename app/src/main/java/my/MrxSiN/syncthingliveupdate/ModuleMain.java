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

    private static final String MODULE_VERSION = "1.1.1";

    private final PlatformContract contract = PlatformContracts.current();
    private final SyncProgressTracker tracker = new SyncProgressTracker();
    private final LiveUpdatePromoter promoter =
            new LiveUpdatePromoter(tracker, new ExpressiveAppearance(), contract);
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
                + ", api=" + getApiVersion()
                + ", contract=" + contract.describe());
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
            try (MemberResolver resolver = Resolvers.open(param.getClassLoader())) {
                if (tracker.install(resolver)) {
                    promoter.install();
                }
            }
        } else if (SystemUi.PACKAGE.equals(packageName)
                && systemUiStyled.compareAndSet(false, true)) {
            styleSystemUi(param.getClassLoader());
        }
    }

    /**
     * The SystemUI polish is optional, and only attempted on a release whose
     * SystemUI the module has been read against. On anything else the Live Update
     * itself still works and the bar is simply the one the platform draws.
     */
    private void styleSystemUi(ClassLoader classLoader) {
        if (!contract.systemUiPolishSupported()) {
            ModuleRuntime.log("SystemUI polish left off on " + contract.describe());
            return;
        }
        try (MemberResolver resolver = Resolvers.open(classLoader)) {
            progressMotion.install(resolver);
            wavyTrack.install(classLoader, resolver);
            chipColors.install(classLoader);
        }
    }

    private void patchPromotionPolicy(ClassLoader classLoader) {
        if (promotionPatched.compareAndSet(false, true)) {
            PromotionPolicyPatch.install(classLoader);
        }
    }
}
