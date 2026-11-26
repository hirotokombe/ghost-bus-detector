package com.hirokombe.ghostbus.gtfs;

import com.hirokombe.ghostbus.gtfs.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    private static final String INSERT_SERVICE = "INSERT INTO services (service_id) VALUES (?)";

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

    public int insertServices(List<GtfsCalendar> calendars, List<GtfsCalendarDate> calendarDates) {
        Set<String> serviceIds = new LinkedHashSet<>();

        for (GtfsCalendar calendar : calendars) {
            serviceIds.add(calendar.serviceId());
        }
        for (GtfsCalendarDate calendarDate : calendarDates) {
            serviceIds.add(calendarDate.serviceId());
        }
        for (String serviceId : serviceIds) {
            jdbcTemplate.update(INSERT_SERVICE, serviceId);
        }
        return serviceIds.size();
    }

    public static void main(String[] args) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:15432/ghostbus",
                "ghostbus",
                System.getenv().getOrDefault("DB_PASSWORD", "localdevpassword"));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        GtfsScheduleWriter writer = new GtfsScheduleWriter(jdbcTemplate);
        GtfsCsvReader reader = new GtfsCsvReader();

        long start = System.nanoTime();
        try {
            // One connection, one commit at the end; any exception undoes everything
            transaction.executeWithoutResult(status -> {
                jdbcTemplate.update("DELETE FROM routes");
                jdbcTemplate.update("DELETE FROM agency");
                jdbcTemplate.update("DELETE FROM stops");
                jdbcTemplate.update("DELETE FROM services");

                try {
                    try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/agency.txt")) {
                        reader.read("agency.txt", in, row -> writer.insertAgency(GtfsAgency.from(row)));
                    }
                    try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/routes.txt")) {
                        reader.read("routes.txt", in, row -> writer.insertRoute(GtfsRoute.from(row)));
                    }
                    try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/stops.txt")) {
                        reader.read("stops.txt", in, row -> writer.insertStop(GtfsStop.from(row)));
                    }

                    // Small files: keep their rows in memory to build services
                    List<GtfsCalendar> calendars = new ArrayList<>();
                    try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/calendar.txt")) {
                        reader.read("calendar.txt", in, row -> calendars.add(GtfsCalendar.from(row)));
                    }
                    List<GtfsCalendarDate> calendarDates = new ArrayList<>();
                    try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/calendar_dates.txt")) {
                        reader.read("calendar_dates.txt", in, row -> calendarDates.add(GtfsCalendarDate.from(row)));
                    }

                    int services = writer.insertServices(calendars, calendarDates);
                    System.out.println("Read " + calendars.size() + " calendar rows and " + calendarDates.size()
                            + " calendar_dates rows; inserted " + services + " services");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            System.out.println("Committed");
        } catch (RuntimeException e) {
            System.out.println("Rolled back: " + e.getMessage());
        }
        long millis = (System.nanoTime() - start) / 1_000_000;

        Integer agencyCount = jdbcTemplate.queryForObject("SELECT count(*) FROM agency", Integer.class);
        Integer routeCount = jdbcTemplate.queryForObject("SELECT count(*) FROM routes", Integer.class);
        Integer stopCount = jdbcTemplate.queryForObject("SELECT count(*) FROM stops", Integer.class);
        Integer serviceCount = jdbcTemplate.queryForObject("SELECT count(*) FROM services", Integer.class);
        System.out.println("Took " + millis + " ms");
        System.out.println("agency=" + agencyCount + ", routes=" + routeCount + ", stops=" + stopCount
                + ", services=" + serviceCount + " (expected 1, 178, 5,842, 22)");
        System.out.println("Service IDs: "
                + jdbcTemplate.queryForList("SELECT service_id FROM services ORDER BY service_id", String.class));
    }
}
