package my.MrxSiN.syncthingliveupdate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reflection helpers shared by the host-facing parts of the module. */
final class Reflect {

    private Reflect() {
    }

    static Class<?> findClass(ClassLoader classLoader, String name) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException notFound) {
            return null;
        }
    }

    static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException notFound) {
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
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
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
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
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
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return false;
        }
    }

    static String describe(Throwable throwable) {
        if (throwable == null) {
            return "none";
        }
        String message = throwable.getMessage();
        return throwable.getClass().getName() + (message == null ? "" : ": " + message);
    }
}
