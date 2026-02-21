package org.papercloud.de.pdfdatabase.config;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

@Component
public class AESCryptoUtil {
    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static SecretKeySpec getKey() {
        String secret = Objects.requireNonNull(
                System.getenv("PDF_AES_SECRET"),
                "PDF_AES_SECRET environment variable must be set"
        );
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        return new SecretKeySpec(decodedKey, ALGO);
    }

    public static byte[] encrypt(byte[] plainBytes) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] cipherBytes = cipher.doFinal(plainBytes);

        byte[] output = new byte[GCM_IV_LENGTH + cipherBytes.length];
        System.arraycopy(iv, 0, output, 0, GCM_IV_LENGTH);
        System.arraycopy(cipherBytes, 0, output, GCM_IV_LENGTH, cipherBytes.length);
        return output;
    }

    public static byte[] decrypt(byte[] ivAndCipherBytes) throws Exception {
        byte[] iv = Arrays.copyOfRange(ivAndCipherBytes, 0, GCM_IV_LENGTH);
        byte[] cipherBytes = Arrays.copyOfRange(ivAndCipherBytes, GCM_IV_LENGTH, ivAndCipherBytes.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(cipherBytes);
    }

    public static String encrypt(String plainText) throws Exception {
        return Base64.getEncoder().encodeToString(encrypt(plainText.getBytes(StandardCharsets.UTF_8)));
    }

    public static String decrypt(String cipherText) throws Exception {
        byte[] ivAndCipherBytes = Base64.getDecoder().decode(cipherText);
        byte[] decrypted = decrypt(ivAndCipherBytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
