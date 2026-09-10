"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  getTrip,
  assignVehicleToTrip,
  assignDriverToTrip,
  addBookingToTrip,
  removeBookingFromTrip,
  updateTripStatus,
  type TripDetail,
  type TripStatus,
} from "@/lib/api/adminTripsApi";
import { listVehicles } from "@/lib/api/adminVehiclesApi";
import { listDrivers } from "@/lib/api/adminDriversApi";
import { listAdminBookings } from "@/lib/api/adminBookingsApi";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { formatDate } from "@/lib/format";
import { ErrorState } from "@/components/ui/ErrorState";
import { LoadingState } from "@/components/ui/LoadingState";

const NEXT_STATUS: Partial<Record<TripStatus, TripStatus>> = {
  ASSIGNED: "READY",
  READY: "IN_PROGRESS",
  IN_PROGRESS: "COMPLETED",
};

export function AdminTripDetailView({ tripId }: Readonly<{ tripId: number }>) {
  const router = useRouter();
  const [trip, setTrip] = useState<TripDetail | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionError, setActionError] = useState("");
  const [vehicleId, setVehicleId] = useState("");
  const [driverId, setDriverId] = useState("");
  const [bookingId, setBookingId] = useState("");
  const [vehicles, setVehicles] = useState<{ id: number; label: string }[]>([]);
  const [drivers, setDrivers] = useState<{ id: number; label: string }[]>([]);
  const [bookings, setBookings] = useState<{ id: number; label: string }[]>([]);

  async function reload() {
    const detail = await getTrip(tripId);
    setTrip(detail);
  }

  useEffect(() => {
    Promise.all([
      getTrip(tripId).then(async (detail) => {
        const bookingPage = await listAdminBookings({
          bookingStatus: "CONFIRMED",
          paymentStatus: "PAID",
          destination: detail.destination,
          size: 100,
        });
        return { detail, bookingPage };
      }),
      listVehicles({ status: "AVAILABLE", size: 100 }),
      listDrivers({ status: "AVAILABLE", size: 100 }),
    ])
      .then(([{ detail, bookingPage }, vehiclePage, driverPage]) => {
        setTrip(detail);
        setVehicles(
          vehiclePage.content.map((v) => ({
            id: v.id,
            label: `${v.registrationNumber} (${v.seatingCapacity} seats)`,
          }))
        );
        setDrivers(
          driverPage.content.map((d) => ({
            id: d.id,
            label: `${d.name} · ${d.licenseStatus}`,
          }))
        );
        setBookings(
          bookingPage.content.map((b) => ({
            id: b.bookingId,
            label: `${b.bookingReference} · ${b.numberOfTravellers} travellers`,
          }))
        );
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [tripId]);

  async function runAction(action: () => Promise<unknown>) {
    setActionError("");
    try {
      await action();
      await reload();
      router.refresh();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Action failed");
    }
  }

  if (loading) return <LoadingState message="Loading trip..." fullPage />;
  if (error || !trip) return <ErrorState message={error || "Trip not found"} fullPage />;

  const nextStatus = NEXT_STATUS[trip.status];

  return (
    <div>
      <Link href="/admin/trips" className="text-sm text-forest hover:underline">
        ← Back to trips
      </Link>

      <div className="mt-4 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h1 className="font-display text-3xl font-semibold text-charcoal">
            {trip.tripReference}
          </h1>
          <p className="mt-1 text-charcoal-muted">
            {trip.packageTitle} · {trip.destination}
          </p>
        </div>
        <StatusBadge status={trip.status} />
      </div>

      {actionError && (
        <p className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {actionError}
        </p>
      )}

      <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <InfoCard label="Travel Date" value={formatDate(trip.travelDate)} />
        <InfoCard label="Travellers" value={String(trip.totalTravellers)} />
        <InfoCard
          label="Capacity"
          value={
            trip.vehicleCapacity != null
              ? `${trip.totalTravellers} / ${trip.vehicleCapacity} (${trip.remainingCapacity ?? 0} left)`
              : "No vehicle assigned"
          }
        />
        <InfoCard label="Vehicle" value={trip.vehicle?.registrationNumber ?? "Not assigned"} />
        <InfoCard label="Driver" value={trip.driver?.name ?? "Not assigned"} />
        <InfoCard label="Pickup" value={trip.pickupLocation ?? "—"} />
        <InfoCard label="Drop" value={trip.dropLocation ?? "—"} />
      </div>

      <div className="mt-8 grid gap-6 xl:grid-cols-2">
        <section className="rounded-2xl border border-border bg-white p-5">
          <h2 className="font-medium text-charcoal">Assigned Bookings</h2>
          <div className="mt-4 space-y-3">
            {trip.bookings.length === 0 ? (
              <p className="text-sm text-charcoal-muted">No bookings assigned yet</p>
            ) : (
              trip.bookings.map((booking) => (
                <div
                  key={booking.bookingId}
                  className="flex items-center justify-between rounded-xl border border-border p-3"
                >
                  <div>
                    <p className="font-medium">{booking.bookingReference}</p>
                    <p className="text-sm text-charcoal-muted">
                      {booking.customerName} · {booking.numberOfTravellers} travellers
                    </p>
                  </div>
                  {!["IN_PROGRESS", "COMPLETED", "CANCELLED"].includes(trip.status) && (
                    <button
                      type="button"
                      onClick={() =>
                        runAction(() => removeBookingFromTrip(tripId, booking.bookingId))
                      }
                      className="text-sm text-red-600 hover:underline"
                    >
                      Remove
                    </button>
                  )}
                </div>
              ))
            )}
          </div>

          {!["IN_PROGRESS", "COMPLETED", "CANCELLED"].includes(trip.status) && (
            <div className="mt-4 flex flex-col gap-2 sm:flex-row">
              <select
                value={bookingId}
                onChange={(e) => setBookingId(e.target.value)}
                className="flex-1 rounded-xl border border-border px-3 py-2 text-sm"
              >
                <option value="">Select confirmed booking</option>
                {bookings.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.label}
                  </option>
                ))}
              </select>
              <button
                type="button"
                disabled={!bookingId}
                onClick={() =>
                  runAction(() => addBookingToTrip(tripId, Number(bookingId)))
                }
                className="rounded-xl bg-forest px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
              >
                Add Booking
              </button>
            </div>
          )}
        </section>

        <section className="rounded-2xl border border-border bg-white p-5">
          <h2 className="font-medium text-charcoal">Assignments</h2>
          <div className="mt-4 space-y-4">
            <div>
              <label htmlFor="trip-vehicle" className="text-sm text-charcoal-muted">Vehicle</label>
              <div className="mt-1 flex flex-col gap-2 sm:flex-row">
                <select
                  id="trip-vehicle"
                  value={vehicleId}
                  onChange={(e) => setVehicleId(e.target.value)}
                  className="flex-1 rounded-xl border border-border px-3 py-2 text-sm"
                >
                  <option value="">Select vehicle</option>
                  {vehicles.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.label}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  disabled={!vehicleId || ["COMPLETED", "CANCELLED"].includes(trip.status)}
                  onClick={() =>
                    runAction(() => assignVehicleToTrip(tripId, Number(vehicleId)))
                  }
                  className="rounded-xl border border-border px-4 py-2 text-sm disabled:opacity-50"
                >
                  Assign Vehicle
                </button>
              </div>
            </div>
            <div>
              <label htmlFor="trip-driver" className="text-sm text-charcoal-muted">Driver</label>
              <div className="mt-1 flex flex-col gap-2 sm:flex-row">
                <select
                  id="trip-driver"
                  value={driverId}
                  onChange={(e) => setDriverId(e.target.value)}
                  className="flex-1 rounded-xl border border-border px-3 py-2 text-sm"
                >
                  <option value="">Select driver</option>
                  {drivers.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.label}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  disabled={!driverId || ["COMPLETED", "CANCELLED"].includes(trip.status)}
                  onClick={() =>
                    runAction(() => assignDriverToTrip(tripId, Number(driverId)))
                  }
                  className="rounded-xl border border-border px-4 py-2 text-sm disabled:opacity-50"
                >
                  Assign Driver
                </button>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div className="mt-8 flex flex-wrap gap-3">
        {nextStatus && (
          <button
            type="button"
            onClick={() => runAction(() => updateTripStatus(tripId, nextStatus))}
            className="rounded-xl bg-forest px-4 py-2.5 text-sm font-medium text-white"
          >
            Mark {nextStatus.replaceAll("_", " ")}
          </button>
        )}
        {!["COMPLETED", "CANCELLED", "IN_PROGRESS"].includes(trip.status) && (
          <button
            type="button"
            onClick={() => runAction(() => updateTripStatus(tripId, "CANCELLED"))}
            className="rounded-xl border border-red-200 px-4 py-2.5 text-sm font-medium text-red-700"
          >
            Cancel Trip
          </button>
        )}
      </div>
    </div>
  );
}

function InfoCard({ label, value }: Readonly<{ label: string; value: string }>) {
  return (
    <div className="rounded-2xl border border-border bg-white p-4">
      <p className="text-xs uppercase tracking-wide text-charcoal-muted">{label}</p>
      <p className="mt-2 text-sm font-medium text-charcoal">{value}</p>
    </div>
  );
}
