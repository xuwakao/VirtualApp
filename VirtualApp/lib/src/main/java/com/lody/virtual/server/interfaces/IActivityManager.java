package com.lody.virtual.server.interfaces;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;

import com.lody.virtual.remote.*;

/**
 * @author Lody
 */
public interface IActivityManager {

    int initProcess(String packageName, String processName, int userId) throws RemoteException;

    int getFreeStubCount() throws RemoteException;

    int getSystemPid() throws RemoteException;

    int getUidByPid(int pid) throws RemoteException;

    int getUidByPluginId(int pluginId) throws RemoteException;

    int getVPidByPackage(String processName, int uid) throws RemoteException;

    boolean isAppProcess(String processName) throws RemoteException;

    boolean isAppRunning(String packageName, int userId) throws RemoteException;

    boolean isAppPid(int pid) throws RemoteException;

    String getAppProcessName(int pid) throws RemoteException;

    java.util.List<String> getProcessPkgList(int pid) throws RemoteException;

    void killAllApps() throws RemoteException;

    void killAppByPkg(String pkg, int userId) throws RemoteException;

    void killApplicationProcess(String processName, int vuid) throws RemoteException;

    void dump() throws RemoteException;

    String getInitialPackage(int pid) throws RemoteException;

    void handleApplicationCrash() throws RemoteException;

    void appDoneExecuting() throws RemoteException;

    void appDoneExecuting(int pluginId) throws RemoteException;

    int startActivities(Intent[] intents, String[] resolvedTypes, IBinder token, Bundle options, int userId) throws RemoteException;

    int startActivity(Intent intent, ActivityInfo info, IBinder resultTo, Bundle options, String resultWho, int requestCode, int userId) throws RemoteException;

    void onActivityCreated(ComponentName component, ComponentName caller, IBinder token, Intent intent, String affinity, int taskId, int launchMode, int flags, int pluginId) throws RemoteException;

    void onActivityResumed(int userId, IBinder token) throws RemoteException;

    boolean onActivityDestroyed(IBinder token) throws RemoteException;

    ComponentName getActivityClassForToken(IBinder token) throws RemoteException;

    String getCallingPackage(IBinder token) throws RemoteException;

    ComponentName getCallingActivity(IBinder token) throws RemoteException;

    AppTaskInfo getTaskInfo(int taskId) throws RemoteException;

    String getPackageForToken(IBinder token) throws RemoteException;

    boolean isVAServiceToken(IBinder token) throws RemoteException;

    ComponentName startService(IBinder caller, Intent service, String resolvedType, int userId) throws RemoteException;

    int stopService(IBinder caller, Intent service, String resolvedType, int userId) throws RemoteException;

    boolean stopServiceToken(ComponentName className, IBinder token, int startId) throws RemoteException;

    void setServiceForeground(ComponentName className, IBinder token, int id, Notification notification, boolean removeNotification, int userId) throws RemoteException;

    int bindService(IBinder caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, int userId) throws RemoteException;

    boolean unbindService(IServiceConnection connection) throws RemoteException;

    void unbindFinished(IBinder token, Intent service, boolean doRebind) throws RemoteException;

    void serviceDoneExecuting(IBinder token, int type, int startId, int res) throws RemoteException;

    IBinder peekService(Intent service, String resolvedType, int userId) throws RemoteException;

    void publishService(IBinder token, Intent intent, IBinder service) throws RemoteException;

    VParceledListSlice getServices(int maxNum, int flags, int userId) throws RemoteException;

    IBinder acquireProviderClient(int userId, ProviderInfo info) throws RemoteException;

    PendingIntentData getPendingIntent(IBinder binder) throws RemoteException;

    void addPendingIntent(IBinder binder, String packageName) throws RemoteException;

    void removePendingIntent(IBinder binder) throws RemoteException;

    String getPackageForIntentSender(IBinder binder) throws RemoteException;

    void processRestarted(String packageName, String processName, int userId) throws RemoteException;

    void broadcastFinish(PendingResultData res) throws RemoteException;

    void notifyBadgerChange(BadgerInfo info) throws RemoteException;
}
