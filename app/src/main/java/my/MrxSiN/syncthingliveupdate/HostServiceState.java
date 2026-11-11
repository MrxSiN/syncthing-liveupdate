package my.MrxSiN.syncthingliveupdate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reads whether the host's {@code SyncthingService} is running.
 *
 * Builds that keep the {@code State} enum expose it through
 * {@code getCurrentState()}. Builds that R8 shrank, such as the ones F-Droid
 * mirrors distribute, unbox the enum into a plain int field and replace
 * {@code name()} with a static {@code (int) -> String} lookup. Neither the field
 * nor the lookup has a stable name, so both are found by what they are: the int
 * field of the service that the persistent-notification method reads, and the
 * one method taking an int that references every state name. Feeding the first
 * to the second yields the same name the enum would have given.
 */
final class HostServiceState {

    private volatile Method currentState;
    private volatile Field unboxedState;
    private volatile Method unboxedName;

    /**
     * Resolves how to read the state. {@code notificationMethod} is the hooked
     * persistent-notification method, which always reads it. Returns whether
     * either form was found.
     */
    boolean install(MemberResolver resolver, Method notificationMethod) {
        currentState = resolver.method(HostApp.CURRENT_STATE_QUERY);
        if (currentState != null) {
            currentState.setAccessible(true);
            return true;
        }
        unboxedState = resolver.fieldReadBy(
                notificationMethod, HostApp.SYNCTHING_SERVICE, "int");
        unboxedName = resolver.method(HostApp.STATE_NAME_QUERY);
        if (unboxedState == null || unboxedName == null) {
            return false;
        }
        unboxedName.setAccessible(true);
        return true;
    }

    /** Whether {@code service} is in the running state. */
    boolean isActive(Object service) {
        return HostApp.STATE_ACTIVE.equals(name(service));
    }

    private String name(Object service) {
        try {
            if (currentState != null) {
                return currentState.invoke(service) instanceof Enum<?> state
                        ? state.name()
                        : null;
            }
            if (unboxedState != null && unboxedName != null) {
                return String.valueOf(unboxedName.invoke(null, unboxedState.getInt(service)));
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            ReflectionDiagnostics.record(
                    HostApp.SYNCTHING_SERVICE + "#state", "state read", failure);
        }
        return null;
    }
}
