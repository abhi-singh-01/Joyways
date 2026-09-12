"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import {
  getAdminBooking,
  cancelAdminBooking,
  refundAdminBooking,
  type AdminBookingDetail,
} from "@/lib/api/adminBookingsApi";
import { formatDateRange, formatPrice } from "@/lib/format";
import { ErrorState } from "@/components/ui/ErrorState";
import { Button } from "@/components/ui/Button";

function canCancelUnpaid(booking: AdminBookingDetail) {
  return booking.bookingStatus === "PENDING_PAYMENT";
}

function isOfflinePaid(booking: AdminBookingDetail) {
  return booking.paymentStatus === "PAID" && !booking.razorpayPaymentId;
}

function canRefund(booking: AdminBookingDetail) {
  if (booking.bookingStatus === "COMPLETED" || booking.bookingStatus === "REFUNDED") {
    return false;
  }
  const record = booking.paymentRecordStatus;
  if (record !== "CAPTURED" && record !== "PARTIALLY_REFUNDED") {
    return false;
  }
  return Boolean(booking.razorpayPaymentId) || isOfflinePaid(booking);
}

export default function AdminBookingDetailPage() {
  const params = useParams<{ id: string }>();
  const bookingId = Number(params.id);
  const hasValidId = Number.isInteger(bookingId) && bookingId > 0;

  const [booking, setBooking] = useState<AdminBookingDetail | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(hasValidId);
  const [cancelling, setCancelling] = useState(false);
  const [refunding, setRefunding] = useState(false);

  useEffect(() => {
    if (!hasValidId) return;

    let cancelled = false;
    getAdminBooking(bookingId)
      .then((detail) => {
        if (!cancelled) setBooking(detail);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to load booking");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [bookingId, hasValidId]);

  if (!hasValidId) {
    return <ErrorState message="Invalid booking ID" fullPage />;
  }

  async function handleCancel() {
    if (!booking || !window.confirm("Cancel this unpaid booking? Seats will be restored.")) {
      return;
    }
    setCancelling(true);
    setError("");
    try {
      await cancelAdminBooking(booking.bookingId);
      setBooking({ ...booking, bookingStatus: "CANCELLED" });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Cancellation failed");
    } finally {
      setCancelling(false);
    }
  }

  async function handleRefund() {
    if (!booking) return;
    const remaining = Math.max(0, booking.totalAmount - (booking.refundedAmount ?? 0));
    const offline = isOfflinePaid(booking);
    if (
      !window.confirm(
        offline
          ? `Void the offline payment for ${booking.bookingReference} and restore seats?`
          : `Refund ₹${remaining.toLocaleString("en-IN")} to the original payment method for ${booking.bookingReference}?`
      )
    ) {
      return;
    }
    setRefunding(true);
    setError("");
    try {
      const result = await refundAdminBooking(booking.bookingId);
      const refreshed = await getAdminBooking(booking.bookingId);
      setBooking(refreshed);
      if (result.refundState === "PENDING") {
        setError("Refund submitted to Razorpay and is pending confirmation.");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Refund failed");
    } finally {
      setRefunding(false);
    }
  }

  if (loading) return <p className="text-charcoal-muted">Loading booking...</p>;
  if (error && !booking) {
    return <ErrorState title="Booking not found" message={error} />;
  }
  if (!booking) {
    return <ErrorState title="Booking not found" message="Unable to load booking" />;
  }

  return (
    <div className="max-w-3xl">
      <Link href="/admin/bookings" className="text-sm font-medium text-forest hover:underline">
        ← Back to bookings
      </Link>
      <h1 className="mt-4 font-display text-3xl font-semibold text-charcoal">
        {booking.bookingReference}
      </h1>
      <p className="mt-1 text-charcoal-muted">{booking.packageTitle}</p>

      {error && (
        <p className="mt-4 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800" role="alert">
          {error}
        </p>
      )}
      {booking.bookingStatus === "PENDING_PAYMENT" && (
        <p className="mt-4 rounded-lg bg-sky-50 px-4 py-3 text-sm text-sky-900">
          Payment is pending. Share booking reference {booking.bookingReference} and{" "}
          {booking.customerEmail} so the customer can pay on the booking lookup page.
          {booking.createdByAdmin
            ? " Seats stay held until they pay or you cancel. This booking does not auto-expire."
            : " Unpaid website bookings expire after the payment hold window."}
        </p>
      )}
      {isOfflinePaid(booking) && (
        <p className="mt-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
          Marked as paid offline
          {booking.offlinePaymentReference
            ? ` (ref ${booking.offlinePaymentReference})`
            : ""}.
          Use Void offline payment to restore seats if this was created by mistake.
        </p>
      )}
      {(booking.adminNotes || booking.offlinePaymentReference) && (
        <div className="mt-4 rounded-lg border border-border bg-white px-4 py-3 text-sm">
          {booking.offlinePaymentReference && (
            <p>
              <span className="text-charcoal-muted">Offline payment ref:</span>{" "}
              {booking.offlinePaymentReference}
            </p>
          )}
          {booking.adminNotes && (
            <p className={booking.offlinePaymentReference ? "mt-1" : undefined}>
              <span className="text-charcoal-muted">Admin notes:</span> {booking.adminNotes}
            </p>
          )}
        </div>
      )}

      <div className="mt-8 grid gap-6 md:grid-cols-2">
        <div className="rounded-2xl border border-border bg-white p-5">
          <h2 className="font-semibold text-charcoal">Customer</h2>
          <dl className="mt-3 space-y-2 text-sm">
            <div><dt className="text-charcoal-muted">Name</dt><dd>{booking.customerName}</dd></div>
            <div><dt className="text-charcoal-muted">Email</dt><dd>{booking.customerEmail}</dd></div>
            <div><dt className="text-charcoal-muted">Mobile</dt><dd>{booking.customerMobile}</dd></div>
          </dl>
        </div>
        <div className="rounded-2xl border border-border bg-white p-5">
          <h2 className="font-semibold text-charcoal">Payment</h2>
          <dl className="mt-3 space-y-2 text-sm">
            <div><dt className="text-charcoal-muted">Amount</dt><dd>{formatPrice(booking.totalAmount)}</dd></div>
            <div><dt className="text-charcoal-muted">Refunded</dt><dd>{formatPrice(booking.refundedAmount ?? 0)}</dd></div>
            <div><dt className="text-charcoal-muted">Booking Status</dt><dd>{booking.bookingStatus.replaceAll("_", " ")}</dd></div>
            <div><dt className="text-charcoal-muted">Payment Status</dt><dd>{booking.paymentStatus.replaceAll("_", " ")}</dd></div>
            <div><dt className="text-charcoal-muted">Payment record</dt><dd>{booking.paymentRecordStatus?.replaceAll("_", " ") || "—"}</dd></div>
            <div><dt className="text-charcoal-muted">Order ID</dt><dd>{booking.razorpayOrderId || "—"}</dd></div>
            <div><dt className="text-charcoal-muted">Payment ID</dt><dd>{booking.razorpayPaymentId || "—"}</dd></div>
            <div><dt className="text-charcoal-muted">Refund ID</dt><dd>{booking.razorpayRefundId || "—"}</dd></div>
            <div><dt className="text-charcoal-muted">Signature Verified</dt><dd>{booking.signatureVerified ? "Yes" : "No"}</dd></div>
          </dl>
        </div>
      </div>

      <div className="mt-6 rounded-2xl border border-border bg-white p-5">
        <h2 className="font-semibold text-charcoal">Trip</h2>
        <p className="mt-2 text-sm">{formatDateRange(booking.startDate, booking.endDate)}</p>
        <p className="text-sm text-charcoal-muted">{booking.numberOfTravellers} travellers</p>
        {booking.pickupPoint && (
          <p className="mt-2 text-sm"><span className="text-charcoal-muted">Pickup:</span> {booking.pickupPoint}</p>
        )}
      </div>

      <div className="mt-6 rounded-2xl border border-border bg-white p-5">
        <h2 className="font-semibold text-charcoal">Travellers</h2>
        <ul className="mt-3 space-y-2 text-sm">
          {booking.travellers.map((t) => (
            <li key={`${t.fullName}-${t.idDocumentType}`}>{t.fullName} · {t.idDocumentType}</li>
          ))}
        </ul>
      </div>

      {(canCancelUnpaid(booking) || canRefund(booking)) && (
        <div className="mt-8 flex flex-wrap gap-3">
          {canCancelUnpaid(booking) && (
            <Button
              type="button"
              variant="outline"
              disabled={cancelling}
              onClick={() => void handleCancel()}
              className="border-red-200 text-red-700 hover:bg-red-50"
            >
              {cancelling ? "Cancelling..." : "Cancel unpaid booking"}
            </Button>
          )}
          {canRefund(booking) && (
            <Button
              type="button"
              variant="outline"
              disabled={refunding}
              onClick={() => void handleRefund()}
            >
              {refunding
                ? isOfflinePaid(booking)
                  ? "Voiding..."
                  : "Refunding..."
                : isOfflinePaid(booking)
                  ? "Void offline payment"
                  : "Refund payment"}
            </Button>
          )}
        </div>
      )}
    </div>
  );
}
