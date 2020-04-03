package com.lody.virtual.plugin;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.plugin.core.PluginCore;
import com.lody.virtual.plugin.utils.PluginDexClassLoaderPatch;
import com.lody.virtual.helper.utils.Reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class PluginDexClassLoader extends DexClassLoader {

    private static final String TAG = "PluginDexClassLoader";

    private final ClassLoader mHostClassLoader;

    private static Method sLoadClassMethod;

    private String mPluginName;

    /**
     * 初始化插件的DexClassLoader的构造函数。插件化框架会调用此函数。
     *
     * @param pluginName         the plugin's name
     * @param dexPath            the list of jar/apk files containing classes and
     *                           resources, delimited by {@code File.pathSeparator}, which
     *                           defaults to {@code ":"} on Android
     * @param optimizedDirectory directory where optimized dex files
     *                           should be written; must not be {@code null}
     * @param librarySearchPath  the list of directories containing native
     *                           libraries, delimited by {@code File.pathSeparator}; may be
     *                           {@code null}
     * @param parent             the parent class loader
     */
    public PluginDexClassLoader(String pluginName, String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);

        mPluginName = pluginName;

//        installMultiDexesBeforeLollipop(pi, dexPath, parent);
        mHostClassLoader = VirtualCore.get().getContext().getClassLoader();

        initMethods(mHostClassLoader);
    }

    private static void initMethods(ClassLoader cl) {
        Class<?> clz = cl.getClass();
        if (sLoadClassMethod == null) {
            try {
                sLoadClassMethod = Reflect.on(clz).exactMethod("loadClass", new Class[]{String.class, Boolean.TYPE});
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError("loadClass");
            }
        }
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> pc = null;
        ClassNotFoundException cnfException = null;
        try {
//            VLog.d(TAG, "plugin load class : " + className);
            pc = super.loadClass(className, resolve);
            if (pc != null) {
                return pc;
            }
        } catch (ClassNotFoundException e) {
            // Do not throw "e" now
            cnfException = e;

            if (PluginDexClassLoaderPatch.need2LoadFromHost(className)) {
                try {
                    return loadClassFromHost(className, resolve);
                } catch (ClassNotFoundException e1) {
                    // Do not throw "e1" now
                    cnfException = e1;
                }
            }
        }

        // 若插件里没有此类，则会从宿主ClassLoader中找，找到了则直接返回
        // 注意：需要读取isUseHostClassIfNotFound开关。默认为关闭的。可参见该开关的说明
        if (PluginCore.get().isUseHostClassIfNotFound()) {
            try {
                return loadClassFromHost(className, resolve);
            } catch (ClassNotFoundException e) {
                // Do not throw "e" now
                cnfException = e;
            }
        }

        // At this point we can throw the previous exception
        if (cnfException != null) {
            throw cnfException;
        }
        return null;
    }

    private Class<?> loadClassFromHost(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> c;
        try {
            c = (Class<?>) sLoadClassMethod.invoke(mHostClassLoader, className, resolve);
        } catch (IllegalAccessException e) {
            // Just rethrow
            throw new ClassNotFoundException("Calling the loadClass method failed (IllegalAccessException)", e);
        } catch (InvocationTargetException e) {
            // Just rethrow
            throw new ClassNotFoundException("Calling the loadClass method failed (InvocationTargetException)", e);
        }
        return c;
    }

