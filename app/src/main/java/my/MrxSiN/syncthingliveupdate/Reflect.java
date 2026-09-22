package my.MrxSiN.syncthingliveupdate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.StringJoiner;

/**
 * Reflection helpers shared by the host-facing parts of the module.
 *
 * Every helper fails softly, because the module is a guest in someone else's
 * process and a member that moved has to cost a feature rather than the host.
 * Each soft failure is handed to {@link ReflectionDiagnostics} first, so the
 * reason is still on record even though the caller only sees {@code null} or
 * {@code false}.
 */
final class Reflect {

    private static final String UNSAFE = "sun.misc.Unsafe";
    private static final String ACTIVITY_THREAD = "android.app.ActivityThread";

    private Reflect() {
    }

    static Class<?> findClass(ClassLoader classLoader, String name) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException | RuntimeException | LinkageError notFound) {
            ReflectionDiagnostics.record(name, "class", notFound);
            return null;
        }
    }

    /**
     * A type by name, including the primitives, which {@code Class.forName} does
     * not answer for. Returns {@code null} when the name names nothing.
     */
    static Class<?> findType(ClassLoader classLoader, String name) {
        return switch (name) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "char" -> char.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "void" -> void.class;
            default -> findClass(classLoader, name);
        };
    }

    /** The types {@code names} name, or {@code null} when any of them is missing. */
    static Class<?>[] findTypes(ClassLoader classLoader, List<String> names) {
        Class<?>[] types = new Class<?>[names.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = findType(classLoader, names.get(i));
            if (types[i] == null) {
                return null;
            }
        }
        return types;
    }

    static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException | RuntimeException | LinkageError notFound) {
            ReflectionDiagnostics.record(
                    owner.getName() + "#" + name, signature(parameterTypes), notFound);
            return null;
        }
    }

    /** Reads a declared field, or returns {@code null} when it is absent. */
    static Object readField(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            ReflectionDiagnostics.record(
                    target.getClass().getName() + "#" + name, "field read", unavailable);
            return null;
        }
    }

    /** Invokes a no-argument method, or returns {@code null} when it is absent. */
    static Object invoke(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            ReflectionDiagnostics.record(
                    target.getClass().getName() + "#" + name, signature(), unavailable);
            return null;
        }
    }

    /** Invokes a static method, or returns {@code null} when the call fails. */
    static Object invokeStatic(Class<?> owner, String name, Class<?>[] parameterTypes,
            Object... arguments) {
        Method method = findMethod(owner, name, parameterTypes);
        if (method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            return method.invoke(null, arguments);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            ReflectionDiagnostics.record(
                    owner.getName() + "#" + name, signature(parameterTypes), unavailable);
            return null;
        }
    }

    /** Writes an int field. Returns whether the value was written. */
    static boolean writeField(Object target, String name, int value) {
        if (target == null) {
            return false;
        }
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.setInt(target, value);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            ReflectionDiagnostics.record(
                    target.getClass().getName() + "#" + name, "int field write", unavailable);
            return false;
        }
    }

    /**
     * Builds an instance of a class the module only needs as a carrier of values.
     *
     * A declared constructor is used whenever one can be called with defaults,
     * because that leaves the class's own initialisation in charge. Only when the
     * optimizer has removed every constructor, which is why this helper exists at
     * all, does it fall back to {@code Unsafe.allocateInstance}. That fallback is
     * the most fragile call in the module, so it is the last resort rather than
     * the first, and both paths return {@code null} on failure.
     */
    static Object allocate(Class<?> type) {
        if (type == null) {
            return null;
        }
        Object constructed = construct(type);
        return constructed != null ? constructed : allocateWithoutConstructor(type);
    }

    /** Calls the shortest declared constructor with default arguments. */
    private static Object construct(Class<?> type) {
        Constructor<?> shortest = null;
        try {
            for (Constructor<?> candidate : type.getDeclaredConstructors()) {
                if (shortest == null
                        || candidate.getParameterCount() < shortest.getParameterCount()) {
                    shortest = candidate;
                }
            }
        } catch (RuntimeException | LinkageError unavailable) {
            ReflectionDiagnostics.record(type.getName(), "declared constructors", unavailable);
            return null;
        }
        if (shortest == null) {
            ReflectionDiagnostics.record(type.getName(), "any constructor", null);
            return null;
        }
        try {
            shortest.setAccessible(true);
            return shortest.newInstance(defaults(shortest.getParameterTypes()));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unusable) {
            ReflectionDiagnostics.record(
                    type.getName() + "#<init>",
                    signature(shortest.getParameterTypes()),
                    unusable);
            return null;
        }
    }

    private static Object[] defaults(Class<?>[] parameterTypes) {
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (!type.isPrimitive()) {
                arguments[i] = null;
            } else if (type == boolean.class) {
                arguments[i] = false;
            } else if (type == char.class) {
                arguments[i] = '\0';
            } else if (type == long.class) {
                arguments[i] = 0L;
            } else if (type == float.class) {
                arguments[i] = 0f;
            } else if (type == double.class) {
                arguments[i] = 0d;
            } else {
                arguments[i] = 0;
            }
        }
        return arguments;
    }

    private static Object allocateWithoutConstructor(Class<?> type) {
        try {
            Class<?> unsafeClass = Class.forName(UNSAFE);
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return unsafeClass.getMethod("allocateInstance", Class.class)
                    .invoke(theUnsafe.get(null), type);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            ReflectionDiagnostics.record(
                    type.getName(), "allocation without a constructor", unavailable);
            return null;
        }
    }

    /** The application of the current process, or {@code null} when unavailable. */
    static Object currentApplication() {
        try {
            return Class.forName(ACTIVITY_THREAD).getMethod("currentApplication").invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            ReflectionDiagnostics.record(
                    ACTIVITY_THREAD + "#currentApplication", signature(), unavailable);
            return null;
        }
    }

    /**
     * Invokes a single-argument method when the platform provides it, so the
     * module can use an API that only exists on newer releases without failing to
     * load on older ones. Returns whether the call happened.
     */
    static boolean invokeIfPresent(
            Object target,
            String name,
            Class<?> parameterType,
            Object argument
    ) {
        if (target == null) {
            return false;
        }
        try {
            Method method = target.getClass().getMethod(name, parameterType);
            method.invoke(target, argument);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            ReflectionDiagnostics.record(
                    target.getClass().getName() + "#" + name,
                    signature(parameterType),
                    unavailable);
            return false;
        }
    }

    /** A parameter list, for the diagnostic that says what was expected. */
    static String signature(Class<?>... parameterTypes) {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (Class<?> parameterType : parameterTypes) {
            joiner.add(parameterType == null ? "?" : parameterType.getName());
        }
        return joiner.toString();
    }

    static String describe(Throwable throwable) {
        if (throwable == null) {
            return "none";
        }
        String message = throwable.getMessage();
        return throwable.getClass().getName() + (message == null ? "" : ": " + message);
    }
}
