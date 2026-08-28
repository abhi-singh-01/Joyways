"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { listCalendarTrips, type TripSummary } from "@/lib/api/adminTripsApi";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { formatDate } from "@/lib/format";
import { ErrorState } from "@/components/ui/ErrorState";

function monthRange(date: Date) {
  const start = new Date(date.getFullYear(), date.getMonth(), 1);
  const end = new Date(date.getFullYear(), date.getMonth() + 1, 0);
  return {
    fromDate: start.toISOString().slice(0, 10),
    toDate: end.toISOString().slice(0, 10),
    label: start.toLocaleDateString("en-IN", { month: "long", year: "numeric" }),
  };
}

export function AdminTripCalendarView() {
  const [currentMonth, setCurrentMonth] = useState(() => new Date());
  const [trips, setTrips] = useState<TripSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const range = useMemo(() => monthRange(currentMonth), [currentMonth]);

  useEffect(() => {
    let cancelled = false;
    listCalendarTrips(range.fromDate, range.toDate)
      .then((result) => {
        if (cancelled) return;
        setTrips(result);
        setError("");
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Failed to load");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [range.fromDate, range.toDate]);

  const grouped = useMemo(() => {
    const map = new Map<string, TripSummary[]>();
    for (const trip of trips) {
      const key = trip.travelDate;
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(trip);
    }
    return [...map.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [trips]);

  if (error) return <ErrorState message={error} fullPage />;

  return (
    <div>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-3xl font-semibold text-charcoal">Trip Calendar</h1>
          <p className="mt-1 text-charcoal-muted">{range.label}</p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => {
              setLoading(true);
              setCurrentMonth(
                new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1)
              );
            }}
            className="rounded-xl border border-border px-4 py-2 text-sm"
          >
            Previous
          </button>
          <button
            type="button"
            onClick={() => {
              setLoading(true);
              setCurrentMonth(
                new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1)
              );
            }}
            className="rounded-xl border border-border px-4 py-2 text-sm"
          >
            Next
          </button>
          <Link
            href="/admin/trips"
            className="rounded-xl border border-border px-4 py-2 text-sm"
          >
            List View
          </Link>
        </div>
      </div>

      <div className="mt-8 space-y-4">
        {loading ? (
          <p className="text-charcoal-muted">Loading calendar...</p>
        ) : grouped.length === 0 ? (
          <div className="rounded-2xl border border-border bg-white p-8 text-center text-charcoal-muted">
            No trips scheduled this month
          </div>
        ) : (
          grouped.map(([date, dayTrips]) => (
            <div key={date} className="rounded-2xl border border-border bg-white p-4">
              <h2 className="font-medium text-charcoal">{formatDate(date)}</h2>
              <div className="mt-3 space-y-2">
                {dayTrips.map((trip) => (
                  <Link
                    key={trip.id}
                    href={`/admin/trips/${trip.id}`}
                    className="flex flex-col gap-2 rounded-xl border border-border p-3 hover:bg-surface sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div>
                      <p className="font-medium text-forest">{trip.tripReference}</p>
                      <p className="text-sm text-charcoal-muted">
                        {trip.destination} · {trip.totalTravellers} travellers
                      </p>
                    </div>
                    <StatusBadge status={trip.status} />
                  </Link>
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
