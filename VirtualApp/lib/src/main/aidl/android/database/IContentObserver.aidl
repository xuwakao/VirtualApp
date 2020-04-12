// IContentObserver.aidl
package android.database;

import android.net.Uri;
interface IContentObserver {
    oneway void onChange(boolean selfUpdate, in Uri uri, int userId);
}
