package com.lody.virtual.server.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import com.lody.virtual.remote.InstalledAppInfo;

/**
 * @author Lody
 */

public class PackageSetting implements Parcelable {

    public static final Creator<PackageSetting> CREATOR = new Creator<PackageSetting>() {
        @Override
        public PackageSetting createFromParcel(Parcel source) {
            return new PackageSetting(source);
        }

        @Override
        public PackageSetting[] newArray(int size) {
            return new PackageSetting[size];
        }
    };
    private static final PackageUserState DEFAULT_USER_STATE = new PackageUserState();
    public String packageName;
    public String apkPath;
    public String libPath;
    public boolean dependSystem;
    @Deprecated
    public boolean skipDexOpt;
    public boolean isPlugin;
    public int appId;
    public long firstInstallTime;
    public long lastUpdateTime;
    private SparseArray<PackageUserState> userState = new SparseArray<>();

    public PackageSetting() {
    }

    protected PackageSetting(Parcel in) {
        this.packageName = in.readString();
        this.apkPath = in.readString();
        this.libPath = in.readString();
        this.dependSystem = in.readByte() != 0;
        this.skipDexOpt = in.readByte() != 0;
        this.isPlugin = in.readByte() != 0;
        this.appId = in.readInt();
        this.userState = in.readSparseArray(PackageUserState.class.getClassLoader());
    }

    public InstalledAppInfo getAppInfo() {
        return new InstalledAppInfo(packageName, apkPath, libPath, dependSystem, skipDexOpt, appId, isPlugin);
    }

    PackageUserState modifyUserState(int userId) {
        PackageUserState state = userState.get(userId);
        if (state == null) {
            state = new PackageUserState();
            userState.put(userId, state);
        }
        return state;
    }

    void setUserState(int userId, boolean launched, boolean hidden, boolean installed, boolean plugin) {
        PackageUserState state = modifyUserState(userId);
        state.launched = launched;
        state.hidden = hidden;
        state.installed = installed;
        state.plugin = plugin;
    }

    PackageUserState readUserState(int userId) {
        PackageUserState state = userState.get(userId);
        if (state != null) {
            return state;
        }
        return DEFAULT_USER_STATE;
    }

    void removeUser(int userId) {
        userState.delete(userId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeString(this.apkPath);
        dest.writeString(this.libPath);
        dest.writeByte(this.dependSystem ? (byte) 1 : (byte) 0);
        dest.writeByte(this.skipDexOpt ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isPlugin ? (byte) 1 : (byte) 0);
        dest.writeInt(this.appId);
        dest.writeSparseArray((SparseArray) this.userState);
    }

    public boolean isLaunched(int userId) {
        return readUserState(userId).launched;
    }

    public boolean isHidden(int userId) {
        return readUserState(userId).hidden;
    }

    public boolean isInstalled(int userId) {
        return readUserState(userId).installed;
    }

    public boolean isPlugin(int userId) {
        return readUserState(userId).plugin;
    }

    public void setLaunched(int userId, boolean launched) {
        modifyUserState(userId).launched = launched;
    }

    public void setHidden(int userId, boolean hidden) {
        modifyUserState(userId).hidden = hidden;
    }

    public void setInstalled(int userId, boolean installed) {
        modifyUserState(userId).installed = installed;
    }

}
