package ck.cpp;

/**
 * <br> ClassName:   ${className}
 * <br> Description:
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2018/5/3 10:20
 */
public class Cut {
    /**
     * <br> Description: 将int转为低字节在前，高字节在后的byte数组
     * <br> Author:      xwl
     * <br> Date:        2018/5/3 10:20
     */
    public static byte[] intToByteArray(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }

    /**
     * <br> Description: 将低字节在前转为int，高字节在后的byte数组(与IntToByteArray1想对应)
     * <br> Author:      xwl
     * <br> Date:        2018/5/3 10:20
     */
    public static int byteArrayToInt(byte[] bArr) {
        if (bArr.length != 4) {
            return -1;
        }
        return (int) ((((bArr[3] & 0xff) << 24)
                | ((bArr[2] & 0xff) << 16)
                | ((bArr[1] & 0xff) << 8)
                | ((bArr[0] & 0xff) << 0)));
    }
}
