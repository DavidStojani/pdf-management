package org.papercloud.de.pdfdatabase.config;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AESCryptoUtil {
    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String SECRET = "Gh6SulC9MstwvgqOaT8pboLIxmosW+4xiRzKFwOys2g=";//System.getenv("PDF_AES_SECRET"); // base64-encoded 32 bytes

    private static SecretKeySpec getKey() {
        byte[] decodedKey = Base64.getDecoder().decode(SECRET);
        return new SecretKeySpec(decodedKey, ALGO);
    }

    public static byte[] encrypt(byte[] plainBytes) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getKey());
        return cipher.doFinal(plainBytes);
    }

    public static byte[] decrypt(byte[] cipherBytes) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getKey());
        return cipher.doFinal(cipherBytes);
    }

    public static String encrypt(String plainText) throws Exception {
        return Base64.getEncoder().encodeToString(encrypt(plainText.getBytes(StandardCharsets.UTF_8)));
    }

    public static String decrypt(String cipherText) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(cipherText);
        byte[] decrypted = decrypt(encryptedBytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}

