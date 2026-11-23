-- Find trips for the service_ids active on a given date
CREATE INDEX idx_trips_service_id
    ON trips (service_id);

-- Feed health: latest polls per feed
CREATE INDEX idx_feed_polls_type_time
    ON feed_polls (feed_type, polled_at);

-- State engine, live-vehicles API, and retention cleanup: recent observations
CREATE INDEX idx_vehicle_observations_received_at
    ON vehicle_observations (received_at);

CREATE INDEX idx_trip_update_observations_received_at
    ON trip_update_observations (received_at);
