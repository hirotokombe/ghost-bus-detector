package com.hirokombe.ghostbus.gtfs.model;

import com.hirokombe.ghostbus.gtfs.GtfsRow;

public record GtfsShape(String shapeId,
                        int shapePtSequence,
                        double shapePtLat,
                        double shapePtLon,
                        Double shapeDistTraveled) {

    public static GtfsShape from(GtfsRow row) {
        return new GtfsShape(
                row.required("shape_id"),
                row.requiredInt("shape_pt_sequence"),
                row.requiredDouble("shape_pt_lat"),
                row.requiredDouble("shape_pt_lon"),
                row.optionalDouble("shape_dist_traveled")
        );
    }
}
