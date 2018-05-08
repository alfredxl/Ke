package ck.cpp;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

/**
 * <br> ClassName:   ${className}
 * <br> Description:
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2018/4/19 16:13
 */
public class ProxyDex {
    public static void loaderDex(Context base) {
        loadPatchDex(base);
//        loadPatchDexMultiDex(base);
    }

    private static void loadPatchDex(Context base) {
        try {
            String dexPath = SplitPlay.splitPayLoadFromDex(base);
            if (!TextUtils.isEmpty(dexPath)) {
                Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread",
                        "currentActivityThread", new Class[]{}, new Object[]{});
                String packageName = base.getPackageName();
                Map mPackages = RefInvoke.getFieldObject("android.app.ActivityThread",
                        currentActivityThread, "mPackages");
                WeakReference wr = (WeakReference) mPackages.get(packageName);
                // 定义类装载器优化后的dex的存放路径
                File optFile = new File(base.getFilesDir().getAbsolutePath());
                if (!optFile.exists()) {
                    optFile.mkdirs();
                }
                DexClassLoader dLoader = new DexClassLoader(dexPath,
                        optFile.getAbsolutePath(), base.getFilesDir().getParentFile().getAbsolutePath() + File.separator + "lib",
                        base.getClassLoader());
                //替換成TargetApk.dex的ClassLoader
                RefInvoke.setFieldOjbect("android.app.LoadedApk",
                        "mClassLoader", wr.get(), dLoader);
                Log.i("ttag", "加载dex成功:" + new File(dexPath).getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadPatchDexMultiDex(Context context){
        List<File> files = SplitPlay.splitPayLoadFromDex2File(context);
        try {
            MultiLoad.installSecondaryDexes(context, files);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Application attachBaseContextProxy(Context base, String appClassName) {
        Log.i(base.getPackageName(), "reality_application : " + appClassName);
        Object currentActivityThread = RefInvoke.invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});
        Object mBoundApplication = RefInvoke.getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Object loadedApkInfo = RefInvoke.getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication",
                loadedApkInfo, null);
        Object oldApplication = RefInvoke.getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mInitialApplication");
        ArrayList<Application> mAllApplications = RefInvoke
                .getFieldObject("android.app.ActivityThread",
                        currentActivityThread, "mAllApplications");
        mAllApplications.remove(oldApplication);

        ApplicationInfo mApplicationInfo = RefInvoke
                .getFieldObject("android.app.LoadedApk", loadedApkInfo,
                        "mApplicationInfo");
        ApplicationInfo appInfo = RefInvoke
                .getFieldObject("android.app.ActivityThread$AppBindData",
                        mBoundApplication, "appInfo");
        mApplicationInfo.className = appClassName;
        appInfo.className = appClassName;
        Application app = (Application) RefInvoke.invokeMethod(
                "android.app.LoadedApk", "makeApplication", loadedApkInfo,
                new Class[]{boolean.class, Instrumentation.class},
                new Object[]{false, null});

        List<ProviderInfo> providers = RefInvoke.getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "providers");
        if (providers != null) {
            RefInvoke.invokeDeclaredMethod("android.app.ActivityThread", "installContentProviders",
                    currentActivityThread, new Class[]{Context.class, List.class}, new Object[]{app, providers});
            providers.clear();
        }

        ArrayMap mProviderMap = RefInvoke.getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mProviderMap");
        Iterator it = mProviderMap.values().iterator();
        while (it.hasNext()) {
            Object providerClientRecord = it.next();
            Object localProvider = RefInvoke.getFieldObject(
                    "android.app.ActivityThread$ProviderClientRecord",
                    providerClientRecord, "mLocalProvider");
            RefInvoke.setFieldOjbect("android.content.ContentProvider",
                    "mContext", localProvider, app);
        }
        return app;
    }

    public static void onCreateProxy(Context base, String appClassName, Application app) {
        if (!TextUtils.isDigitsOnly(appClassName)) {
            Log.i(base.getPackageName(), "reality_application : " + appClassName);
            Object currentActivityThread = RefInvoke.invokeStaticMethod(
                    "android.app.ActivityThread", "currentActivityThread",
                    new Class[]{}, new Object[]{});
            Object mBoundApplication = RefInvoke.getFieldObject(
                    "android.app.ActivityThread", currentActivityThread,
                    "mBoundApplication");
            Object loadedApkInfo = RefInvoke.getFieldObject(
                    "android.app.ActivityThread$AppBindData",
                    mBoundApplication, "info");

            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication",
                    loadedApkInfo, app);
            RefInvoke.setFieldOjbect("android.app.ActivityThread",
                    "mInitialApplication", currentActivityThread, app);
            app.onCreate();
        }
    }
}
