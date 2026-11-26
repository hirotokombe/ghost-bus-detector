package com.hirokombe.ghostbus.gtfs;

import com.hirokombe.ghostbus.gtfs.model.GtfsAgency;
import com.hirokombe.ghostbus.gtfs.model.GtfsRoute;
import com.hirokombe.ghostbus.gtfs.model.GtfsStop;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class GtfsScheduleWriter {

    private static final String INSERT_AGENCY = """
            INSERT INTO agency (agency_id, agency_name, agency_url, agency_timezone,
                    agency_lang, agency_phone, agency_fare_url, agency_email)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_ROUTE = """
            INSERT INTO routes (route_id, agency_id, route_short_name, route_long_name,
                    route_desc, route_type, route_url, route_color, route_text_color,
                    route_sort_order, continuous_pickup, continuous_drop_off, network_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_STOP = """
            INSERT INTO stops (stop_id, stop_code, stop_name, tts_stop_name, stop_desc,
                    stop_lat, stop_lon, zone_id, stop_url, location_type, parent_station,
                    stop_timezone, wheelchair_boarding, level_id, platform_code)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public GtfsScheduleWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertAgency(GtfsAgency agency) {
        jdbcTemplate.update(INSERT_AGENCY,
                agency.agencyId(),
                agency.agencyName(),
                agency.agencyUrl(),
                agency.agencyTimezone(),
                agency.agencyLang(),
                agency.agencyPhone(),
                agency.agencyFareUrl(),
                agency.agencyEmail());
    }

    public void insertRoute(GtfsRoute route) {
        jdbcTemplate.update(INSERT_ROUTE,
                route.routeId(),
                route.agencyId(),
                route.routeShortName(),
                route.routeLongName(),
                route.routeDesc(),
                route.routeType(),
                route.routeUrl(),
                route.routeColor(),
                route.routeTextColor(),
                route.routeSortOrder(),
                route.continuousPickup(),
                route.continuousDropOff(),
                route.networkId());
    }

    public void insertStop(GtfsStop stop) {
        jdbcTemplate.update(INSERT_STOP,
                stop.stopId(),
                stop.stopCode(),
                stop.stopName(),
                stop.ttsStopName(),
                stop.stopDesc(),
                stop.stopLat(),
                stop.stopLon(),
                stop.zoneId(),
                stop.stopUrl(),
                stop.locationType(),
                stop.parentStation(),
                stop.stopTimezone(),
                stop.wheelchairBoarding(),
                stop.levelId(),
                stop.platformCode());
    }

    public static void main(String[] args) throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:15432/ghostbus",
                "ghostbus",
                System.getenv().getOrDefault("DB_PASSWORD", "localdevpassword"));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        GtfsScheduleWriter writer = new GtfsScheduleWriter(jdbcTemplate);
        GtfsCsvReader reader = new GtfsCsvReader();

        // Clear earlier runs, children before parents
        jdbcTemplate.update("DELETE FROM routes");
        jdbcTemplate.update("DELETE FROM agency");
        jdbcTemplate.update("DELETE FROM stops");

        try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/agency.txt")) {
            reader.read("agency.txt", in, row -> writer.insertAgency(GtfsAgency.from(row)));
        }
        try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/routes.txt")) {
            reader.read("routes.txt", in, row -> writer.insertRoute(GtfsRoute.from(row)));
        }

        AtomicInteger inserted = new AtomicInteger();
        try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/stops.txt")) {
            reader.read("stops.txt", in, row -> {
                writer.insertStop(GtfsStop.from(row));
                inserted.incrementAndGet();
            });
            System.out.println("All " + inserted.get() + " stops inserted");
        } catch (DataAccessException e) {
            System.out.println("Insert failed after " + inserted.get() + " stops");
            System.out.println("Reason: " + e.getMostSpecificCause().getMessage());
        }

        Integer stopCount = jdbcTemplate.queryForObject("SELECT count(*) FROM stops", Integer.class);
        System.out.println("stops table now has " + stopCount + " rows (file has 5,842)");
    }
}
