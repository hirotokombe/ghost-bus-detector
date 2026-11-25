package com.hirokombe.ghostbus.gtfs.model;

import com.hirokombe.ghostbus.gtfs.GtfsRow;

import java.time.LocalDate;

public record GtfsCalendarDate(String serviceId,
                               LocalDate date,
                               int exceptionType) {

    public static GtfsCalendarDate from(GtfsRow row) {
        return new GtfsCalendarDate(
                row.required("service_id"),
                row.requiredDate("date"),
                row.requiredInt("exception_type")
        );
    }
}
