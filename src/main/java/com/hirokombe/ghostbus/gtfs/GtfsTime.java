package com.hirokombe.ghostbus.gtfs;

import java.util.regex.Pattern;

public final class GtfsTime {
    private static final Pattern DURATION_PATTERN = Pattern.compile("\\d{1,2}:[0-5]\\d:[0-5]\\d");

    private GtfsTime() {}

    public static Integer toSeconds(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }

        String trimmed = time.trim();
        if (!DURATION_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid GTFS time: \"" + time + "\"");
        }

        String[] parts = trimmed.split(":");
        return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
    }
}
