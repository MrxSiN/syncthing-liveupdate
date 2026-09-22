package my.MrxSiN.syncthingliveupdate;

import java.lang.reflect.Method;

/**
 * Finds a method by the name and signature the module was written against.
 *
 * This is the cheap, exact resolver: it costs one class lookup and one method
 * lookup, and it is right for as long as the host keeps the names. It cannot
 * survive a rename, which is what {@link DexKitResolver} is for.
 */
final class ReflectionResolver implements MemberResolver {

    private final ClassLoader classLoader;

    ReflectionResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Method method(MemberQuery query) {
        Class<?>[] parameterTypes =
                Reflect.findTypes(classLoader, query.parameterTypeNames());
        if (parameterTypes == null) {
            return null;
        }
        return Reflect.findMethod(
                Reflect.findClass(classLoader, query.ownerName()),
                query.name(),
                parameterTypes);
    }

    @Override
    public void close() {
    }
}
