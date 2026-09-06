package my.MrxSiN.syncthingliveupdate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;

/**
 * Tracks the sync figures the host publishes.
 *
 * The host recalculates the overall completion percentage in
 * {@code RestApi.onTotalSyncCompletionChange()} and hands it straight to
 * {@code NotificationHandler.updatePersistentNotification(...)}, so observing
 * that one method yields the percentage without polling the REST API. The two
 * figures that percentage is derived from are read from the completion models on
 * the way past, because only they say whether the transfer is inbound, outbound
 * or both.
 *
 * The values are read before the host call proceeds, because the host posts its
 * notification during that call and {@link LiveUpdatePromoter} has to see the new
 * state by then.
 */
final class SyncProgressTracker implements SyncProgressSource {

    private final AtomicBoolean installed = new AtomicBoolean(false);
    private final SyncingFolders folders = new SyncingFolders();

    private volatile SyncSnapshot snapshot = SyncSnapshot.IDLE;

    /**
     * Completion is only carried by the calls that recalculated it. The host asks
     * for a redraw with stale figures whenever it only wants the previous text
     * repeated, so the last real value is kept across those calls.
     */
    private volatile int lastCompletion = SyncSnapshot.COMPLETION_UNKNOWN;

    private volatile int folderCompletion = SyncSnapshot.COMPLETION_COMPLETE;
    private volatile int deviceCompletion = SyncSnapshot.COMPLETION_UNKNOWN;

    @Override
    public SyncSnapshot current() {
        return snapshot;
    }

    /** Installs the hooks. Returns false when the host method is not present. */
    boolean install(ClassLoader hostClassLoader) {
        if (!installed.compareAndSet(false, true)) {
            return true;
        }

        Class<?> handlerClass = Reflect.findClass(hostClassLoader, HostApp.NOTIFICATION_HANDLER);
        Class<?> serviceClass = Reflect.findClass(hostClassLoader, HostApp.SYNCTHING_SERVICE);
        Method origin = Reflect.findMethod(
                handlerClass,
                HostApp.UPDATE_PERSISTENT_NOTIFICATION,
                serviceClass,
                Boolean.class,
                int.class,
                int.class
        );
        if (origin == null) {
            ModuleRuntime.log("Host notification method not found; module stays inactive");
            return false;
        }

        XposedInterface.HookHandle handle = ModuleRuntime.hook(origin, this::intercept);
        if (handle == null) {
            ModuleRuntime.log("Progress hook rejected by the framework; module stays inactive");
            return false;
        }
        ModuleRuntime.log("Observing " + origin);
        installDirectionHooks(hostClassLoader);
        if (!folders.install(hostClassLoader)) {
            ModuleRuntime.log("Folder names unavailable; the Live Update omits them");
        }
        return true;
    }

    /**
     * The direction hooks are optional: without them the progress is still shown,
     * only without the inbound or outbound badge.
     */
    private void installDirectionHooks(ClassLoader hostClassLoader) {
        boolean folders = hookCompletion(
                hostClassLoader,
                HostApp.LOCAL_COMPLETION,
                HostApp.TOTAL_FOLDER_COMPLETION,
                completion -> folderCompletion = completion
        );
        boolean devices = hookCompletion(
                hostClassLoader,
                HostApp.REMOTE_COMPLETION,
                HostApp.TOTAL_DEVICE_COMPLETION,
                completion -> deviceCompletion = completion
        );
        if (!folders || !devices) {
            ModuleRuntime.log("Transfer direction unavailable; the badge stays off");
        }
    }

    private boolean hookCompletion(
            ClassLoader hostClassLoader,
            String className,
            String methodName,
            CompletionSink sink
    ) {
        Method origin = Reflect.findMethod(
                Reflect.findClass(hostClassLoader, className), methodName);
        if (origin == null) {
            return false;
        }
        return ModuleRuntime.hook(origin, chain -> {
            Object result = chain.proceed();
            if (result instanceof Integer completion) {
                sink.accept(completion);
            }
            return result;
        }) != null;
    }

    private Object intercept(XposedInterface.Chain chain) throws Throwable {
        try {
            update(chain.getArgs());
        } catch (Throwable failure) {
            ModuleRuntime.log("Progress update skipped: " + Reflect.describe(failure));
        }
        return chain.proceed();
    }

    private void update(List<Object> args) {
        if (args.size() < 4) {
            return;
        }

        boolean recalculated = Boolean.FALSE.equals(args.get(1));
        if (recalculated && args.get(3) instanceof Integer completion) {
            lastCompletion = completion;
        }

        boolean running = isRunning(args.get(0));
        if (!running) {
            lastCompletion = SyncSnapshot.COMPLETION_UNKNOWN;
        }
        snapshot = new SyncSnapshot(running, lastCompletion, direction(), folders.current());
    }

    private SyncDirection direction() {
        boolean downloading = isIncomplete(folderCompletion);
        boolean uploading = isIncomplete(deviceCompletion);
        return SyncDirection.of(downloading, uploading);
    }

    /** A figure of -1 means the host considers it inapplicable, not pending. */
    private static boolean isIncomplete(int completion) {
        return completion >= 0 && completion < SyncSnapshot.COMPLETION_COMPLETE;
    }

    private static boolean isRunning(Object service) {
        Object state = Reflect.invoke(service, HostApp.CURRENT_STATE_METHOD);
        return state instanceof Enum<?> value && HostApp.STATE_ACTIVE.equals(value.name());
    }

    /** Receives a completion percentage observed on the host. */
    private interface CompletionSink {
        void accept(int completion);
    }
}
