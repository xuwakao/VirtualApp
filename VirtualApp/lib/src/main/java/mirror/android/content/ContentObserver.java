package mirror.android.content;

import android.os.Handler;

import mirror.RefClass;
import mirror.RefObject;

public class ContentObserver {
    public static Class<?> TYPE = RefClass.load(ContentObserver.class, android.database.ContentObserver.class);
    public static RefObject<Object> mTransport;
    public static RefObject<Handler> mHandler;

    public static class Transport {
        public static Class<?> TYPE = RefClass.load(Transport.class, "android.database.ContentObserver$Transport");
        public static RefObject<android.database.ContentObserver> mContentObserver;

    }
}
