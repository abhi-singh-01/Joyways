"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  listVehicles,
  type VehicleSummary,
  type VehicleStatus,
} from "@/lib/api/adminVehiclesApi";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { ErrorState } from "@/components/ui/ErrorState";

export function AdminVehiclesView() {
  const [vehicles, setVehicles] = useState<VehicleSummary[]>([]);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    listVehicles({ query, status, page, size: 20 })
      .then((result) => {
        if (cancelled) return;
        setVehicles(result.content);
        setTotalPages(result.totalPages);
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
  }, [query, status, page]);

  if (error) return <ErrorState message={error} fullPage />;

  return (
    <div>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-3xl font-semibold text-charcoal">Vehicles</h1>
          <p className="mt-1 text-charcoal-muted">Manage fleet and vehicle availability</p>
        </div>
        <Link
          href="/admin/vehicles/new"
          className="inline-flex items-center justify-center rounded-xl bg-forest px-4 py-2.5 text-sm font-medium text-white hover:bg-forest/90"
        >
          Add Vehicle
        </Link>
      </div>

      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <input
          type="search"
          placeholder="Search registration or type..."
          value={query}
          onChange={(e) => {
            setLoading(true);
            setPage(0);
            setQuery(e.target.value);
          }}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm sm:max-w-xs"
        />
        <select
          value={status}
          onChange={(e) => {
            setLoading(true);
            setPage(0);
            setStatus(e.target.value);
          }}
          className="rounded-xl border border-border px-4 py-2.5 text-sm"
        >
          <option value="">All statuses</option>
          {(["AVAILABLE", "ASSIGNED", "ON_TRIP", "MAINTENANCE", "INACTIVE"] as VehicleStatus[]).map(
            (s) => (
              <option key={s} value={s}>
                {s.replaceAll("_", " ")}
              </option>
            )
          )}
        </select>
      </div>

      <div className="mt-6 overflow-x-auto rounded-2xl border border-border bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-border bg-surface text-charcoal-muted">
            <tr>
              <th className="px-4 py-3 font-medium">Registration</th>
              <th className="px-4 py-3 font-medium">Vehicle Type</th>
              <th className="px-4 py-3 font-medium">Capacity</th>
              <th className="px-4 py-3 font-medium">Ownership</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Current Trip</th>
              <th className="px-4 py-3 font-medium"></th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-charcoal-muted">
                  Loading vehicles...
                </td>
              </tr>
            ) : vehicles.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-charcoal-muted">
                  No vehicles found
                </td>
              </tr>
            ) : (
              vehicles.map((vehicle) => (
                <tr key={vehicle.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 font-medium text-forest">
                    {vehicle.registrationNumber}
                  </td>
                  <td className="px-4 py-3">{vehicle.vehicleType}</td>
                  <td className="px-4 py-3">{vehicle.seatingCapacity}</td>
                  <td className="px-4 py-3">{vehicle.ownershipType}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={vehicle.status} />
                  </td>
                  <td className="px-4 py-3 text-charcoal-muted">
                    {vehicle.currentTripReference ?? "—"}
                  </td>
                  <td className="px-4 py-3">
                    <Link
                      href={`/admin/vehicles/${vehicle.id}`}
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