//    /**
//     * install extra dexes
//     *
//     * @param pi
//     * @param dexPath
//     * @param parent
//     * @deprecated apply to ROM before Lollipop,may be deprecated
//     */
//    private void installMultiDexesBeforeLollipop(PluginInfo pi, String dexPath, ClassLoader parent) {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return;
//        }
//
//        try {
//
//            // get paths of extra dex
//            List<File> dexFiles = getExtraDexFiles(pi, dexPath);
//
//            if (dexFiles != null && dexFiles.size() > 0) {
//
//                List<Object[]> allElements = new LinkedList<>();
//
//                // get dexElements of main dex
//                Class<?> clz = Class.forName("dalvik.system.BaseDexClassLoader");
//                Object pathList = ReflectUtils.readField(clz, this, "pathList");
//                Object[] mainElements = (Object[]) ReflectUtils.readField(pathList.getClass(), pathList, "dexElements");
//                allElements.add(mainElements);
//
//                // get dexElements of extra dex (need to load dex first)
//                String optimizedDirectory = pi.getExtraOdexDir().getAbsolutePath();
//
//                for (File file : dexFiles) {
//                    if (LogDebug.LOG && RePlugin.getConfig().isPrintDetailLog()) {
//                        LogDebug.d(TAG, "dex file:" + file.getName());
//                    }
//
//                    DexClassLoader dexClassLoader = new DexClassLoader(file.getAbsolutePath(), optimizedDirectory, optimizedDirectory, parent);
//
//                    Object obj = ReflectUtils.readField(clz, dexClassLoader, "pathList");
//                    Object[] dexElements = (Object[]) ReflectUtils.readField(obj.getClass(), obj, "dexElements");
//                    allElements.add(dexElements);
//                }
//
//                // combine Elements
//                Object combineElements = combineArray(allElements);
//
//                // rewrite Elements combined to classLoader
//                ReflectUtils.writeField(pathList.getClass(), pathList, "dexElements", combineElements);
//
//                // delete extra dex, after optimized
//                FileUtils.forceDelete(pi.getExtraDexDir());
//
//                //Test whether the Extra Dex is installed
//                if (LogDebug.LOG && RePlugin.getConfig().isPrintDetailLog()) {
//
//                    Object object = ReflectUtils.readField(pathList.getClass(), pathList, "dexElements");
//                    int length = Array.getLength(object);
//                    LogDebug.d(TAG, "dexElements length:" + length);
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * combine dexElements Array
//     *
//     * @param allElements all dexElements of dexes
//     * @return the combined dexElements
//     */
//    private Object combineArray(List<Object[]> allElements) {
//
//        int startIndex = 0;
//        int arrayLength = 0;
//        Object[] originalElements = null;
//
//        for (Object[] elements : allElements) {
//
//            if (originalElements == null) {
//                originalElements = elements;
//            }
//
//            arrayLength += elements.length;
//        }
//
//        Object[] combined = (Object[]) Array.newInstance(
//                originalElements.getClass().getComponentType(), arrayLength);
//
//        for (Object[] elements : allElements) {
//
//            System.arraycopy(elements, 0, combined, startIndex, elements.length);
//            startIndex += elements.length;
//        }
//
//        return combined;
//    }
//
//    /**
//     * get paths of extra dex
//     *
//     * @param pi
//     * @param dexPath
//     * @return the File list of the extra dexes
//     */
//    private List<File> getExtraDexFiles(PluginInfo pi, String dexPath) {
//        ZipFile zipFile = null;
//        List<File> files = null;
//
//        try {
//
//            if (pi != null) {
//                zipFile = new ZipFile(dexPath);
//                files = traverseExtraDex(pi, zipFile);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            CloseableUtils.closeQuietly(zipFile);
//        }
//
//        return files;
//
//    }
//
//    /**
//     * traverse extra dex files
//     *
//     * @param pi
//     * @param zipFile
//     * @return the File list of the extra dexes
//     */
//    private static List<File> traverseExtraDex(PluginInfo pi, ZipFile zipFile) {
//
//        String dir = null;
//        List<File> files = new LinkedList<>();
//        Enumeration<? extends ZipEntry> entries = zipFile.entries();
//        while (entries.hasMoreElements()) {
//            ZipEntry entry = entries.nextElement();
//            String name = entry.getName();
//            if (name.contains("../")) {
//                // 过滤，防止被攻击
//                continue;
//            }
//
//            try {
//                if (name.contains(".dex") && !name.equals("classes.dex")) {
//
//                    if (dir == null) {
//                        dir = pi.getExtraDexDir().getAbsolutePath();
//                    }
//
//                    File file = new File(dir, name);
//                    extractFile(zipFile, entry, file);
//                    files.add(file);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }
//
//        return files;
//    }
//
//    /**
//     * extract File
//     *
//     * @param zipFile
//     * @param ze
//     * @param outFile
//     * @throws IOException
//     */
//    private static void extractFile(ZipFile zipFile, ZipEntry ze, File outFile) throws IOException {
//        InputStream in = null;
//        try {
//            in = zipFile.getInputStream(ze);
//            FileUtils.copyInputStreamToFile(in, outFile);
//            if (LogDebug.LOG && RePlugin.getConfig().isPrintDetailLog()) {
//                LogDebug.d(TAG, "extractFile(): Success! fn=" + outFile.getName());
//            }
//        } finally {
//            CloseableUtils.closeQuietly(in);
//        }
//    }
}
