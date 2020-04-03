package com.lody.virtual.plugin.stub;

import android.app.Activity;

import com.lody.virtual.plugin.PluginCore;
import com.lody.virtual.plugin.PluginImpl;

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
