package com.hirokombe.ghostbus.gtfs.model;

import com.hirokombe.ghostbus.gtfs.GtfsRow;

public record GtfsStop(String stopId,
                       String stopCode,
                       String stopName,
                       String ttsStopName,
                       String stopDesc,
                       Double stopLat,
                       Double stopLon,
                       String zoneId,
                       String stopUrl,
                       int locationType,
                       String parentStation,
                       String stopTimezone,
                       Integer wheelchairBoarding,
                       String levelId,
                       String platformCode) {

    public static GtfsStop from(GtfsRow row) {
        Integer locationType = row.optionalInt("location_type");
        return new GtfsStop(
                row.required("stop_id"),
                row.optional("stop_code"),
                row.optional("stop_name"),
                row.optional("tts_stop_name"),
                row.optional("stop_desc"),
                row.optionalDouble("stop_lat"),
                row.optionalDouble("stop_lon"),
                row.optional("zone_id"),
                row.optional("stop_url"),
                locationType == null ? 0 : locationType,
                row.optional("parent_station"),
                row.optional("stop_timezone"),
                row.optionalInt("wheelchair_boarding"),
                row.optional("level_id"),
                row.optional("platform_code")
        );
    }
}
