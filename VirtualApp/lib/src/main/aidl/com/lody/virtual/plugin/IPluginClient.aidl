// IPluginClient.aidl
package com.lody.virtual.plugin;

interface IPluginClient {
    IBinder getAppThread();
    IBinder getToken();
}