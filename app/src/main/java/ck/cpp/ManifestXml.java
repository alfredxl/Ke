package ck.cpp;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * <br> ClassName:   ${className}
 * <br> Description:
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2018/4/23 15:13
 */
public class ManifestXml {

    public static String getMetaData(Context context, String metaKey) {
        try {
            ApplicationInfo recInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            if (recInfo != null && recInfo.metaData != null) {
                return recInfo.metaData.getString(metaKey);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getApplicationReallyName(Context context, String applicationAttributeName) {
        Object assets = RefInvoke.instanceObject("android.content.res.AssetManager");
        int cookie = (int) RefInvoke.invokeMethod("android.content.res.AssetManager", "addAssetPath", assets,
                new Class[]{String.class}, new Object[]{context.getApplicationInfo().sourceDir});
        XmlResourceParser xmlParser = (XmlResourceParser) RefInvoke.invokeMethod("android.content.res.AssetManager", "openXmlResourceParser", assets,
                new Class[]{int.class, String.class}, new Object[]{cookie, "AndroidManifest.xml"});
        return xmlToValue(xmlParser, applicationAttributeName);
    }

    private static String xmlToValue(XmlResourceParser xmlParser, String applicationAttributeName) {
        try {
            int event = xmlParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if ("application".equals(xmlParser.getName())) {
                            String value = null;
                            for (int i = 0; i < xmlParser.getAttributeCount(); i++) {
                                if (applicationAttributeName.equals(xmlParser.getAttributeName(i))) {
                                    value = xmlParser.getAttributeValue(i);
                                }
                            }
                            xmlParser.close();
                            return value;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        break;
                }
                event = xmlParser.next();
            }
            xmlParser.close();
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
