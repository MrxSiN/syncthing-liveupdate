package my.MrxSiN.syncthingliveupdate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reflection helpers shared by the host-facing parts of the module. */
final class Reflect {

    private static final String UNSAFE = "sun.misc.Unsafe";
    private static final String ACTIVITY_THREAD = "android.app.ActivityThread";

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
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return false;
        }
    }

    /**
     * Allocates an instance without running a constructor, for classes whose
     * constructors were removed by the optimizer. Returns {@code null} on failure.
     */
    static Object allocate(Class<?> type) {
        if (type == null) {
            return null;
        }
        try {
            Class<?> unsafeClass = Class.forName(UNSAFE);
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return unsafeClass.getMethod("allocateInstance", Class.class)
                    .invoke(theUnsafe.get(null), type);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return null;
        }
    }

    /** The application of the current process, or {@code null} when unavailable. */
    static Object currentApplication() {
        try {
            return Class.forName(ACTIVITY_THREAD).getMethod("currentApplication").invoke(null);
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
