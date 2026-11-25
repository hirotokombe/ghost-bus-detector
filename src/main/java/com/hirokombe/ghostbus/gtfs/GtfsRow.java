package com.hirokombe.ghostbus.gtfs;

import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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

    public Double optionalDouble(String column) {
        String value = optional(column);
        if (value == null) {
            return null;
        }
        return parseDouble(column, value);
    }

    public double requiredDouble(String column) {
        return parseDouble(column, required(column));
    }

    public LocalDate requiredDate(String column) {
        return parseDate(column, required(column));
    }

    public boolean requiredBoolean(String column) {
        return parseBoolean(column, required(column));
    }

    private int parseInt(String column, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(where(column) + " must be an integer: \"" + value + "\"", e);
        }
    }

    private double parseDouble(String column, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(where(column) + " must be a number: \"" + value + "\"", e);
        }
    }

    private LocalDate parseDate(String column, String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(where(column) + " must be a date (YYYYMMDD): \"" + value + "\"", e);
        }
    }

    private boolean parseBoolean(String column, String value) {
        return switch (value) {
            case "1" -> true;
            case "0" -> false;
            default -> throw new IllegalArgumentException(
                    where(column) + " must be 0 or 1: \"" + value + "\"");
        };
    }

    private String where(String column) {
        return fileName + " line " + (record.getRecordNumber() + 1) + ": " + column;
    }
}
