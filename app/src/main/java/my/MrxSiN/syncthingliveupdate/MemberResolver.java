package my.MrxSiN.syncthingliveupdate;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Finds the methods a {@link MemberQuery} describes in a host process.
 *
 * The rest of the module asks for a method through this interface and never
 * decides how it is found, so which strategy is in use — a plain name lookup, a
 * dex search, or one falling back to the other — is a wiring decision made once
 * in {@link Resolvers}.
 *
 * A resolver may hold an open dex index, so it is closed once the hooks that
 * needed it are installed.
 */
interface MemberResolver extends Closeable {

    /** The method {@code query} describes, or {@code null} when it cannot be found. */
    Method method(MemberQuery query);

    /**
     * The one field of type {@code typeName}, declared by {@code ownerName}, that
     * {@code reader}'s body reads, or {@code null} when it is not unique or the
     * resolver cannot see method bodies.
     */
    default Field fieldReadBy(Method reader, String ownerName, String typeName) {
        return null;
    }

    @Override
    void close();
}
