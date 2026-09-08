"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listAdminBookings, type AdminBookingSummary } from "@/lib/api/adminBookingsApi";
import { formatPrice } from "@/lib/format";
import { ErrorState } from "@/components/ui/ErrorState";

export function AdminBookingsView() {
  const [bookings, setBookings] = useState<AdminBookingSummary[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listAdminBookings()
      .then(setBookings)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load bookings"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <p className="text-charcoal-muted">Loading bookings...</p>;
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

      <div className="mt-8 overflow-x-auto rounded-2xl border border-border bg-white">
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
          <tbody>
            {bookings.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-charcoal-muted">
                  No bookings yet
                </td>
              </tr>
            ) : (
              bookings.map((booking) => (
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
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
