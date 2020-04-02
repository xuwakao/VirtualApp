package mirror.android.content.pm;

import android.content.pm.ApplicationInfo;

import mirror.RefClass;
import mirror.RefObject;

public class ComponentInfo {
    public static Class<?> TYPE = RefClass.load(ComponentInfo.class, android.content.pm.ComponentInfo.class);
    public static RefObject<ApplicationInfo> applicationInfo;

}
