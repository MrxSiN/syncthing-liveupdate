package my.MrxSiN.syncthingliveupdate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * The service state as an R8-shrunk host stores it: an int field and a static
 * name lookup, both found by shape. Mirrors Syncthing-Fork 2.1.7.0 from F-Droid,
 * where {@code SyncthingService.State} became {@code SyncthingService.v:I} and
 * {@code ft2.f(int)}.
 */
class HostServiceStateTest {

    /** Stands in for the shrunk SyncthingService. */
    static class ShrunkService {
        @SuppressWarnings("unused")
        int v;
    }

    /** Stands in for R8's replacement of {@code State.name()}: 1-based ordinals. */
    @SuppressWarnings("unused")
    static String stateName(int state) {
        return state >= 1 && state <= HostApp.STATE_NAMES.length
                ? HostApp.STATE_NAMES[state - 1]
                : "null";
    }

    private static MemberResolver shrunkHost() throws ReflectiveOperationException {
        Field state = ShrunkService.class.getDeclaredField("v");
        Method name = HostServiceStateTest.class.getDeclaredMethod("stateName", int.class);
        return new MemberResolver() {
            @Override
            public Method method(MemberQuery query) {
                return query == HostApp.STATE_NAME_QUERY ? name : null;
            }

            @Override
            public Field fieldReadBy(Method reader, String ownerName, String typeName) {
                return HostApp.SYNCTHING_SERVICE.equals(ownerName) && "int".equals(typeName)
                        ? state
                        : null;
            }

            @Override
            public void close() {
            }
        };
    }

    @Test
    @DisplayName("an unboxed state reads as running only while it names ACTIVE")
    void unboxedState() throws ReflectiveOperationException {
        HostServiceState state = new HostServiceState();
        assertTrue(state.install(shrunkHost(), Object.class.getMethod("toString")));

        ShrunkService service = new ShrunkService();
        for (int ordinal = 0; ordinal <= HostApp.STATE_NAMES.length + 1; ordinal++) {
            service.v = ordinal;
            boolean active = ordinal >= 1 && ordinal <= HostApp.STATE_NAMES.length
                    &&HostApp.STATE_ACTIVE.equals(HostApp.STATE_NAMES[ordinal - 1]);
            assertTrue(state.isActive(service) == active, "state " + ordinal);
        }
    }

    @Test
    @DisplayName("a host where neither form is found reports the state as unavailable")
    void nothingFound() throws ReflectiveOperationException {
        MemberResolver empty = new ReflectionResolver(getClass().getClassLoader());

        HostServiceState state = new HostServiceState();
        assertFalse(state.install(empty, Object.class.getMethod("toString")));
        assertFalse(state.isActive(new ShrunkService()));
    }
}
