CREATE TABLE sensor_logs
(
    id            INTEGER GENERATED ALWAYS AS IDENTITY,
    device_name   VARCHAR(255)     NOT NULL,
    reading_value DOUBLE PRECISION NOT NULL,
    recorded_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);