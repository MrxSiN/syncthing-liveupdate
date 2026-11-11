package my.MrxSiN.syncthingliveupdate;

import android.os.SystemClock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.FieldUsingType;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.result.UsingFieldData;

/**
 * Finds a method by what it looks like rather than by what it is called.
 *
 * DexKit indexes a host's dex files and can search them by shape: which class
 * declares the method, what it takes, and which string constants its body
 * references. The last of those is the useful one here, because the host is
 * shrunk by R8 in some of its distributions: a method that was renamed, or moved
 * to another class, still references the same constants, and is still the only
 * method that does.
 *
 * Two searches are made per query, narrowest first. The narrow one asks for the
 * exact class, name and signature and is therefore certain; the wide one drops
 * the name and the class and asks only for the signature and the constants, which
 * is what survives a rename. A wide search that matches more than one method is
 * treated as no match at all, because guessing between candidates in someone
 * else's process is worse than doing nothing.
 *
 * The index is read from the host's APK file, never from the process's memory.
 * DexKit's in-memory mode walks ART's own dex mappings, and on Android 17 QPR2 one
 * of those mappings is not readable: the read faults inside libdexkit.so, which no
 * Java handler can catch, and in SystemUI that became a boot-time crash loop.
 */
final class DexKitResolver implements MemberResolver {

    private final DexKitBridge bridge;
    private final ClassLoader classLoader;

    private DexKitResolver(DexKitBridge bridge, ClassLoader classLoader) {
        this.bridge = bridge;
        this.classLoader = classLoader;
    }

    /**
     * Opens an index over the APK at {@code apkPath}, whose classes
     * {@code classLoader} loaded, or returns {@code null} when DexKit is unavailable.
     */
    static DexKitResolver open(String apkPath, ClassLoader classLoader) {
        if (apkPath == null || !DexKitLibrary.load()) {
            return null;
        }
        try {
            long startedAt = SystemClock.uptimeMillis();
            DexKitBridge bridge = DexKitBridge.create(apkPath);
            if (!bridge.isValid()) {
                ModuleRuntime.log("DexKit index unavailable; falling back to name lookups");
                return null;
            }
            ModuleRuntime.log("DexKit indexed " + bridge.getDexNum() + " dex file(s) in "
                    + (SystemClock.uptimeMillis() - startedAt) + "ms");
            return new DexKitResolver(bridge, classLoader);
        } catch (RuntimeException | LinkageError unavailable) {
            ModuleRuntime.log("DexKit could not be loaded; falling back to name lookups",
                    unavailable);
            return null;
        }
    }

    @Override
    public Method method(MemberQuery query) {
        if (query.isNamed()) {
            Method exact = search(MethodMatcher.create()
                    .declaredClass(query.ownerName())
                    .name(query.name())
                    .paramTypes(query.parameterTypeNames()));
            if (exact != null) {
                return exact;
            }
        }
        if (query.usingStrings().isEmpty()) {
            return null;
        }
        Method renamed = search(MethodMatcher.create()
                .paramTypes(query.parameterTypeNames())
                .usingStrings(query.usingStrings()));
        if (renamed != null) {
            ModuleRuntime.log("Resolved " + query.describe() + " by its constants: " + renamed);
        }
        return renamed;
    }

    @Override
    public Field fieldReadBy(Method reader, String ownerName, String typeName) {
        try {
            List<UsingFieldData> read = bridge.getMethodData(reader).getUsingFields().stream()
                    .filter(use -> use.getUsingType() == FieldUsingType.Read
                            && ownerName.equals(use.getField().getDeclaredClassName())
                            && typeName.equals(use.getField().getTypeName()))
                    .distinct()
                    .toList();
            if (read.size() != 1) {
                return null;
            }
            Field field = read.get(0).getField().getFieldInstance(classLoader);
            field.setAccessible(true);
            ModuleRuntime.log("Resolved " + ownerName + " " + typeName
                    + " read by " + reader.getName() + ": " + field);
            return field;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ModuleRuntime.log("DexKit field search failed: " + Reflect.describe(failure));
            return null;
        }
    }

    /** The one method the matcher describes, or {@code null} when it is not unique. */
    private Method search(MethodMatcher matcher) {
        try {
            MethodDataList found = bridge.findMethod(FindMethod.create().matcher(matcher));
            if (found.size() != 1) {
                return null;
            }
            return found.get(0).getMethodInstance(classLoader);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ModuleRuntime.log("DexKit search failed: " + Reflect.describe(failure));
            return null;
        }
    }

    @Override
    public void close() {
        try {
            bridge.close();
        } catch (RuntimeException | LinkageError ignored) {
            ModuleRuntime.log("DexKit index could not be closed: " + Reflect.describe(ignored));
        }
    }
}
