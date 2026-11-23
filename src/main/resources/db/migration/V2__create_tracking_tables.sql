-- Realtime tracking. Raw feed tables have no foreign keys or CHECKs on feed values,
-- so a poll is never rejected. trip_statuses holds the app's own conclusions.

-- Every attempt to fetch a realtime feed, including failures. Used to detect a stale or down feed.
CREATE TABLE feed_polls (
    id             BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    feed_type      TEXT        NOT NULL
        CHECK (feed_type IN ('VEHICLE_POSITIONS', 'TRIP_UPDATES')),
    polled_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    success        BOOLEAN     NOT NULL,
    http_status    INTEGER,
    feed_timestamp TIMESTAMPTZ,
    entity_count   INTEGER CHECK (entity_count >= 0),
    error_message  TEXT
);

-- One row per vehicle per poll: where each bus was and which trip it reported.
CREATE TABLE vehicle_observations (
    id                    BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    received_at           TIMESTAMPTZ NOT NULL,
    vehicle_timestamp     TIMESTAMPTZ,
    vehicle_id            TEXT,
    trip_id               TEXT,
    route_id              TEXT,
    start_date            DATE,
    start_time_seconds    INTEGER,
    schedule_relationship TEXT,
    latitude              DOUBLE PRECISION,
    longitude             DOUBLE PRECISION,
    bearing               REAL,
    speed_mps             REAL,
    current_stop_sequence INTEGER,
    stop_id               TEXT
);

-- One row per trip per poll from the trip updates feed. Cancelled trips appear here.
CREATE TABLE trip_update_observations (
    id                    BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    received_at           TIMESTAMPTZ NOT NULL,
    update_timestamp      TIMESTAMPTZ,
    trip_id               TEXT,
    route_id              TEXT,
    start_date            DATE,
    start_time_seconds    INTEGER,
    schedule_relationship TEXT,
    vehicle_id            TEXT
);

-- Current status of every planned trip on each service day (tracked, ghost, cancelled, ...).
-- Route and schedule times are copied in, so rows survive a schedule re-import.
CREATE TABLE trip_statuses (
    service_date       DATE        NOT NULL,
    trip_id            TEXT        NOT NULL,
    feed_version_id    BIGINT      NOT NULL REFERENCES feed_versions (id),
    route_id           TEXT        NOT NULL,
    route_short_name   TEXT,
    trip_headsign      TEXT,
    direction_id       INTEGER,
    scheduled_start    TIMESTAMPTZ NOT NULL,
    scheduled_end      TIMESTAMPTZ NOT NULL,
    last_stop_sequence INTEGER,
    status             TEXT        NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED', 'DUE', 'TRACKED', 'CANCELLED',
                          'MISSING', 'GHOST', 'VANISHED', 'COMPLETED')),
    status_changed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    first_seen_at      TIMESTAMPTZ,
    last_seen_at       TIMESTAMPTZ,
    last_vehicle_id    TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (service_date, trip_id),
    CHECK (scheduled_start <= scheduled_end),
    CHECK (first_seen_at <= last_seen_at)
);
