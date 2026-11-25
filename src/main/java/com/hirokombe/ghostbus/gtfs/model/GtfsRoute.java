package com.hirokombe.ghostbus.gtfs.model;

import com.hirokombe.ghostbus.gtfs.GtfsRow;

public record GtfsRoute(String routeId,
                        String agencyId,
                        String routeShortName,
                        String routeLongName,
                        String routeDesc,
                        int routeType,
                        String routeUrl,
                        String routeColor,
                        String routeTextColor,
                        Integer routeSortOrder,
                        Integer continuousPickup,
                        Integer continuousDropOff,
                        String networkId) {

    public static GtfsRoute from(GtfsRow row) {
        return new GtfsRoute(row.required("route_id"),
                row.required("agency_id"),
                row.optional("route_short_name"),
                row.optional("route_long_name"),
                row.optional("route_desc"),
                row.requiredInt("route_type"),
                row.optional("route_url"),
                row.optional("route_color"),
                row.optional("route_text_color"),
                row.optionalInt("route_sort_order"),
                row.optionalInt("continuous_pickup"),
                row.optionalInt("continuous_drop_off"),
                row.optional("network_id"));
    }
}
