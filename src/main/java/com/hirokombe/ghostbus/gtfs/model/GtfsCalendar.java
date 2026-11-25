package com.hirokombe.ghostbus.gtfs.model;

import com.hirokombe.ghostbus.gtfs.GtfsRow;

import java.time.LocalDate;

public record GtfsCalendar(String serviceId,
                           boolean monday,
                           boolean tuesday,
                           boolean wednesday,
                           boolean thursday,
                           boolean friday,
                           boolean saturday,
                           boolean sunday,
                           LocalDate startDate,
                           LocalDate endDate) {

    public static GtfsCalendar from(GtfsRow row) {
        return new GtfsCalendar(
                row.required("service_id"),
                row.requiredBoolean("monday"),
                row.requiredBoolean("tuesday"),
                row.requiredBoolean("wednesday"),
                row.requiredBoolean("thursday"),
                row.requiredBoolean("friday"),
                row.requiredBoolean("saturday"),
                row.requiredBoolean("sunday"),
                row.requiredDate("start_date"),
                row.requiredDate("end_date")
        );
    }
}
