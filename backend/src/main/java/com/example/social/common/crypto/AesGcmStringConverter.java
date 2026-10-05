package com.example.social.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Converter
public class AesGcmStringConverter implements AttributeConverter<String, String> {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile byte[] encryptionKey;

    public static void configure(String encodedKey) {
        byte[] decoded = Base64.getDecoder().decode(encodedKey);
        if (decoded.length != 32) {
            throw new IllegalArgumentException("AES encryption key must be exactly 32 bytes");
        }
        encryptionKey = decoded;
    }

    @Override
    public String convertToDatabaseColumn(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(requireKey(), "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt sensitive field", exception);
        }
    }

    @Override
    public String convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (!value.startsWith("v1:")) {
            throw new IllegalStateException("Unsupported encrypted field format");
        }
        try {
            ByteBuffer payload = ByteBuffer.wrap(Base64.getDecoder().decode(value.substring(3)));
            byte[] iv = new byte[IV_LENGTH];
            payload.get(iv);
            byte[] encrypted = new byte[payload.remaining()];
            payload.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(requireKey(), "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | RuntimeException exception) {
            throw new IllegalStateException("Could not decrypt sensitive field", exception);
        }
    }

    private byte[] requireKey() {
        byte[] key = encryptionKey;
        if (key == null) {
            throw new IllegalStateException("AES encryption key has not been configured");
        }
        return key;
    }
}
