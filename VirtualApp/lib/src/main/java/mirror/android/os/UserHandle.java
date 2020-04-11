package mirror.android.os;

import android.annotation.TargetApi;
import android.content.Intent;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefConstructor;
import mirror.RefStaticInt;
import mirror.RefStaticMethod;

@TargetApi(17)
public class UserHandle {
    public static Class<?> TYPE = RefClass.load(UserHandle.class, android.os.UserHandle.class);
    public static RefStaticMethod<Integer> myUserId;
    @MethodParams({int.class})
    public static RefConstructor<android.os.UserHandle> ctor;
}
