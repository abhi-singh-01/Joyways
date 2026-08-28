-- Phase 8: Transport, Driver & Trip Operations

CREATE TABLE vehicles (
    id                   BIGSERIAL PRIMARY KEY,
    registration_number  VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type         VARCHAR(100) NOT NULL,
    brand_model          VARCHAR(150),
    seating_capacity     INTEGER NOT NULL,
    ownership_type       VARCHAR(20) NOT NULL DEFAULT 'OWNED',
    status               VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_vehicle_capacity CHECK (seating_capacity > 0),
    CONSTRAINT chk_vehicle_status CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'ON_TRIP', 'MAINTENANCE', 'INACTIVE')),
    CONSTRAINT chk_vehicle_ownership CHECK (ownership_type IN ('OWNED', 'RENTED', 'PARTNER'))
);

CREATE INDEX idx_vehicles_status ON vehicles(status);
CREATE INDEX idx_vehicles_registration ON vehicles(registration_number);
CREATE INDEX idx_vehicles_type ON vehicles(vehicle_type);

CREATE TABLE drivers (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(150) NOT NULL,
    phone                VARCHAR(20) NOT NULL,
    email                VARCHAR(255),
    license_number       VARCHAR(50) NOT NULL UNIQUE,
    license_expiry_date  DATE NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_driver_status CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'ON_TRIP', 'INACTIVE'))
);

CREATE INDEX idx_drivers_status ON drivers(status);
CREATE INDEX idx_drivers_phone ON drivers(phone);
CREATE INDEX idx_drivers_license ON drivers(license_number);
CREATE INDEX idx_drivers_license_expiry ON drivers(license_expiry_date);

CREATE TABLE trips (
    id                   BIGSERIAL PRIMARY KEY,
    trip_reference       VARCHAR(30) NOT NULL UNIQUE,
    package_id           BIGINT NOT NULL REFERENCES packages(id),
    travel_date          DATE NOT NULL,
    start_at             TIMESTAMPTZ NOT NULL,
    end_at               TIMESTAMPTZ NOT NULL,
    pickup_location      TEXT,
    drop_location        TEXT,
    destination          VARCHAR(150) NOT NULL,
    driver_id            BIGINT REFERENCES drivers(id),
    status               VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_trip_status CHECK (status IN ('PLANNED', 'ASSIGNED', 'READY', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_trip_times CHECK (end_at > start_at)
);

CREATE INDEX idx_trips_reference ON trips(trip_reference);
CREATE INDEX idx_trips_travel_date ON trips(travel_date);
CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_trips_package ON trips(package_id);
CREATE INDEX idx_trips_driver ON trips(driver_id);
CREATE INDEX idx_trips_start_end ON trips(start_at, end_at);

CREATE TABLE trip_bookings (
    id                   BIGSERIAL PRIMARY KEY,
    trip_id              BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    booking_id           BIGINT NOT NULL REFERENCES bookings(id),
    assigned_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_trip_bookings_booking UNIQUE (booking_id)
);

CREATE INDEX idx_trip_bookings_trip ON trip_bookings(trip_id);

CREATE TABLE trip_vehicles (
    id                   BIGSERIAL PRIMARY KEY,
    trip_id              BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    vehicle_id           BIGINT NOT NULL REFERENCES vehicles(id),
    is_primary           BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_trip_vehicles_trip_vehicle UNIQUE (trip_id, vehicle_id)
);

CREATE INDEX idx_trip_vehicles_trip ON trip_vehicles(trip_id);
CREATE INDEX idx_trip_vehicles_vehicle ON trip_vehicles(vehicle_id);

CREATE TABLE audit_logs (
    id                   BIGSERIAL PRIMARY KEY,
    actor_user_id        BIGINT REFERENCES users(id),
    action               VARCHAR(100) NOT NULL,
    entity_type          VARCHAR(50) NOT NULL,
    entity_id            BIGINT,
    summary              TEXT NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
