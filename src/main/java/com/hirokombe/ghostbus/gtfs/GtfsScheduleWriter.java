package com.hirokombe.ghostbus.gtfs;

import com.hirokombe.ghostbus.gtfs.model.GtfsAgency;
import com.hirokombe.ghostbus.gtfs.model.GtfsRoute;
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

        // Parents first: routes reference agency
        try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/agency.txt")) {
            reader.read("agency.txt", in, row -> writer.insertAgency(GtfsAgency.from(row)));
        }

        AtomicInteger inserted = new AtomicInteger();
        long start = System.nanoTime();
        try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/routes.txt")) {
            reader.read("routes.txt", in, row -> {
                writer.insertRoute(GtfsRoute.from(row));
                inserted.incrementAndGet();
            });
        }
        long millis = (System.nanoTime() - start) / 1_000_000;

        Integer agencyCount = jdbcTemplate.queryForObject("SELECT count(*) FROM agency", Integer.class);
        Integer routeCount = jdbcTemplate.queryForObject("SELECT count(*) FROM routes", Integer.class);
        System.out.println("Agency rows: " + agencyCount);
        System.out.println("Inserted " + inserted.get() + " routes in " + millis + " ms; routes table now has " + routeCount);

        System.out.println("Routes per route_type:");
        jdbcTemplate.queryForList("SELECT route_type, count(*) FROM routes GROUP BY route_type ORDER BY route_type")
                .forEach(System.out::println);

        System.out.println("Sample routes:");
        jdbcTemplate.queryForList("SELECT * FROM routes WHERE route_id IN ('1-373', '11', 'E1')")
                .forEach(System.out::println);
    }
}
