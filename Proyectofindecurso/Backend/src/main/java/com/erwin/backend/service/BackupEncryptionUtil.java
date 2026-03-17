package com.erwin.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class BackupEncryptionUtil {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LEN = 12;
    private static final int    GCM_TAG    = 128;

    @Value("${backup.encryption.key}")
    private String base64Key;

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined  = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error cifrando credencial", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) return cipherText;
        try {
            byte[] combined  = Base64.getDecoder().decode(cipherText);
            byte[] iv        = new byte[GCM_IV_LEN];
            byte[] encrypted = new byte[combined.length - GCM_IV_LEN];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LEN);
            System.arraycopy(combined, GCM_IV_LEN, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error descifrando credencial", e);
        }
    }

    private SecretKey buildKey() {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(keyBytes, "AES");
    }
}