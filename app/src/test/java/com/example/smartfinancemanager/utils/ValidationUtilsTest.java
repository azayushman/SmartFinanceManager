package com.example.smartfinancemanager.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ValidationUtilsTest {

    @Test
    public void description_rules() {
        assertNotNull(ValidationUtils.validateDescription(null));
        assertNotNull(ValidationUtils.validateDescription("   "));
        assertNull(ValidationUtils.validateDescription("Rent"));
        String tooLong = new String(new char[61]).replace('\0', 'a');
        assertNotNull(ValidationUtils.validateDescription(tooLong));
    }

    @Test
    public void date_cannotBeInTheFutureOrMissing() {
        long now = System.currentTimeMillis();
        long today = DateUtils.startOfDay(now);
        long twoDaysAhead = today + 2L * 24 * 60 * 60 * 1000;
        assertNull(ValidationUtils.validateDate(today, now));
        assertNotNull(ValidationUtils.validateDate(twoDaysAhead, now));
        assertNotNull(ValidationUtils.validateDate(0, now));
    }
}