package my.MrxSiN.syncthingliveupdate;

import java.util.List;

/**
 * One method the module needs from a host, described rather than named.
 *
 * A name alone is only enough while the host keeps it. The same method is
 * therefore described three ways at once: where it is expected to be declared,
 * what it is expected to be called, and what it provably does — the string
 * constants its body references. A resolver that can search by shape, such as
 * {@link DexKitResolver}, can still find the method after a rename; one that can
 * only look a name up, such as {@link ReflectionResolver}, uses the first two and
 * ignores the third.
 *
 * @param ownerName          fully qualified name of the declaring class, or
 *                           {@code null} when the host never kept one
 * @param name               the method name the module was written against, or
 *                           {@code null} when the host never kept one
 * @param parameterTypeNames parameter types in order, as {@code int} or
 *                           {@code java.lang.String}
 * @param usingStrings       string constants the method's body is known to
 *                           reference, or empty when none identifies it
 */
record MemberQuery(
        String ownerName,
        String name,
        List<String> parameterTypeNames,
        List<String> usingStrings
) {

    MemberQuery {
        parameterTypeNames = List.copyOf(parameterTypeNames);
        usingStrings = List.copyOf(usingStrings);
    }

    /** A method identified by its name and signature alone. */
    static MemberQuery of(String ownerName, String name, String... parameterTypeNames) {
        return new MemberQuery(ownerName, name, List.of(parameterTypeNames), List.of());
    }

    /**
     * A method known only by its signature and its constants, for host code that
     * has no stable name in any build: R8 creates it, as it does the name lookup
     * that replaces an unboxed enum's {@code name()}.
     */
    static MemberQuery byShape(String... parameterTypeNames) {
        return new MemberQuery(null, null, List.of(parameterTypeNames), List.of());
    }

    /** Whether the query names a class and a method, which a name lookup needs. */
    boolean isNamed() {
        return ownerName != null && name != null;
    }

    /** This query with the string constants that identify the method after a rename. */
    MemberQuery using(String... strings) {
        return new MemberQuery(ownerName, name, parameterTypeNames, List.of(strings));
    }

    /** The query as it reads in a log line. */
    String describe() {
        String signature = "(" + String.join(", ", parameterTypeNames) + ")";
        return isNamed()
                ? ownerName + "#" + name + signature
                : "method" + signature + " using " + usingStrings;
    }
}
