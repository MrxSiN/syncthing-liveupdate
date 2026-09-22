package my.MrxSiN.syncthingliveupdate;

import java.lang.reflect.Method;

/**
 * Builds the {@link MemberResolver} a host process gets.
 *
 * The module searches the dex first and falls back to a plain name lookup, which
 * is the order that matters: a name lookup that succeeds proves nothing about
 * whether the method is still the right one, whereas a dex search that matches
 * the class, the name and the signature has checked all three. When the search
 * finds nothing — because DexKit is unavailable, or because the method it
 * describes has no unique match — the name lookup still answers, so the module is
 * never worse off than it was before DexKit.
 *
 * Opening the index costs time and memory, so a resolver is opened around the
 * installation of a process's hooks and closed immediately afterwards. Nothing
 * holds one past startup.
 */
final class Resolvers {

    private Resolvers() {
    }

    /** A resolver for {@code classLoader}, to be closed once the hooks are installed. */
    static MemberResolver open(ClassLoader classLoader) {
        MemberResolver fallback = new ReflectionResolver(classLoader);
        DexKitResolver primary = DexKitResolver.open(classLoader);
        return primary == null ? fallback : new FirstOf(primary, fallback);
    }

    /** Asks one resolver, then the other. */
    private record FirstOf(MemberResolver primary, MemberResolver fallback)
            implements MemberResolver {

        @Override
        public Method method(MemberQuery query) {
            Method found = primary.method(query);
            return found != null ? found : fallback.method(query);
        }

        @Override
        public void close() {
            try {
                primary.close();
            } finally {
                fallback.close();
            }
        }
    }
}
