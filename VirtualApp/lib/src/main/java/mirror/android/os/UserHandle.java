package mirror.android.os;

import android.annotation.TargetApi;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

@TargetApi(17)
public class UserHandle {
    public static Class<?> TYPE = RefClass.load(UserHandle.class, android.os.UserHandle.class);
    @MethodParams(int.class)
    public static RefStaticMethod<android.os.UserHandle> of;
}
