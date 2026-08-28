"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { createPaymentOrder, verifyPayment } from "@/lib/api/paymentApi";
import { openRazorpayCheckout } from "@/lib/razorpay";
import type { BookingReceipt, BookingResult } from "@/lib/api/bookingApi";
import type { CustomerInput } from "@/lib/api/bookingApi";
import { formatPrice } from "@/lib/format";
import { CheckCircle } from "lucide-react";
import Link from "next/link";

type PaymentCheckoutProps = {
  booking: BookingResult;
  customer: CustomerInput;
  onReset?: () => void;
};

export function PaymentCheckout({ booking, customer, onReset }: PaymentCheckoutProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [receipt, setReceipt] = useState<BookingReceipt | null>(null);
  const [paid, setPaid] = useState(booking.paymentStatus === "PAID");

  async function handlePayNow() {
    setError("");
    setLoading(true);
    try {
      const order = await createPaymentOrder(booking.bookingReference, customer.email);

      await openRazorpayCheckout({
        keyId: order.razorpayKeyId,
        orderId: order.razorpayOrderId,
        amountInr: order.amount,
        currency: order.currency,
        customerName: customer.fullName,
        customerEmail: customer.email,
        customerMobile: customer.mobile,
        packageTitle: booking.packageTitle,
        onSuccess: async (response) => {
          try {
            const result = await verifyPayment({
              bookingReference: booking.bookingReference,
              customerEmail: customer.email,
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            setReceipt(result.receipt);
            setPaid(true);
          } catch (err) {
            setError(err instanceof Error ? err.message : "Payment verification failed");
          } finally {
            setLoading(false);
          }
        },
        onDismiss: () => {
          setLoading(false);
          setError("Payment cancelled. You can try again when ready.");
        },
        onPaymentFailed: (message) => {
          setLoading(false);
          setError(message);
        },
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to start payment");
      setLoading(false);
    }
  }

  if (paid && receipt) {
    return (
      <div className="mx-auto max-w-lg text-center">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
          <CheckCircle className="h-8 w-8 text-forest" />
        </div>
        <h1 className="mt-6 font-display text-3xl font-semibold text-charcoal">
          Payment Successful
        </h1>
        <p className="mt-2 text-charcoal-muted">
          Your booking is confirmed. Save your reference for future lookup.
        </p>
        <div className="mt-8 rounded-2xl border border-border bg-white p-6 text-left">
          <p className="text-center text-xs uppercase tracking-wide text-charcoal-muted">
            {receipt.brandName} · {receipt.title}
          </p>
          <p className="mt-4 text-sm text-charcoal-muted">Booking Reference</p>
          <p className="font-display text-2xl font-semibold text-forest">
            {receipt.bookingReference}
          </p>
          <div className="mt-4 space-y-2 text-sm">
            <p>
              <span className="text-charcoal-muted">Customer:</span> {receipt.customerName}
            </p>
            <p>
              <span className="text-charcoal-muted">Package:</span> {receipt.packageTitle}
            </p>
            <p>
              <span className="text-charcoal-muted">Destination:</span> {receipt.destination}
            </p>
            <p>
              <span className="text-charcoal-muted">Travellers:</span>{" "}
              {receipt.numberOfTravellers}
            </p>
            <p>
              <span className="text-charcoal-muted">Amount Paid:</span>{" "}
              {formatPrice(receipt.packageAmount)}
            </p>
            <p>
              <span className="text-charcoal-muted">Payment Status:</span>{" "}
              {receipt.paymentStatus}
            </p>
            {receipt.razorpayPaymentId && (
              <p>
                <span className="text-charcoal-muted">Payment ID:</span>{" "}
                {receipt.razorpayPaymentId}
              </p>
            )}
          </div>
        </div>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <Link href={`/bookings/lookup?reference=${encodeURIComponent(booking.bookingReference)}&email=${encodeURIComponent(customer.email)}`}>
            <Button variant="outline">View Booking</Button>
          </Link>
          <Link href="/trips">
            <Button>Browse more trips</Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg text-center">
      <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
        <CheckCircle className="h-8 w-8 text-forest" />
      </div>
      <h1 className="mt-6 font-display text-3xl font-semibold text-charcoal">
        Booking Created
      </h1>
      <p className="mt-2 text-charcoal-muted">
        Complete payment to confirm your seats.
      </p>
      <div className="mt-8 rounded-2xl border border-border bg-white p-6 text-left">
        <p className="text-sm text-charcoal-muted">Booking Reference</p>
        <p className="font-display text-2xl font-semibold text-forest">
          {booking.bookingReference}
        </p>
        <div className="mt-4 space-y-2 text-sm">
          <p>
            <span className="text-charcoal-muted">Trip:</span> {booking.packageTitle}
          </p>
          <p>
            <span className="text-charcoal-muted">Total:</span>{" "}
            {formatPrice(booking.totalAmount)}
          </p>
          <p>
            <span className="text-charcoal-muted">Status:</span>{" "}
            {booking.status.replaceAll("_", " ")}
          </p>
        </div>
      </div>

      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
          {error}
        </p>
      )}

      <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-center">
        <Button onClick={handlePayNow} disabled={loading} size="lg">
          {loading ? "Opening Razorpay..." : "Pay Now with Razorpay"}
        </Button>
        {onReset && (
          <Button variant="outline" onClick={onReset} disabled={loading}>
            Browse more trips
          </Button>
        )}
      </div>
      <p className="mt-4 text-xs text-charcoal-muted">
        Secure test-mode payment powered by Razorpay. Use Razorpay test cards/UPI in sandbox.
      </p>
    </div>
  );
}
