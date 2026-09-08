ALTER TABLE payments
    ADD COLUMN razorpay_refund_id VARCHAR(100),
    ADD COLUMN refunded_amount DECIMAL(12, 2) NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_payments_razorpay_refund_id ON payments(razorpay_refund_id);
