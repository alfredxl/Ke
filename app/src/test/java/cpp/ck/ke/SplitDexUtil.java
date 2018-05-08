package cpp.ck.ke;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;

/**
 * <br> ClassName:   ${className}
 * <br> Description:
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2018/4/19 15:32
 */
public class SplitDexUtil {
    private static final String SRC = "src/test/java/cpp/ck/ke/dex";

    public static void mergeDex() {
        try {
            //需要加壳的dex
            File payloadSrcFile = new File(SRC + File.separator + "old.dex");
            //解壳dex
            File unShellDexFile = new File(SRC + File.separator + "force.dex");
            //以二进制形式读出apk，并进行加密处理//对源Apk进行加密操作
            byte[] payloadArray = encrypt(readFileBytes(payloadSrcFile));
            //以二进制形式读出dex
            byte[] unShellDexArray = readFileBytes(unShellDexFile);
            int payloadLen = payloadArray.length;
            int unShellDexLen = unShellDexArray.length;
            int totalLen = payloadLen + unShellDexLen + 4;//多出4字节是存放长度的。
            byte[] newDex = new byte[totalLen]; // 申请了新的长度
            //添加解壳代码
            System.arraycopy(unShellDexArray, 0, newDex, 0, unShellDexLen);//先拷贝dex内容
            //添加加密后的解壳数据
            System.arraycopy(payloadArray, 0, newDex, unShellDexLen, payloadLen);//再在dex内容后面拷贝apk的内容
            //添加解壳数据长度
            System.arraycopy(intToByte(payloadLen), 0, newDex, totalLen - 4, 4);//最后4为长度
            //修改DEX file size文件头
            fixFileSizeHeader(newDex);
            //修改DEX SHA1 文件头
            fixSHA1Header(newDex);
            //修改DEX CheckSum文件头
            fixCheckSumHeader(newDex);

            File file = new File(SRC + File.separator + "force.dex");
            file.delete();
            file.createNewFile();

            // 输出加壳后的dex
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            localFileOutputStream.write(newDex);
            localFileOutputStream.flush();
            localFileOutputStream.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //直接返回数据，读者可以添加自己加密方法
    private static byte[] encrypt(byte[] srcData) {
//        for (int i = 0; i < srcData.length; i++) {
//            srcData[i] = (byte) (0xFF ^ srcData[i]);
//        }
        return srcData;
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
    private static byte[] intToByte(int number) {
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
