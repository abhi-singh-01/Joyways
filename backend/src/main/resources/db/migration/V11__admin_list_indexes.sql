CREATE INDEX IF NOT EXISTS idx_bookings_created_at ON bookings (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bookings_payment_status ON bookings (payment_status);
CREATE INDEX IF NOT EXISTS idx_payments_booking_created_at ON payments (booking_id, created_at DESC);
