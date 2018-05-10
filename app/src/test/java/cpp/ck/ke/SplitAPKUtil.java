package cpp.ck.ke;


import org.zeroturnaround.zip.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.Adler32;

import ck.cpp.Cryptographic;
import ck.cpp.Cut;
import cn.wjdiankong.main.XmlMain;

/**
 * <br> ClassName:   ${className}
 * <br> Description:
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2018/4/19 16:26
 */
public class SplitAPKUtil {

    private static final String SRC = "src/test/java/cpp/ck/ke/apk";

    public static void mergeApk() {
        try {
            // 原始APK
            File oldApk = new File(SRC + File.separator + "oldApk.apk");
            // 壳DEX
            File forceDex = new File(SRC + File.separator + "force.dex");
            //以二进制形式读出解壳dex
            byte[] unShellDexArray = readFileBytes(forceDex);
            // 加密密钥
            byte[] byteCry = new byte[32];
            new Random().nextBytes(byteCry);
            int dexNum = 2;
            byte[] dexByteNum = ZipUtil.unpackEntry(oldApk, "classes.dex");
            List<byte[]> dexArray = new ArrayList<>();
            while (dexByteNum != null) {
                dexArray.add(Cryptographic.encryptByteC(dexByteNum, byteCry));
                dexByteNum = ZipUtil.unpackEntry(oldApk, "classes" + dexNum + ".dex");
                dexNum++;
            }
            int unShellDexLen = unShellDexArray.length;
            int totalLen = unShellDexLen + 4 + 32;
            for (byte[] sunDexByte : dexArray) {
                totalLen += sunDexByte.length + 4;
            }
            // 用来存储壳dex和加密dex
            byte[] newDex = new byte[totalLen];
            // 拷贝的起始位置
            int tempCopyNumStart = 0;
            // 拷贝的长度
            int tempCopyNumLength = unShellDexLen;
            // 拷贝壳dex到newDex
            System.arraycopy(unShellDexArray, 0, newDex, tempCopyNumStart, tempCopyNumLength);
            // 移动拷贝起始位置
            tempCopyNumStart += tempCopyNumLength;
            for (byte[] sunDexByte : dexArray) {
                // 拷贝内容
                // 重新赋值拷贝长度
                tempCopyNumLength = sunDexByte.length;
                System.arraycopy(sunDexByte, 0, newDex, tempCopyNumStart, tempCopyNumLength);
                // 移动拷贝起始位置
                tempCopyNumStart += tempCopyNumLength;
                // 拷贝长度字节
                // 计算长度字节数组
                byte[] lengthByte = Cut.intToByteArray(tempCopyNumLength);
                // 重新赋值拷贝长度
                tempCopyNumLength = 4;
                System.arraycopy(lengthByte, 0, newDex, tempCopyNumStart, tempCopyNumLength);
                // 移动拷贝起始位置
                tempCopyNumStart += tempCopyNumLength;
            }
            // 拷贝dex个数
            tempCopyNumLength = 4;
            System.arraycopy(Cut.intToByteArray(dexArray.size()), 0, newDex, tempCopyNumStart, tempCopyNumLength);
            // 移动拷贝起始位置
            tempCopyNumStart += tempCopyNumLength;
            // 拷贝密钥
            tempCopyNumLength = 32;
            System.arraycopy(byteCry, 0, newDex, tempCopyNumStart, tempCopyNumLength);
            //修改DEX file size文件头
            fixFileSizeHeader(newDex);
            //修改DEX SHA1 文件头
            fixSHA1Header(newDex);
            //修改DEX CheckSum文件头
            fixCheckSumHeader(newDex);

            File newApkFile = new File(SRC + File.separator + "newApk.apk");
            if (newApkFile.exists()) {
                newApkFile.delete();
            }
            // 替换原始APK中的dex为加壳的dex
            ZipUtil.replaceEntry(oldApk, "classes.dex", newDex, newApkFile);
            // 删除签名信息
            ZipUtil.removeDirectoryEntries(newApkFile, new String[]{"META-INF"});
            // 删除多余的dex
            if (dexArray.size() > 1) {
                for (int i = 2; i <= dexArray.size(); i++) {
                    ZipUtil.removeEntry(newApkFile, "classes" + String.valueOf(i) + ".dex");
                }
            }
            // 修改manifest文件的application
           /* byte[] manifest = ZipUtil.unpackEntry(newApkFile, "AndroidManifest.xml");
            String applicationName = XmlMain.getApplicationAttrValue(manifest, "name");
            manifest = XmlMain.replaceApplicationAttrValue(manifest, "name", "ck.cpp.ProxyApplication");
            if (applicationName != null && applicationName.length() > 0) {
                manifest = XmlMain.addApplicationAttrValue(manifest, "reallyTnt", applicationName);
            }
            ZipUtil.replaceEntry(newApkFile, "AndroidManifest.xml", manifest);*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 修改dex头，CheckSum 校验码
     *
     * @param dexBytes dexBytes
     */
    private static void fixCheckSumHeader(byte[] dexBytes) {
        Adler32 adler = new Adler32();
        adler.update(dexBytes, 12, dexBytes.length - 12);//从12到文件末尾计算校验码
        long value = adler.getValue();
        int va = (int) value;
        byte[] newCopy = intToByte(va);
        //高位在前，低位在前掉个个
        byte[] recs = new byte[4];
        for (int i = 0; i < 4; i++) {
            recs[i] = newCopy[newCopy.length - 1 - i];
        }
        System.arraycopy(recs, 0, dexBytes, 8, 4);//效验码赋值（8-11）
    }


    /**
     * int 转byte[]
     *
     * @param number number
     * @return byte[]
     */
    public static byte[] intToByte(int number) {
        byte[] b = new byte[4];
        for (int i = 3; i >= 0; i--) {
            b[i] = (byte) (number % 256);
            number >>= 8;
        }
        return b;
    }

    /**
     * 修改dex头 sha1值
     *
     * @param dexBytes dexBytes
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    private static void fixSHA1Header(byte[] dexBytes)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(dexBytes, 32, dexBytes.length - 32);//从32为到结束计算sha--1
        byte[] newData = md.digest();
        System.arraycopy(newData, 0, dexBytes, 12, 20);//修改sha-1值（12-31）
        //输出sha-1值，可有可无
        StringBuilder hexStr = new StringBuilder();
        for (byte SunNewData : newData) {
            hexStr.append(Integer.toString((SunNewData & 0xff) + 0x100, 16)
                    .substring(1));
        }
    }

    /**
     * 修改dex头 file_size值
     *
     * @param dexBytes dexBytes
     */
    private static void fixFileSizeHeader(byte[] dexBytes) {
        //新文件长度
        byte[] newFileByte = intToByte(dexBytes.length);
        byte[] refs = new byte[4];
        //高位在前，低位在前掉个个
        for (int i = 0; i < 4; i++) {
            refs[i] = newFileByte[newFileByte.length - 1 - i];
        }
        System.arraycopy(refs, 0, dexBytes, 32, 4);//修改（32-35）
    }

    /**
     * 以二进制读出文件内容
     *
     * @param file File
     * @return 文件的字节码
     * @throws IOException IOException
     */
    private static byte[] readFileBytes(File file) throws IOException {
        byte[] arrayOfByte = new byte[1024];
        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        while (true) {
            int i = fis.read(arrayOfByte);
            if (i != -1) {
                localByteArrayOutputStream.write(arrayOfByte, 0, i);
            } else {
                return localByteArrayOutputStream.toByteArray();
            }
        }
    }
}
