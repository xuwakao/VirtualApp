package mirror.android.app;

import mirror.RefClass;
import mirror.RefObject;

public class Application {
    public static Class<?> TYPE = RefClass.load(Application.class, android.app.Application.class);
    public static RefObject<Object> mLoadedApk;


}
