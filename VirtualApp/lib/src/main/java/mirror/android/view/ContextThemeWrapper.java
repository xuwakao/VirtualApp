package mirror.android.view;

import android.content.res.Resources;

import mirror.RefClass;
import mirror.RefObject;

public class ContextThemeWrapper {
    public static Class<?> TYPE = RefClass.load(ContextThemeWrapper.class, android.view.ContextThemeWrapper.class);
    public static RefObject<Resources> mResources;
}
