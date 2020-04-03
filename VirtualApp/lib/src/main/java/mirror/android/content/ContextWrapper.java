package mirror.android.content;

import android.content.Context;

import mirror.RefClass;
import mirror.RefObject;

public class ContextWrapper {
    public static Class<?> TYPE = RefClass.load(ContextWrapper.class, android.content.ContextWrapper.class);
    public static RefObject<Context> mBase;
}
