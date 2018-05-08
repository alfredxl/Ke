package ck.cpp;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

/**
 * <br> ClassName:   ${className}
 * <br> Description:
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2018/4/16 11:48
 */
public class ProxyApplication extends Application {
    private Application app;
    private String appClassName;
    public static ProxyApplication mProxyApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        mProxyApplication = this;
        if (!TextUtils.isEmpty(appClassName) && app != null) {
            ProxyDex.onCreateProxy(app, appClassName, app);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        initAppClassName(base);
        ProxyDex.loaderDex(base);
        if (!TextUtils.isEmpty(appClassName)) {
            app = ProxyDex.attachBaseContextProxy(base, appClassName);
        }
    }

    private void initAppClassName(Context base) {
        appClassName = ManifestXml.getMetaData(base, "really_name");
    }
}
