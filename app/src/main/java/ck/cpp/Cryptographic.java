package ck.cpp;


import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

/**
 * <br> ClassName:   Cryptographic
 * <br> Description: 字节加密解密
 * <br>
 * <br> Author:      谢文良
 * <br> Date:         2017/5/10 9:24
 */
public class Cryptographic {
    private static final String ALGORITHM = "desede";
    private static final String TRANSFORMATION = "DESede/ECB/PKCS5Padding";

    public static byte[] encryptByte(byte[] context, byte[] key) {
        try {
            DESedeKeySpec spec = new DESedeKeySpec(key);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            Key desKey = secretKeyFactory.generateSecret(spec);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, desKey);
            return cipher.doFinal(context);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] decodeByte(byte[] context, byte[] key) {
        try {
            DESedeKeySpec spec = new DESedeKeySpec(key);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            Key desKey = secretKeyFactory.generateSecret(spec);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, desKey);
            return cipher.doFinal(context);
        } catch (Exception e) {
            return null;
        }
    }
}
