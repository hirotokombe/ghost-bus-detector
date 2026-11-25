package com.hirokombe.ghostbus.gtfs;

import org.apache.commons.csv.CSVRecord;

public class GtfsRow {
    private final String fileName;
    private final CSVRecord record;

    public GtfsRow(String fileName, CSVRecord record) {
        this.fileName = fileName;
        this.record = record;
    }

    public String optional(String column) {
        if (!record.isSet(column)) {
            return null;
        }
        String value = record.get(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public String required(String column) {
        String value = optional(column);
        if (value == null) {
            throw new IllegalArgumentException(where(column) + " is required");
        }
        return value;
    }

    public Integer optionalInt(String column) {
        String value = optional(column);
        if (value == null) {
            return null;
        }
        return parseInt(column, value);
    }

    public int requiredInt(String column) {
        return parseInt(column, required(column));
    }

    private int parseInt(String column, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(where(column) + " must be an integer: \"" + value + "\"", e);
        }
    }

    private String where(String column) {
        return fileName + " line " + (record.getRecordNumber() + 1) + ": " + column;
    }
}
