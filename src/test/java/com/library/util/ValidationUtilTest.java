package com.library.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationUtilTest {

    @Test
    public void validEmail_acceptsCorrectFormat() {
        assertTrue(ValidationUtil.isValidEmail("deepak@example.com"));
    }

    @Test
    public void validEmail_rejectsMissingAtSign() {
        assertFalse(ValidationUtil.isValidEmail("deepak.example.com"));
    }

    @Test
    public void validEmail_rejectsNull() {
        assertFalse(ValidationUtil.isValidEmail(null));
    }

    @Test
    public void validPhone_acceptsTenDigits() {
        assertTrue(ValidationUtil.isValidPhone("9876543210"));
    }

    @Test
    public void validPhone_rejectsTooShort() {
        assertFalse(ValidationUtil.isValidPhone("12345"));
    }

    @Test
    public void validIsbn_accepts13DigitIsbn() {
        assertTrue(ValidationUtil.isValidIsbn("9780306406157"));
    }

    @Test
    public void validIsbn_rejectsMalformed() {
        assertFalse(ValidationUtil.isValidIsbn("abc123"));
    }

    @Test
    public void isNonBlank_rejectsWhitespaceOnly() {
        assertFalse(ValidationUtil.isNonBlank("   "));
    }

    @Test
    public void isPositive_rejectsZeroAndNegative() {
        assertFalse(ValidationUtil.isPositive(0));
        assertFalse(ValidationUtil.isPositive(-5));
        assertTrue(ValidationUtil.isPositive(1));
    }
}
