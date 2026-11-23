-- GTFS static schedule. These tables hold one schedule at a time (the ACTIVE feed_version)
-- and are replaced in full on each import.

-- Every schedule import attempt, and which one is currently loaded.
CREATE TABLE feed_versions (
    id                 BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_url         TEXT        NOT NULL,
    zip_sha256         TEXT        NOT NULL,
    publisher_version  TEXT,
    feed_start_date    DATE,
    feed_end_date      DATE,
    status             TEXT        NOT NULL
        CHECK (status IN ('LOADING', 'ACTIVE', 'FAILED', 'SUPERSEDED')),
    import_started_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    import_finished_at TIMESTAMPTZ,
    error_message      TEXT,
    CHECK (feed_start_date <= feed_end_date)
);

-- At most one ACTIVE version
CREATE UNIQUE INDEX feed_versions_one_active
    ON feed_versions (status)
    WHERE status = 'ACTIVE';

-- Transit agencies in the feed (agency.txt).
CREATE TABLE agency (
    agency_id       TEXT PRIMARY KEY,
    agency_name     TEXT NOT NULL,
    agency_url      TEXT NOT NULL,
    agency_timezone TEXT NOT NULL,
    agency_lang     TEXT,
    agency_phone    TEXT,
    agency_fare_url TEXT,
    agency_email    TEXT
);

-- Bus and train routes, e.g. route 11 (routes.txt).
CREATE TABLE routes (
    route_id            TEXT    PRIMARY KEY,
    agency_id           TEXT    NOT NULL REFERENCES agency (agency_id),
    route_short_name    TEXT,
    route_long_name     TEXT,
    route_desc          TEXT,
    route_type          INTEGER NOT NULL
        CHECK (route_type IN (0, 1, 2, 3, 4, 5, 6, 7, 11, 12)),
    route_url           TEXT,
    route_color         TEXT,
    route_text_color    TEXT,
    route_sort_order    INTEGER,
    continuous_pickup   INTEGER CHECK (continuous_pickup IN (0, 1, 2, 3)),
    continuous_drop_off INTEGER CHECK (continuous_drop_off IN (0, 1, 2, 3)),
    network_id          TEXT,
    CHECK (
        NULLIF(BTRIM(route_short_name), '') IS NOT NULL
        OR NULLIF(BTRIM(route_long_name), '') IS NOT NULL
    )
);

-- Stops and stations with their locations (stops.txt).
CREATE TABLE stops (
    stop_id             TEXT    PRIMARY KEY,
    stop_code           TEXT,
    stop_name           TEXT,
    tts_stop_name       TEXT,
    stop_desc           TEXT,
    stop_lat            DOUBLE PRECISION CHECK (stop_lat BETWEEN -90 AND 90),
    stop_lon            DOUBLE PRECISION CHECK (stop_lon BETWEEN -180 AND 180),
    zone_id             TEXT,
    stop_url            TEXT,
    location_type       INTEGER NOT NULL DEFAULT 0
        CHECK (location_type IN (0, 1, 2, 3, 4)),
    -- Checked at COMMIT, so stop order in stops.txt doesn't matter
    parent_station      TEXT
        REFERENCES stops (stop_id) DEFERRABLE INITIALLY DEFERRED,
    stop_timezone       TEXT,
    wheelchair_boarding INTEGER CHECK (wheelchair_boarding IN (0, 1, 2)),
    level_id            TEXT,
    platform_code       TEXT
);

-- Every service_id from calendar.txt and calendar_dates.txt, so trips can reference either.
CREATE TABLE services (
    service_id TEXT PRIMARY KEY
);

-- Weekly operating pattern and date range for each service (calendar.txt).
CREATE TABLE calendar (
    service_id TEXT    PRIMARY KEY REFERENCES services (service_id),
    monday     BOOLEAN NOT NULL,
    tuesday    BOOLEAN NOT NULL,
    wednesday  BOOLEAN NOT NULL,
    thursday   BOOLEAN NOT NULL,
    friday     BOOLEAN NOT NULL,
    saturday   BOOLEAN NOT NULL,
    sunday     BOOLEAN NOT NULL,
    start_date DATE    NOT NULL,
    end_date   DATE    NOT NULL,
    CHECK (start_date <= end_date)
);

-- Dates where a service is added (1) or removed (2), e.g. holidays (calendar_dates.txt).
CREATE TABLE calendar_dates (
    service_id     TEXT    NOT NULL REFERENCES services (service_id),
    date           DATE    NOT NULL,
    exception_type INTEGER NOT NULL CHECK (exception_type IN (1, 2)),
    PRIMARY KEY (service_id, date)
);

-- Every shape_id in shapes.txt, so trips can reference a shape.
CREATE TABLE shape_definitions (
    shape_id TEXT PRIMARY KEY
);

-- Ordered points that draw a route's path on a map (shapes.txt).
CREATE TABLE shapes (
    shape_id            TEXT    NOT NULL REFERENCES shape_definitions (shape_id),
    shape_pt_sequence   INTEGER NOT NULL CHECK (shape_pt_sequence >= 0),
    shape_pt_lat        DOUBLE PRECISION NOT NULL CHECK (shape_pt_lat BETWEEN -90 AND 90),
    shape_pt_lon        DOUBLE PRECISION NOT NULL CHECK (shape_pt_lon BETWEEN -180 AND 180),
    shape_dist_traveled DOUBLE PRECISION CHECK (shape_dist_traveled >= 0),
    PRIMARY KEY (shape_id, shape_pt_sequence)
);

-- One scheduled run of a route, e.g. the 11 to Lincoln Fields at 22:48 (trips.txt).
CREATE TABLE trips (
    trip_id               TEXT    PRIMARY KEY,
    route_id              TEXT    NOT NULL REFERENCES routes (route_id),
    service_id            TEXT    NOT NULL REFERENCES services (service_id),
    trip_headsign         TEXT,
    trip_short_name       TEXT,
    direction_id          INTEGER CHECK (direction_id IN (0, 1)),
    block_id              TEXT,
    shape_id              TEXT    REFERENCES shape_definitions (shape_id),
    wheelchair_accessible INTEGER CHECK (wheelchair_accessible IN (0, 1, 2)),
    bikes_allowed         INTEGER CHECK (bikes_allowed IN (0, 1, 2))
);

-- Each stop a trip makes, in order, with scheduled times (stop_times.txt).
-- Times are seconds since "noon minus 12h" of the service day (25:30:00 -> 91800).
CREATE TABLE stop_times (
    trip_id                TEXT    NOT NULL REFERENCES trips (trip_id),
    stop_sequence          INTEGER NOT NULL CHECK (stop_sequence >= 0),
    stop_id                TEXT    NOT NULL REFERENCES stops (stop_id),
    arrival_time_seconds   INTEGER CHECK (arrival_time_seconds >= 0),
    departure_time_seconds INTEGER CHECK (departure_time_seconds >= 0),
    stop_headsign          TEXT,
    pickup_type            INTEGER CHECK (pickup_type IN (0, 1, 2, 3)),
    drop_off_type          INTEGER CHECK (drop_off_type IN (0, 1, 2, 3)),
    shape_dist_traveled    DOUBLE PRECISION CHECK (shape_dist_traveled >= 0),
    timepoint              INTEGER CHECK (timepoint IN (0, 1)),
    PRIMARY KEY (trip_id, stop_sequence)
);
