package ck.cpp;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * <br> ClassName:   ${className}
 * <br> Description:
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2018/4/19 13:54
 */
public class SplitPlay {

    /**
     * 释放被加壳的dex文件
     *
     * @param context Context
     * @return 文件名
     */
    public static String splitPayLoadFromDex(Context context) {
        List<File> files = splitPayLoadFromDex2File(context);
        if (files != null && files.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            for (File fl : files) {
                stringBuilder.append(fl.getAbsolutePath() + File.pathSeparator);
            }
            return stringBuilder.toString();
        }
        return null;
    }

    private static List<File> splitPayLoadFromDex2File(Context context) {
        try {
            byte[] dexData = readDexFileFromApk(context);
            int oldDexLen = dexData.length;
            //取出密钥
            byte[] byteCry = new byte[32];
            // 拷贝的长度
            int tempCopyNumLength = 32;
            // 拷贝的起始位置
            int tempCopyNumStart = oldDexLen - tempCopyNumLength;
            System.arraycopy(dexData, tempCopyNumStart, byteCry, 0, tempCopyNumLength);
            // 取出dex个数
            byte[] dexNumByte = new byte[4];
            // 重置拷贝长度
            tempCopyNumLength = 4;
            // 重置拷贝起始位置
            tempCopyNumStart -= tempCopyNumLength;
            System.arraycopy(dexData, tempCopyNumStart, dexNumByte, 0, tempCopyNumLength);
            int dexNum = Cut.byteArrayToInt(dexNumByte);
            String versionCode = String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
            List<File> files = new ArrayList<>();
            // 定义文件名
            File parent = new File(context.getFilesDir().getAbsolutePath() + File.separator + "synchronous_odx_dex");
            if (!parent.exists()) {
                parent.mkdirs();
            }
            for (int i = dexNum - 1; i >= 0; i--) {
                // 取出dex长度
                byte[] sunDexLengthByte = new byte[4];
                // 重置拷贝长度
                tempCopyNumLength = 4;
                //重置拷贝起始位置
                tempCopyNumStart -= tempCopyNumLength;
                System.arraycopy(dexData, tempCopyNumStart, sunDexLengthByte, 0, tempCopyNumLength);
                int sunDexLength = Cut.byteArrayToInt(sunDexLengthByte);
                File file = new File(parent.getAbsolutePath() + File.separator + versionCode + String.valueOf(sunDexLength) + String.valueOf(i) + ".dex");
                // 重置拷贝长度
                tempCopyNumLength = sunDexLength;
                //重置拷贝起始位置
                tempCopyNumStart -= tempCopyNumLength;
                if (!file.exists() || file.length() == 0) {
                    // 取出dex文件
                    byte[] dexFileByte = new byte[tempCopyNumLength];
                    System.arraycopy(dexData, tempCopyNumStart, dexFileByte, 0, tempCopyNumLength);
                    dexFileByte = Cryptographic.decodeByte(dexFileByte, byteCry);
                    try {
                        FileOutputStream localFileOutputStream = new FileOutputStream(file);
                        localFileOutputStream.write(dexFileByte);
                        localFileOutputStream.close();
                    } catch (IOException localIOException) {
                        throw new RuntimeException(localIOException);
                    }
                }
                files.add(file);
            }
            deleteDex(parent, versionCode);
            return files;
        } catch (IOException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void deleteDex(File file, String versionCode) {
        if (file.isFile()) {
            if (!file.getName().startsWith(versionCode)) {
                file.delete();
            }
        } else if (file.isDirectory()) {
            for (File fl : file.listFiles()) {
                deleteDex(fl, versionCode);
            }
        }
    }

    /**
     * 从apk包里面获取dex文件内容（byte）
     *
     * @param context Context
     * @return byte[]
     * @throws IOException IOException
     */
    private static byte[] readDexFileFromApk(Context context) throws IOException {
        ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(
                        context.getApplicationInfo().sourceDir)));
        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
            if (localZipEntry == null) {
                break;
            }
            if ("classes.dex".equals(localZipEntry.getName())) {
                byte[] arrayOfByte = new byte[1024];
                int i;
                while ((i = localZipInputStream.read(arrayOfByte)) != -1) {
                    dexByteArrayOutputStream.write(arrayOfByte, 0, i);
                }
            }
        }
        localZipInputStream.closeEntry();
        localZipInputStream.close();
        return dexByteArrayOutputStream.toByteArray();
    }
}
