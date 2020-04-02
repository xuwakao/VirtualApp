package com.lody.virtual.client.hook.plugin.stub;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.plugin.PluginCore;
import com.lody.virtual.client.hook.plugin.PluginImpl;
import com.lody.virtual.client.hook.proxies.am.HCallbackStub;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.remote.StubActivityRecord;

/**
 * @author Lody
 */
public abstract class PluginStubActivity extends Activity {

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        // The savedInstanceState's classLoader is not exist.
//        super.onCreate(null);
//        finish();
//        // It seems that we have conflict with the other Android-Plugin-Framework.
//        Intent stubIntent = getIntent();
//        // Try to acquire the actually component information.
//        StubActivityRecord r = new StubActivityRecord(stubIntent);
//        if (r.intent != null) {
//            Intent intent = r.intent;
////            if (intent.getComponent() != null) {
////                ComponentName component = new ComponentName(getPackageName(), intent.getComponent().getClassName());
////                intent.setComponent(component);
////            }
//            PluginImpl plugin = PluginCore.get().getClient(stubIntent.getComponent().getClassName());
//            intent.setExtrasClassLoader(plugin.getPluginDexClassLoader());
////            r.info.applicationInfo = VirtualCore.get().getContext().getApplicationInfo();
////            startActivity(stubIntent);
//            startActivity(intent);
//        }
//    }

    public static class P0 extends PluginStubActivity {
    }

    public static class P1 extends PluginStubActivity {
    }

    public static class P2 extends PluginStubActivity {
    }

}
