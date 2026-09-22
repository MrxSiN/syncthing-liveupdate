package my.MrxSiN.syncthingliveupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

/**
 * The name-lookup half of member resolution, which is the fallback every host
 * relies on when DexKit is unavailable. It is checked against this test's own
 * classes, because the behaviour under test is the name-to-member mapping rather
 * than anything about the host.
 */
class ReflectionResolverTest {

    @SuppressWarnings("unused")
    static class Target {

        void present(int count, String label, boolean flag) {
        }

        void overloaded(int count) {
        }

        void overloaded(String label) {
        }
    }

    private final MemberResolver resolver =
            new ReflectionResolver(getClass().getClassLoader());

    private static MemberQuery query(String name, String... parameterTypeNames) {
        return MemberQuery.of(Target.class.getName(), name, parameterTypeNames);
    }

    @Test
    @DisplayName("finds a method whose primitive and reference parameters both match")
    void mixedSignature() {
        Method found = resolver.method(
                query("present", "int", "java.lang.String", "boolean"));

        assertNotNull(found);
        assertEquals("present", found.getName());
    }

    @Test
    @DisplayName("picks the overload the signature names")
    void overloads() {
        Method byInt = resolver.method(query("overloaded", "int"));
        Method byString = resolver.method(query("overloaded", "java.lang.String"));

        assertNotNull(byInt);
        assertNotNull(byString);
        assertEquals(int.class, byInt.getParameterTypes()[0]);
        assertEquals(String.class, byString.getParameterTypes()[0]);
    }

    @Test
    @DisplayName("a renamed method is simply not found, rather than guessed at")
    void renamed() {
        assertNull(resolver.method(query("renamedUpstream", "int")));
    }

    @Test
    @DisplayName("a missing class is not found")
    void missingClass() {
        assertNull(resolver.method(MemberQuery.of("no.such.Class", "present")));
    }

    @Test
    @DisplayName("a parameter type that no longer exists stops the lookup")
    void missingParameterType() {
        assertNull(resolver.method(query("present", "no.such.Type")));
    }

    @Test
    @DisplayName("every primitive name resolves to its own type")
    void primitives() {
        ClassLoader loader = getClass().getClassLoader();

        assertEquals(boolean.class, Reflect.findType(loader, "boolean"));
        assertEquals(byte.class, Reflect.findType(loader, "byte"));
        assertEquals(char.class, Reflect.findType(loader, "char"));
        assertEquals(short.class, Reflect.findType(loader, "short"));
        assertEquals(int.class, Reflect.findType(loader, "int"));
        assertEquals(long.class, Reflect.findType(loader, "long"));
        assertEquals(float.class, Reflect.findType(loader, "float"));
        assertEquals(double.class, Reflect.findType(loader, "double"));
        assertEquals(void.class, Reflect.findType(loader, "void"));
    }

    @Test
    @DisplayName("one missing type makes the whole signature unresolvable")
    void findTypes() {
        ClassLoader loader = getClass().getClassLoader();

        assertNotNull(Reflect.findTypes(loader, List.of("int", "java.lang.String")));
        assertNull(Reflect.findTypes(loader, List.of("int", "no.such.Type")));
    }
}
