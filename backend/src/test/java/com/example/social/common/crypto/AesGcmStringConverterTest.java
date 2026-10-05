package com.example.social.common.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AesGcmStringConverterTest {
    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @BeforeAll
    static void configureKey() {
        AesGcmStringConverter.configure(KEY);
    }

    @Test
    void encryptsAndDecryptsWithAuthenticatedCiphertext() {
        AesGcmStringConverter converter = new AesGcmStringConverter();
        String encrypted = converter.convertToDatabaseColumn("private note");
        assertNotEquals("private note", encrypted);
        assertEquals("private note", converter.convertToEntityAttribute(encrypted));
    }

    @Test
    void rejectsTamperedCiphertext() {
        AesGcmStringConverter converter = new AesGcmStringConverter();
        String encrypted = converter.convertToDatabaseColumn("private note");
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "AA";
        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(tampered));
    }
}
