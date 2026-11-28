package com.hirokombe.ghostbus.benchmark;

import com.hirokombe.ghostbus.gtfs.GtfsCsvReader;
import com.hirokombe.ghostbus.gtfs.model.GtfsStopTime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Times JPA saveAll on stop_times, then rolls back. Runs only with the jpa-benchmark profile.
@Component
@Profile("jpa-benchmark")
public class JpaStopTimesBenchmark implements CommandLineRunner {

    private static final long FILE_ROWS = 2_901_867;
    private static final int SAMPLE_SIZE = (int) FILE_ROWS; // the whole file

    private final StopTimeRepository repository;
    private final EntityManager entityManager;
    private final Statistics statistics;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transaction;

    public JpaStopTimesBenchmark(StopTimeRepository repository, EntityManager entityManager,
                                 EntityManagerFactory entityManagerFactory, JdbcTemplate jdbcTemplate,
                                 PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.entityManager = entityManager;
        // Hibernate's counters; they only count when hibernate.generate_statistics is true
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.jdbcTemplate = jdbcTemplate;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        GtfsCsvReader reader = new GtfsCsvReader();

        transaction.executeWithoutResult(status -> {
            // Nothing references stop_times, so it can be truncated on its own
            jdbcTemplate.execute("TRUNCATE stop_times");

            statistics.clear(); // count only what the saveAll loop does
            List<StopTimeEntity> batch = new ArrayList<>();
            AtomicInteger saved = new AtomicInteger();
            long start = System.nanoTime();
            try (InputStream in = new FileInputStream("data/gtfs/GTFSExport/stop_times.txt")) {
                reader.read("stop_times.txt", in, row -> {
                    if (saved.get() + batch.size() >= SAMPLE_SIZE) {
                        return; // sample is full; skip the rest of the file
                    }
                    batch.add(StopTimeEntity.from(GtfsStopTime.from(row)));
                    if (batch.size() == 1_000) {
                        repository.saveAll(batch);
                        entityManager.flush(); // send this batch's SQL now
                        entityManager.clear(); // forget saved entities so memory stays flat
                        saved.addAndGet(batch.size());
                        batch.clear();
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (!batch.isEmpty()) {
                // The last batch is usually not full
                repository.saveAll(batch);
                entityManager.flush();
                entityManager.clear();
                saved.addAndGet(batch.size());
            }
            long millis = (System.nanoTime() - start) / 1_000_000;

            Long inTable = jdbcTemplate.queryForObject("SELECT count(*) FROM stop_times", Long.class);
            System.out.println("JPA saveAll: " + saved.get() + " rows in " + millis + " ms ("
                    + inTable + " in the table)");
            System.out.println("Hibernate statements sent: " + statistics.getPrepareStatementCount()
                    + ", entity inserts: " + statistics.getEntityInsertCount());

            status.setRollbackOnly(); // undo the test, keep the real data
        });

        Long afterRollback = jdbcTemplate.queryForObject("SELECT count(*) FROM stop_times", Long.class);
        System.out.println("After rollback: stop_times=" + afterRollback + " (expected 2,901,867)");
    }
}