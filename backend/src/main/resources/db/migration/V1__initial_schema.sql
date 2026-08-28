-- JOYWAY Initial Schema

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE packages (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    slug                VARCHAR(255) NOT NULL UNIQUE,
    destination         VARCHAR(255) NOT NULL,
    short_description   TEXT,
    full_description    TEXT,
    departure_city      VARCHAR(100) NOT NULL DEFAULT 'Delhi',
    pickup_points       TEXT,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    duration            VARCHAR(50) NOT NULL,
    price_per_person    DECIMAL(12, 2) NOT NULL,
    total_seats         INTEGER NOT NULL,
    available_seats     INTEGER NOT NULL,
    hotel_name          VARCHAR(255),
    hotel_details       TEXT,
    vehicle_type        VARCHAR(100),
    itinerary           TEXT,
    inclusions          TEXT,
    exclusions          TEXT,
    cancellation_policy TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_package_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_package_seats CHECK (available_seats >= 0 AND total_seats >= available_seats)
);

CREATE INDEX idx_packages_slug ON packages(slug);
CREATE INDEX idx_packages_status ON packages(status);
CREATE INDEX idx_packages_start_date ON packages(start_date);
CREATE INDEX idx_packages_destination ON packages(destination);

CREATE TABLE package_images (
    id                  BIGSERIAL PRIMARY KEY,
    package_id          BIGINT NOT NULL REFERENCES packages(id) ON DELETE CASCADE,
    image_url           VARCHAR(500),
    private_storage_key VARCHAR(500),
    alt_text            VARCHAR(255),
    sort_order          INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_package_images_package_id ON package_images(package_id);

CREATE TABLE customers (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    mobile      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_mobile ON customers(mobile);

CREATE TABLE bookings (
    id                  BIGSERIAL PRIMARY KEY,
    booking_reference   VARCHAR(20) NOT NULL UNIQUE,
    package_id          BIGINT NOT NULL REFERENCES packages(id),
    customer_id         BIGINT NOT NULL REFERENCES customers(id),
    number_of_travellers INTEGER NOT NULL,
    total_amount        DECIMAL(12, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    payment_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_booking_status CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'COMPLETED', 'REFUNDED')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED'))
);

CREATE INDEX idx_bookings_reference ON bookings(booking_reference);
CREATE INDEX idx_bookings_package_id ON bookings(package_id);
CREATE INDEX idx_bookings_customer_id ON bookings(customer_id);
CREATE INDEX idx_bookings_status ON bookings(status);

CREATE TABLE travellers (
    id                          BIGSERIAL PRIMARY KEY,
    booking_id                  BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    full_name                   VARCHAR(255) NOT NULL,
    date_of_birth               DATE NOT NULL,
    gender                      VARCHAR(20) NOT NULL,
    mobile                      VARCHAR(20) NOT NULL,
    email                       VARCHAR(255) NOT NULL,
    emergency_contact           VARCHAR(255) NOT NULL,
    id_document_type            VARCHAR(50),
    id_document_reference       VARCHAR(255),
    private_document_storage_key VARCHAR(500),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_travellers_booking_id ON travellers(booking_id);

CREATE TABLE payments (
    id                  BIGSERIAL PRIMARY KEY,
    booking_id          BIGINT NOT NULL REFERENCES bookings(id),
    razorpay_order_id   VARCHAR(100) UNIQUE,
    razorpay_payment_id VARCHAR(100),
    razorpay_signature  VARCHAR(500),
    amount              DECIMAL(12, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    status              VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_record_status CHECK (status IN ('CREATED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED'))
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_razorpay_order_id ON payments(razorpay_order_id);
CREATE INDEX idx_payments_razorpay_payment_id ON payments(razorpay_payment_id);

CREATE TABLE payment_webhook_events (
    id          BIGSERIAL PRIMARY KEY,
    event_id    VARCHAR(100) NOT NULL UNIQUE,
    event_type  VARCHAR(100) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    processed   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_events_event_id ON payment_webhook_events(event_id);

-- Seed roles
INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_CUSTOMER');
