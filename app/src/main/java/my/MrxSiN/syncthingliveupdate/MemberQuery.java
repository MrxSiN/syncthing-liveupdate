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
 * @param ownerName          fully qualified name of the declaring class
 * @param name               the method name the module was written against
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

    /** This query with the string constants that identify the method after a rename. */
    MemberQuery using(String... strings) {
        return new MemberQuery(ownerName, name, parameterTypeNames, List.of(strings));
    }

    /** The query as it reads in a log line. */
    String describe() {
        return ownerName + "#" + name + "(" + String.join(", ", parameterTypeNames) + ")";
    }
}
