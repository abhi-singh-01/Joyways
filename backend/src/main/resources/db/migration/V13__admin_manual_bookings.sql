ALTER TABLE bookings
    ADD COLUMN created_by_admin BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN admin_notes VARCHAR(500),
    ADD COLUMN offline_payment_reference VARCHAR(100);
