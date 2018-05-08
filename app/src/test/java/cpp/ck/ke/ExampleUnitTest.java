package cpp.ck.ke;


import org.junit.Test;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;

import ck.cpp.Cut;
import cn.wjdiankong.main.Utils;
import cn.wjdiankong.main.XmlMain;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void testDex() {
        // 直接加壳dex
        SplitDexUtil.mergeDex();
    }

    @Test
    public void testAPK() {
        // 直接对APK文件中的dex进行加壳, 并删除签名，修改manifest文件
        SplitAPKUtil.mergeApk();
    }

    @Test
    public void testManifest(){
        SplitManifestUtil.manifest();
    }
}