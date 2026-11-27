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

    private static final String INSERT_SERVICE = "INSERT INTO services (service_id) VALUES (?)";

    private static final String INSERT_CALENDAR = """
            INSERT INTO calendar (service_id, monday, tuesday, wednesday, thursday, friday,
                    saturday, sunday, start_date, end_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_CALENDAR_DATE =
            "INSERT INTO calendar_dates (service_id, date, exception_type) VALUES (?, ?, ?)";

    private static final String INSERT_SHAPE_DEFINITION = "INSERT INTO shape_definitions (shape_id) VALUES (?)";

    private static final String INSERT_SHAPE = """
           INSERT INTO shapes (shape_id, shape_pt_lat, shape_pt_lon, shape_pt_sequence,
                   shape_dist_traveled)
           VALUES (?, ?, ?, ?, ?)
           """;

    private static final String INSERT_TRIP = """
            INSERT INTO trips (trip_id, route_id, service_id, trip_headsign, trip_short_name,
                direction_id, block_id, shape_id, wheelchair_accessible, bikes_allowed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_STOP_TIME = """
            INSERT INTO stop_times (trip_id, stop_sequence, stop_id, arrival_time_seconds, departure_time_seconds,
                stop_headsign, pickup_type, drop_off_type, shape_dist_traveled, timepoint)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

    public void insertCalendar(GtfsCalendar calendar) {
        jdbcTemplate.update(INSERT_CALENDAR,
                calendar.serviceId(),
                calendar.monday(),
                calendar.tuesday(),
                calendar.wednesday(),
                calendar.thursday(),
                calendar.friday(),
                calendar.saturday(),
                calendar.sunday(),
                calendar.startDate(),
                calendar.endDate());
    }

    public void insertCalendarDate(GtfsCalendarDate calendarDate) {
        jdbcTemplate.update(INSERT_CALENDAR_DATE,
                calendarDate.serviceId(),
                calendarDate.date(),
                calendarDate.exceptionType());
    }

    public void insertShapeDefinition(String shapeId) {
        jdbcTemplate.update(INSERT_SHAPE_DEFINITION, shapeId);
    }

    public void insertShapes(List<GtfsShape> shapes) {
        List<Object[]> batch = new ArrayList<>();
        for (GtfsShape shape : shapes) {
            batch.add(new Object[] {
                    shape.shapeId(),
                    shape.shapePtLat(),
                    shape.shapePtLon(),
                    shape.shapePtSequence(),
                    shape.shapeDistTraveled()
            });
        }
        jdbcTemplate.batchUpdate(INSERT_SHAPE, batch);
    }

    public void insertTrips(List<GtfsTrip> trips) {
        List<Object[]> batch  = new ArrayList<>();
        for (GtfsTrip trip : trips) {
            batch.add(new Object[] {
                    trip.tripId(),
                    trip.routeId(),
                    trip.serviceId(),
                    trip.tripHeadsign(),
                    trip.tripShortName(),
                    trip.directionId(),
                    trip.blockId(),
                    trip.shapeId(),
                    trip.wheelchairAccessible(),
                    trip.bikesAllowed()
            });
        }
        jdbcTemplate.batchUpdate(INSERT_TRIP, batch);
    }

    public void insertStopTimes(List<GtfsStopTime> stopTimes) {
        List<Object[]> batch = new ArrayList<>();
        for (GtfsStopTime stopTime : stopTimes) {
            batch.add(new Object[] {
                    stopTime.tripId(),
                    stopTime.stopSequence(),
                    stopTime.stopId(),
                    stopTime.arrivalTimeSeconds(),
                    stopTime.departureTimeSeconds(),
                    stopTime.stopHeadsign(),
                    stopTime.pickupType(),
                    stopTime.dropOffType(),
                    stopTime.shapeDistTraveled(),
                    stopTime.timepoint()
            });
        }
        jdbcTemplate.batchUpdate(INSERT_STOP_TIME, batch);
    }

    public static void main(String[] args) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:15432/ghostbus?reWriteBatchedInserts=true",
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
                // Empty every schedule table at once; tables that reference each other must be in the same statement
                long truncateStart = System.nanoTime();
                jdbcTemplate.execute("""
                    TRUNCATE stop_times, trips, routes, agency, stops,
                             calendar_dates, calendar, services, shapes, shape_definitions
                    """);
                System.out.println("Truncate: " + (System.nanoTime() - truncateStart) / 1_000_000 + " ms");

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

                    // Parents first: calendar and calendar_dates reference services
                    writer.insertServices(calendars, calendarDates);
                    for (GtfsCalendar calendar : calendars) {
                        writer.insertCalendar(calendar);
                    }
                    for (GtfsCalendarDate calendarDate : calendarDates) {
                        writer.insertCalendarDate(calendarDate);
                    }

                    // shapes.txt is too big to keep in memory: first pass collects only the IDs
                    Set<String> shapeIds = new LinkedHashSet<>();
                    try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/shapes.txt")) {
                        reader.read("shapes.txt", in, row -> shapeIds.add(row.required("shape_id")));
                    }
                    for (String shapeId : shapeIds) {
                        writer.insertShapeDefinition(shapeId);
                    }

                    // Second pass: insert the points, 1,000 per batch
                    List<GtfsShape> shapeBatch = new ArrayList<>();
                    long shapesStart = System.nanoTime();
                    try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/shapes.txt")) {
                        reader.read("shapes.txt", in, row -> {
                            shapeBatch.add(GtfsShape.from(row));
                            if (shapeBatch.size() == 1_000) {
                                writer.insertShapes(shapeBatch);
                                shapeBatch.clear();
                            }
                        });
                    }
                    if (!shapeBatch.isEmpty()) {
                        // The last batch is usually not full
                        writer.insertShapes(shapeBatch);
                    }
                    System.out.println("Shapes: " + (System.nanoTime() - shapesStart) / 1_000_000 + " ms");

                    // trips reference routes, services and shape_definitions, all loaded above
                    List<GtfsTrip> tripBatch = new ArrayList<>();
                    long tripsStart = System.nanoTime();
                    try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/trips.txt")) {
                        reader.read("trips.txt", in, row -> {
                            tripBatch.add(GtfsTrip.from(row));
                            if (tripBatch.size() == 1_000) {
                                writer.insertTrips(tripBatch);
                                tripBatch.clear();
                            }
                        });
                    }
                    if (!tripBatch.isEmpty()) {
                        writer.insertTrips(tripBatch);
                    }
                    System.out.println("Trips: " + (System.nanoTime() - tripsStart) / 1_000_000 + " ms");

                    // stop_times reference trips and stops, both loaded above
                    List<GtfsStopTime> stopTimeBatch = new ArrayList<>();
                    AtomicInteger stopTimes = new AtomicInteger();
                    long stopTimesStart = System.nanoTime();
                    try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/stop_times.txt")) {
                        reader.read("stop_times.txt", in, row -> {
                            stopTimeBatch.add(GtfsStopTime.from(row));
                            if (stopTimeBatch.size() == 1_000) {
                                writer.insertStopTimes(stopTimeBatch);
                                stopTimes.addAndGet(stopTimeBatch.size());
                                stopTimeBatch.clear();
                            }
                        });
                    }
                    if (!stopTimeBatch.isEmpty()) {
                        writer.insertStopTimes(stopTimeBatch);
                        stopTimes.addAndGet(stopTimeBatch.size());
                    }
                    System.out.println("Stop times (batchUpdate): sent " + stopTimes.get() + " in "
                            + (System.nanoTime() - stopTimesStart) / 1_000_000 + " ms");
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
        Integer calendarCount = jdbcTemplate.queryForObject("SELECT count(*) FROM calendar", Integer.class);
        Integer calendarDateCount = jdbcTemplate.queryForObject("SELECT count(*) FROM calendar_dates", Integer.class);
        Integer shapeDefinitionCount = jdbcTemplate.queryForObject("SELECT count(*) FROM shape_definitions", Integer.class);
        Integer shapeCount = jdbcTemplate.queryForObject("SELECT count(*) FROM shapes", Integer.class);
        Integer tripCount = jdbcTemplate.queryForObject("SELECT count(*) FROM trips", Integer.class);
        Integer stopTimeCount = jdbcTemplate.queryForObject("SELECT count(*) FROM stop_times", Integer.class);
        System.out.println("Took " + millis + " ms in total");
        System.out.println("agency=" + agencyCount + ", routes=" + routeCount + ", stops=" + stopCount
                + ", services=" + serviceCount + ", calendar=" + calendarCount + ", calendar_dates=" + calendarDateCount
                + ", shape_definitions=" + shapeDefinitionCount + ", shapes=" + shapeCount + ", trips=" + tripCount
                + ", stop_times=" + stopTimeCount
                + " (expected 1, 178, 5,842, 22, 18, 52, 704, 874,720, 73,559, 2,901,867)");
    }
}
