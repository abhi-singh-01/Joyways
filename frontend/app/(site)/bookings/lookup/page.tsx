"use client";

import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { getBooking, getBookingReceipt, downloadBookingReceiptPdf, cancelBooking, type BookingDetail, type BookingReceipt } from "@/lib/api/bookingApi";
import { PaymentCheckout } from "@/components/booking/PaymentCheckout";
import { TripReviewForm } from "@/components/booking/TripReviewForm";
import { paidBookingWhatsAppMessage, whatsappUrl } from "@/lib/publicContact";
import { formatDateRange, formatPrice } from "@/lib/format";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import {
  bookingLookupSchema,
  fieldErrorsFromZod,
  type FieldErrors,
} from "@/lib/formValidation";

async function fetchBookingAndReceipt(reference: string, email: string) {
  const detail = await getBooking(reference.trim(), email.trim());
  const receipt =
    detail.paymentStatus === "PAID"
    || detail.paymentStatus === "REFUNDED"
    || detail.paymentStatus === "PARTIALLY_REFUNDED"
      ? await getBookingReceipt(reference.trim(), email.trim())
      : null;
  return { detail, receipt };
}

function BookingLookupContent({ initialReference }: Readonly<{ initialReference: string }>) {
  const [reference, setReference] = useState(
    initialReference ? initialReference.toUpperCase() : ""
  );
  const [email, setEmail] = useState("");
  const [fieldErrors, setFieldErrors] = useState<
    FieldErrors<{ reference: string; email: string }>
  >({});
  const [booking, setBooking] = useState<BookingDetail | null>(null);
  const [receipt, setReceipt] = useState<BookingReceipt | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [downloadingPdf, setDownloadingPdf] = useState(false);

  async function lookup(ref = reference, em = email) {
    const validation = bookingLookupSchema.safeParse({
      reference: ref,
      email: em,
    });

    if (!validation.success) {
      setFieldErrors(fieldErrorsFromZod(validation));
      setError("Please fix the highlighted fields.");
      return;
    }

    setFieldErrors({});
    setError("");
    setLoading(true);
    setBooking(null);
    setReceipt(null);
    try {
      const { detail, receipt: receiptData } = await fetchBookingAndReceipt(ref, em);
      setBooking(detail);
      setReceipt(receiptData);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Booking not found");
    } finally {
      setLoading(false);
    }
  }

  async function handleCancel() {
    if (!booking || !window.confirm("Are you sure you want to cancel this booking?")) {
      return;
    }
    setCancelling(true);
    setError("");
    try {
      const updated = await cancelBooking(booking.bookingReference, email.trim());
      setBooking({ ...booking, status: updated.status, paymentStatus: updated.paymentStatus });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Cancellation failed");
    } finally {
      setCancelling(false);
    }
  }

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="font-display text-3xl font-semibold text-charcoal">Find Your Booking</h1>
      <p className="mt-2 text-charcoal-muted">
        Enter your booking reference and the email used at checkout. For your privacy, we never
        include your email in shared links.
      </p>

      <form
        className="mt-8 space-y-4 rounded-2xl border border-border bg-white p-6"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          void lookup();
        }}
      >
        <div>
          <Label htmlFor="reference">Booking Reference *</Label>
          <Input
            id="reference"
            required
            aria-invalid={Boolean(fieldErrors.reference)}
            error={fieldErrors.reference}
            value={reference}
            onChange={(e) => setReference(e.target.value.toUpperCase())}
            placeholder="JW-2026-00001"
          />
        </div>
        <div>
          <Label htmlFor="email">Email *</Label>
          <Input
            id="email"
            type="email"
            required
            autoComplete="email"
            aria-invalid={Boolean(fieldErrors.email)}
            error={fieldErrors.email}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
        <Button type="submit" disabled={loading}>
          {loading ? "Searching..." : "View Booking"}
        </Button>
      </form>

      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
          {error}
        </p>
      )}

      {booking && (
        <div className="mt-8 rounded-2xl border border-border bg-white p-6">
          <p className="text-sm text-charcoal-muted">Booking Reference</p>
          <p className="font-display text-2xl font-semibold text-forest">{booking.bookingReference}</p>
          <div className="mt-4 space-y-2 text-sm">
            <p><span className="text-charcoal-muted">Package:</span> {booking.packageTitle}</p>
            <p><span className="text-charcoal-muted">Dates:</span> {formatDateRange(booking.startDate, booking.endDate)}</p>
            <p><span className="text-charcoal-muted">Travellers:</span> {booking.numberOfTravellers}</p>
            {booking.pickupPoint && (
              <p><span className="text-charcoal-muted">Pickup:</span> {booking.pickupPoint}</p>
            )}
            <p><span className="text-charcoal-muted">Total:</span> {formatPrice(booking.totalAmount)}</p>
            <p><span className="text-charcoal-muted">Booking Status:</span> {booking.status.replaceAll("_", " ")}</p>
            <p><span className="text-charcoal-muted">Payment Status:</span> {booking.paymentStatus}</p>
          </div>

          {(booking.paymentStatus === "PAID" || booking.status === "CONFIRMED") && (
            <a
              href={whatsappUrl(paidBookingWhatsAppMessage(booking))}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-4 inline-flex rounded-xl bg-[#25D366] px-4 py-2.5 text-sm font-medium text-white"
            >
              Confirm on WhatsApp
            </a>
          )}

          {receipt && (
            <div className="mt-6 border-t border-border pt-6">
              <h2 className="font-semibold text-charcoal">{receipt.title}</h2>
              <p className="mt-2 text-sm text-charcoal-muted">{receipt.brandName}</p>
              {receipt.razorpayPaymentId && (
                <p className="mt-2 text-sm">
                  <span className="text-charcoal-muted">Payment ID:</span> {receipt.razorpayPaymentId}
                </p>
              )}
              <Button
                type="button"
                variant="outline"
                className="mt-4"
                disabled={downloadingPdf}
                onClick={() => {
                  setDownloadingPdf(true);
                  downloadBookingReceiptPdf(booking.bookingReference, email.trim())
                    .catch((err) => setError(err instanceof Error ? err.message : "Download failed"))
                    .finally(() => setDownloadingPdf(false));
                }}
              >
                {downloadingPdf ? "Preparing PDF..." : "Download PDF receipt"}
              </Button>
            </div>
          )}

          {booking.status === "PENDING_PAYMENT" && booking.paymentStatus !== "PAID" && (
            <div className="mt-6 border-t border-border pt-6">
              <h2 className="font-semibold text-charcoal">Complete payment</h2>
              <div className="mt-4">
                <PaymentCheckout
                  embedded
                  booking={booking}
                  customer={{
                    fullName: booking.customerName,
                    email: booking.customerEmail,
                    mobile: booking.customerMobile,
                  }}
                  onPaid={async (paidReceipt) => {
                    setReceipt(paidReceipt);
                    try {
                      const detail = await getBooking(booking.bookingReference, email.trim());
                      setBooking(detail);
                    } catch {
                      setBooking({
                        ...booking,
                        status: "CONFIRMED",
                        paymentStatus: "PAID",
                      });
                    }
                  }}
                />
              </div>
              <div className="mt-6">
                <Button
                  type="button"
                  variant="outline"
                  disabled={cancelling}
                  onClick={() => void handleCancel()}
                  className="border-red-200 text-red-700 hover:bg-red-50"
                >
                  {cancelling ? "Cancelling..." : "Cancel Booking"}
                </Button>
              </div>
            </div>
          )}

          {booking.reviewEligible && !booking.reviewSubmitted && (
            <TripReviewForm
              bookingReference={booking.bookingReference}
              email={email.trim()}
              onSubmitted={() => setBooking({ ...booking, reviewSubmitted: true })}
            />
          )}

          {booking.reviewSubmitted && (
            <p className="mt-6 border-t border-border pt-6 text-sm text-charcoal-muted">
              Thank you — your review is on the trip page.
            </p>
          )}
        </div>
      )}

      <div className="mt-8">
        <Link href="/trips" className="text-sm font-medium text-forest hover:underline">
          Browse trips
        </Link>
      </div>
    </div>
  );
}

function BookingLookupPageInner() {
  const searchParams = useSearchParams();
  const initialReference = searchParams.get("reference") ?? "";

  return (
    <BookingLookupContent
      key={initialReference}
      initialReference={initialReference}
    />
  );
}

export default function BookingLookupPage() {
  return (
    <Suspense fallback={<p className="text-charcoal-muted">Loading...</p>}>
      <BookingLookupPageInner />
    </Suspense>
  );
}
