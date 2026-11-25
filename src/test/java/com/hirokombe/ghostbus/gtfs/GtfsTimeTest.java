package com.hirokombe.ghostbus.gtfs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtfsTimeTest {

    // Each line is one test run: input | expected seconds.
    // Values in single quotes keep their spaces; unquoted values are trimmed.
    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
            00:00:00   | 0
            08:00:00   | 28800
            23:59:59   | 86399
            24:00:00   | 86400
            25:30:00   | 91800
            7:05:00    | 25500
            ' 7:05:00' | 25500
            """)
    void convertsValidTimesToSeconds(String input, Integer expected) {
        assertEquals(expected, GtfsTime.toSeconds(input));
    }

    // @NullAndEmptySource runs the test with null and "", @ValueSource adds a blank string.
    @ParameterizedTest(name = "\"{0}\" -> null")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void returnsNullWhenNoTimeIsGiven(String input) {
        assertNull(GtfsTime.toSeconds(input));
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @ValueSource(strings = {
            "25:61:00",    // minutes out of range
            "08:00:60",    // seconds out of range
            "08:5:00",     // minutes must be two digits
            "8:00",        // missing seconds
            "08:00:00:00", // too many parts
            "ab:cd:ef",    // not numbers
            "-1:00:00",    // negative hours
            "08-00-00"     // wrong separator
    })
    void rejectsInvalidTimes(String input) {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> GtfsTime.toSeconds(input));

        assertTrue(ex.getMessage().contains(input),
                "error message should include the bad value, but was: " + ex.getMessage());
    }

    // A plain @Test for one specific case worth calling out on its own.
    @Test
    void allowsHoursPastMidnightForOvernightTrips() {
        // 1:30 a.m. the next morning, still part of the previous service day
        assertEquals(91800, GtfsTime.toSeconds("25:30:00"));
    }
}