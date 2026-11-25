package com.hirokombe.ghostbus.gtfs.model;

import com.hirokombe.ghostbus.gtfs.GtfsRow;

public record GtfsStopTime(String tripId,
                           int stopSequence,
                           String stopId,
                           Integer arrivalTimeSeconds,
                           Integer departureTimeSeconds,
                           String stopHeadsign,
                           Integer pickupType,
                           Integer dropOffType,
                           Double shapeDistTraveled,
                           Integer timepoint) {

    public static GtfsStopTime from(GtfsRow row) {
        return new GtfsStopTime(
                row.required("trip_id"),
                row.requiredInt("stop_sequence"),
                row.required("stop_id"),
                row.optionalTime("arrival_time"),
                row.optionalTime("departure_time"),
                row.optional("stop_headsign"),
                row.optionalInt("pickup_type"),
                row.optionalInt("drop_off_type"),
                row.optionalDouble("shape_dist_traveled"),
                row.optionalInt("timepoint")
        );
    }
}
