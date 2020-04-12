package mirror.android.content;

import android.net.Uri;
import android.os.Handler;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;
import mirror.RefObject;

public class ContentObserver {
    public static Class<?> TYPE = RefClass.load(ContentObserver.class, android.database.ContentObserver.class);
    public static RefObject<Object> mTransport;
    public static RefObject<Handler> mHandler;
    @MethodParams({boolean.class, Uri.class, int.class})
    public static RefMethod<Void> onChange;

    public static class Transport {
        public static Class<?> TYPE = RefClass.load(Transport.class, "android.database.ContentObserver$Transport");
        public static RefObject<android.database.ContentObserver> mContentObserver;
        @MethodParams({boolean.class, Uri.class, int.class})
        public static RefMethod<Void> onChange;

    }
}
