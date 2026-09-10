"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listAdminBookings, type AdminBookingSummary } from "@/lib/api/adminBookingsApi";
import { formatPrice } from "@/lib/format";
import { ErrorState } from "@/components/ui/ErrorState";
import { AdminTableStatusRow } from "@/components/admin/AdminTableStatusRow";

const BOOKING_STATUSES = [
  "PENDING_PAYMENT",
  "CONFIRMED",
  "CANCELLED",
  "EXPIRED",
  "COMPLETED",
  "REFUNDED",
] as const;

const PAYMENT_STATUSES = [
  "PENDING",
  "PAID",
  "FAILED",
  "REFUNDED",
  "PARTIALLY_REFUNDED",
] as const;

export function AdminBookingsView() {
  const [bookings, setBookings] = useState<AdminBookingSummary[]>([]);
  const [query, setQuery] = useState("");
  const [bookingStatus, setBookingStatus] = useState("");
  const [paymentStatus, setPaymentStatus] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    listAdminBookings({ query, bookingStatus, paymentStatus, page, size: 20 })
      .then((result) => {
        if (cancelled) return;
        setBookings(result.content);
        setTotalPages(result.totalPages);
        setError("");
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Failed to load bookings");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [query, bookingStatus, paymentStatus, page]);

  function bookingRows() {
    if (loading) {
      return <AdminTableStatusRow colSpan={8} message="Loading bookings..." />;
    }
    if (bookings.length === 0) {
      return <AdminTableStatusRow colSpan={8} message="No bookings found" />;
    }
    return bookings.map((booking) => (
      <tr key={booking.bookingId} className="border-b border-border last:border-0">
        <td className="px-4 py-3 font-medium text-forest">
          {booking.bookingReference}
        </td>
        <td className="px-4 py-3">
          <p>{booking.customerName}</p>
          <p className="text-xs text-charcoal-muted">{booking.customerEmail}</p>
        </td>
        <td className="px-4 py-3">
          <p>{booking.packageTitle}</p>
          <p className="text-xs text-charcoal-muted">{booking.destination}</p>
        </td>
        <td className="px-4 py-3">{booking.numberOfTravellers}</td>
        <td className="px-4 py-3">{formatPrice(booking.totalAmount)}</td>
        <td className="px-4 py-3">{booking.bookingStatus.replaceAll("_", " ")}</td>
        <td className="px-4 py-3">{booking.paymentStatus}</td>
        <td className="px-4 py-3">
          <Link
            href={`/admin/bookings/${booking.bookingId}`}
            className="font-medium text-forest hover:underline"
          >
            View
          </Link>
        </td>
      </tr>
    ));
  }

  if (error) {
    return <ErrorState title="Unable to load bookings" message={error} />;
  }

  return (
    <div>
      <h1 className="font-display text-3xl font-semibold text-charcoal">Bookings</h1>
      <p className="mt-1 text-charcoal-muted">
        All customer bookings and payment status
      </p>

      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <input
          type="search"
          placeholder="Search reference, name, email, trip..."
          value={query}
          onChange={(e) => {
            setLoading(true);
            setPage(0);
            setQuery(e.target.value);
          }}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm sm:max-w-xs"
        />
        <select
          value={bookingStatus}
          onChange={(e) => {
            setLoading(true);
            setPage(0);
            setBookingStatus(e.target.value);
          }}
          className="rounded-xl border border-border px-4 py-2.5 text-sm"
        >
          <option value="">All booking statuses</option>
          {BOOKING_STATUSES.map((status) => (
            <option key={status} value={status}>
              {status.replaceAll("_", " ")}
            </option>
          ))}
        </select>
        <select
          value={paymentStatus}
          onChange={(e) => {
            setLoading(true);
            setPage(0);
            setPaymentStatus(e.target.value);
          }}
          className="rounded-xl border border-border px-4 py-2.5 text-sm"
        >
          <option value="">All payment statuses</option>
          {PAYMENT_STATUSES.map((status) => (
            <option key={status} value={status}>
              {status.replaceAll("_", " ")}
            </option>
          ))}
        </select>
      </div>

      <div className="mt-6 overflow-x-auto rounded-2xl border border-border bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-border bg-surface text-charcoal-muted">
            <tr>
              <th className="px-4 py-3 font-medium">Reference</th>
              <th className="px-4 py-3 font-medium">Customer</th>
              <th className="px-4 py-3 font-medium">Package</th>
              <th className="px-4 py-3 font-medium">Travellers</th>
              <th className="px-4 py-3 font-medium">Amount</th>
              <th className="px-4 py-3 font-medium">Booking</th>
              <th className="px-4 py-3 font-medium">Payment</th>
              <th className="px-4 py-3 font-medium"></th>
            </tr>
          </thead>
          <tbody>{bookingRows()}</tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-2">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => {
              setLoading(true);
              setPage((p) => p - 1);
            }}
            className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
          >
            Previous
          </button>
          <span className="text-sm text-charcoal-muted">
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= totalPages}
            onClick={() => {
              setLoading(true);
              setPage((p) => p + 1);
            }}
            className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
