package com.ProyectoTGS.BackCore.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import services.RutValidator;

import static org.junit.jupiter.api.Assertions.*;

class RutValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"12.345.678-5", "12345678-5", "123456785", " 12.345.678-5 "})
    void normalize_AcceptsCommonFormats(String input) {
        assertEquals("12.345.678-5", RutValidator.normalize(input));
    }

    @Test
    void normalize_HandlesCheckDigitK_CaseInsensitive() {
        // 10.000.013 -> check digit K
        assertEquals("10.000.013-K", RutValidator.normalize("10000013-k"));
    }

    @Test
    void normalize_HandlesCheckDigitZero() {
        // 10.000.004 -> check digit 0
        assertEquals("10.000.004-0", RutValidator.normalize("10000004-0"));
    }

    @Test
    void normalize_HandlesSevenDigitBody() {
        assertEquals("1.000.005-K", RutValidator.normalize("1000005-K"));
    }

    @Test
    void normalize_RejectsWrongCheckDigit() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> RutValidator.normalize("12.345.678-9"));
        assertTrue(ex.getMessage().contains("check digit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "12.345.678-X", "123456789012"})
    void normalize_RejectsInvalidFormat(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> RutValidator.normalize(input));
        assertTrue(ex.getMessage().contains("format"));
    }

    @Test
    void normalize_RejectsNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> RutValidator.normalize(null));
        assertThrows(IllegalArgumentException.class, () -> RutValidator.normalize("  "));
    }
}
