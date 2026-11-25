package com.hirokombe.ghostbus.gtfs.model;

import com.hirokombe.ghostbus.gtfs.GtfsRow;

public record GtfsTrip(String tripId,
                       String routeId,
                       String serviceId,
                       String tripHeadsign,
                       String tripShortName,
                       Integer directionId,
                       String blockId,
                       String shapeId,
                       Integer wheelchairAccessible,
                       Integer bikesAllowed) {

    public static GtfsTrip from(GtfsRow row) {
        return new GtfsTrip(
                row.required("trip_id"),
                row.required("route_id"),
                row.required("service_id"),
                row.optional("trip_headsign"),
                row.optional("trip_short_name"),
                row.optionalInt("direction_id"),
                row.optional("block_id"),
                row.optional("shape_id"),
                row.optionalInt("wheelchair_accessible"),
                row.optionalInt("bikes_allowed")
        );
    }
}
