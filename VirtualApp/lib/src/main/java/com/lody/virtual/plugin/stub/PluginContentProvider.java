package com.lody.virtual.plugin.stub;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Process;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.plugin.core.PluginCore;
import com.lody.virtual.plugin.PluginImpl;
import com.lody.virtual.helper.compat.BundleCompat;

/**
 * @author Lody
 *
 */
public class PluginContentProvider extends ContentProvider {

	private PluginImpl mClient;

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Bundle call(String method, String arg, Bundle extras) {
		if ("_VA_|_init_plugin_".equals(method)) {
			return initPlugin(extras);
		}
		return null;
	}

	private Bundle initPlugin(Bundle extras) {
		ConditionVariable lock = VirtualCore.get().getInitLock();
		if (lock != null) {
			lock.block();
		}
		IBinder token = BundleCompat.getBinder(extras,"_VA_|_binder_");
		int vuid = extras.getInt("_VA_|_vuid_");
		int vpid = extras.getInt("_VA_|_vpid_");
		String pkgName = extras.getString("_VA_|_pkg_");
		mClient = PluginImpl.create();
		mClient.initPlugin(token, vuid, vpid, pkgName);
		Bundle res = new Bundle();
		BundleCompat.putBinder(res, "_VA_|_client_", mClient.asBinder());
		res.putInt("_VA_|_pid_", Process.myPid());
		PluginCore.get().putPlugin(vpid, mClient);
		return res;
	}

	public PluginImpl getClient() {
		return mClient;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return null;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	public static class P0 extends PluginContentProvider {
	}

	public static class P1 extends PluginContentProvider {
	}

	public static class P2 extends PluginContentProvider {
	}

}
