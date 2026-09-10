ALTER TABLE bookings
    ADD COLUMN pickup_point VARCHAR(200);

CREATE TABLE waitlist_entries (
    id               BIGSERIAL PRIMARY KEY,
    package_id       BIGINT NOT NULL REFERENCES packages(id),
    full_name        VARCHAR(120) NOT NULL,
    email            VARCHAR(254) NOT NULL,
    mobile           VARCHAR(10),
    seats_requested  INTEGER NOT NULL DEFAULT 1,
    notified_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_waitlist_package_email UNIQUE (package_id, email),
    CONSTRAINT chk_waitlist_seats CHECK (seats_requested >= 1 AND seats_requested <= 20)
);

CREATE INDEX idx_waitlist_package_waiting
    ON waitlist_entries (package_id, created_at)
    WHERE notified_at IS NULL;

CREATE TABLE package_reviews (
    id             BIGSERIAL PRIMARY KEY,
    booking_id     BIGINT NOT NULL UNIQUE REFERENCES bookings(id),
    package_id     BIGINT NOT NULL REFERENCES packages(id),
    customer_name  VARCHAR(120) NOT NULL,
    rating         SMALLINT NOT NULL,
    comment        VARCHAR(1000),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_package_reviews_rating CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX idx_package_reviews_package ON package_reviews (package_id, created_at DESC);
