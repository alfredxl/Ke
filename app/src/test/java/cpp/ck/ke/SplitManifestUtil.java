package cpp.ck.ke;

import com.lianggzone.AndroidXMLPrinter;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;

/**
 * <br> ClassName:   ${className}
 * <br> Description:
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2018/4/26 11:47
 */
public class SplitManifestUtil {
    private static final String SRC = "src/test/java/cpp/ck/ke/manifest";

    public static void manifest() {
        File oldApk = new File(SRC + File.separator + "oldApk.apk");
        File manifest = new File(SRC + File.separator + "AndroidManifest.xml");
        if (manifest.exists()) {
            manifest.delete();
        }
        ZipUtil.unpackEntry(oldApk, "AndroidManifest.xml", manifest);
        System.out.print(AndroidXMLPrinter.getInfo(manifest.getAbsolutePath()));
    }
}
