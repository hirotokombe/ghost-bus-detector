package com.hirokombe.ghostbus.gtfs;

import com.hirokombe.ghostbus.gtfs.model.GtfsAgency;
import com.hirokombe.ghostbus.gtfs.model.GtfsRoute;
import com.hirokombe.ghostbus.gtfs.model.GtfsStop;
import com.hirokombe.ghostbus.gtfs.model.GtfsTrip;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class GtfsCsvReader {

    private static final CSVFormat DEFAULT_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true)
            .get();

    public GtfsCsvReader() {}

    public void read(String fileName, InputStream in, Consumer<GtfsRow> rowHandler) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            bufferedReader.mark(1);
            int firstChar = bufferedReader.read();
            if (firstChar != '\uFEFF' && firstChar != -1) {
                bufferedReader.reset();
            }

            try (CSVParser parser = DEFAULT_FORMAT.parse(bufferedReader)) {
                for (CSVRecord record : parser) {
                    rowHandler.accept(new GtfsRow(fileName, record));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading " + fileName, e);
        }
    }

    public static void main(String[] args) throws Exception {
        GtfsCsvReader reader = new GtfsCsvReader();
        AtomicInteger count = new AtomicInteger();

        try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/trips.txt")) {
            reader.read("trips.txt", in, row -> {
                GtfsTrip trip = GtfsTrip.from(row);
                count.incrementAndGet();
                if (trip.tripId().equals("10000010")) {
                    System.out.println(trip);
                }
            });
        }

        System.out.println("Total trips: " + count.get());
    }
}
