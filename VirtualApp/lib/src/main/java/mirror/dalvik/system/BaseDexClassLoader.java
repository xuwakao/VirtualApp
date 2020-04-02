package mirror.dalvik.system;

import java.net.URL;
import java.util.Enumeration;

import mirror.RefClass;
import mirror.RefMethod;
import mirror.RefObject;

public class BaseDexClassLoader {
    public static Class<?> TYPE = RefClass.load(BaseDexClassLoader.class, "dalvik.system.BaseDexClassLoader");
    public static RefMethod<URL> findResource;
    public static RefMethod<Enumeration<URL>> findResources;
    public static RefMethod<String> findLibrary;
    public static RefMethod<Package> getPackage;

    //DexPathList
    public static RefObject<Object> pathList;
}
