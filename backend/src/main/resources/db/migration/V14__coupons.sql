CREATE TABLE coupons (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(40)  NOT NULL,
    package_id      BIGINT       NOT NULL REFERENCES travel_packages (id),
    discount_type   VARCHAR(20)  NOT NULL,
    discount_value  NUMERIC(12, 2) NOT NULL,
    expires_on      DATE         NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_coupons_code UNIQUE (code),
    CONSTRAINT chk_coupons_discount_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT chk_coupons_discount_value CHECK (discount_value > 0)
);

CREATE INDEX idx_coupons_package_id ON coupons (package_id);
CREATE INDEX idx_coupons_expires_on ON coupons (expires_on);

CREATE TABLE coupon_redemptions (
    id               BIGSERIAL PRIMARY KEY,
    coupon_id        BIGINT       NOT NULL REFERENCES coupons (id),
    booking_id       BIGINT       NOT NULL REFERENCES bookings (id),
    package_id       BIGINT       NOT NULL REFERENCES travel_packages (id),
    customer_email   VARCHAR(255) NOT NULL,
    customer_mobile  VARCHAR(15)  NOT NULL,
    discount_amount  NUMERIC(12, 2) NOT NULL,
    redeemed_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_coupon_redemption_booking UNIQUE (booking_id)
);

CREATE UNIQUE INDEX uq_coupon_redemption_email
    ON coupon_redemptions (coupon_id, lower(customer_email));

CREATE UNIQUE INDEX uq_coupon_redemption_mobile
    ON coupon_redemptions (coupon_id, customer_mobile);

CREATE INDEX idx_coupon_redemptions_coupon_id ON coupon_redemptions (coupon_id);

ALTER TABLE bookings
    ADD COLUMN coupon_id BIGINT REFERENCES coupons (id),
    ADD COLUMN coupon_code VARCHAR(40),
    ADD COLUMN discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;
