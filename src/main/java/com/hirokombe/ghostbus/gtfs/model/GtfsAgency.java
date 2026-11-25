package com.hirokombe.ghostbus.gtfs.model;

import com.hirokombe.ghostbus.gtfs.GtfsRow;

public record GtfsAgency(String agencyId,
                         String agencyName,
                         String agencyUrl,
                         String agencyTimezone,
                         String agencyLang,
                         String agencyPhone,
                         String agencyFareUrl,
                         String agencyEmail) {

    public static GtfsAgency from(GtfsRow row) {
        return new GtfsAgency(
                    row.required("agency_id"),
                    row.required("agency_name"),
                    row.required("agency_url"),
                    row.required("agency_timezone"),
                    row.optional("agency_lang"),
                    row.optional("agency_phone"),
                    row.optional("agency_fare_url"),
                    row.optional("agency_email"));
    }
}
