-- Phase 5: payment signature tracking and duplicate payment protection

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS signature_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_razorpay_payment_id_unique
    ON payments (razorpay_payment_id)
    WHERE razorpay_payment_id IS NOT NULL;
