package my.MrxSiN.syncthingliveupdate;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/**
 * Loads drawables from the module's own APK while running inside the host.
 *
 * The module's classes are injected into the host process, but its resources are
 * not, so they are opened from the module's application info the framework hands
 * over.
 */
final class ModuleDrawables {

    private ModuleDrawables() {
    }

    /** The drawable, or {@code null} when the module's resources cannot be opened. */
    static Drawable load(Context hostContext, int id) {
        ApplicationInfo module = ModuleRuntime.moduleApplicationInfo();
        if (module == null) {
            return null;
        }
        try {
            return hostContext.getPackageManager()
                    .getResourcesForApplication(module)
                    .getDrawable(id, null);
        } catch (PackageManager.NameNotFoundException | RuntimeException unavailable) {
            return null;
        }
    }
}
