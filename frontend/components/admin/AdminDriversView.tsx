"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  listDrivers,
  type DriverSummary,
  type DriverStatus,
} from "@/lib/api/adminDriversApi";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { formatDate } from "@/lib/format";
import { ErrorState } from "@/components/ui/ErrorState";

export function AdminDriversView() {
  const [drivers, setDrivers] = useState<DriverSummary[]>([]);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [licenseFilter, setLicenseFilter] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    listDrivers({ query, status, licenseFilter, page, size: 20 })
      .then((result) => {
        if (cancelled) return;
        setDrivers(result.content);
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
  }, [query, status, licenseFilter, page]);

  if (error) return <ErrorState message={error} fullPage />;

  return (
    <div>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-3xl font-semibold text-charcoal">Drivers</h1>
          <p className="mt-1 text-charcoal-muted">Manage drivers and license compliance</p>
        </div>
        <Link
          href="/admin/drivers/new"
          className="inline-flex items-center justify-center rounded-xl bg-forest px-4 py-2.5 text-sm font-medium text-white hover:bg-forest/90"
        >
          Add Driver
        </Link>
      </div>

      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <input
          type="search"
          placeholder="Search name, phone, license..."
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
          {(["AVAILABLE", "ASSIGNED", "ON_TRIP", "INACTIVE"] as DriverStatus[]).map((s) => (
            <option key={s} value={s}>
              {s.replaceAll("_", " ")}
            </option>
          ))}
        </select>
        <select
          value={licenseFilter}
          onChange={(e) => {
            setLoading(true);
            setPage(0);
            setLicenseFilter(e.target.value);
          }}
          className="rounded-xl border border-border px-4 py-2.5 text-sm"
        >
          <option value="">All licenses</option>
          <option value="EXPIRING_SOON">Expiring soon</option>
          <option value="EXPIRED">Expired</option>
        </select>
      </div>

      <div className="mt-6 overflow-x-auto rounded-2xl border border-border bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-border bg-surface text-charcoal-muted">
            <tr>
              <th className="px-4 py-3 font-medium">Driver</th>
              <th className="px-4 py-3 font-medium">Phone</th>
              <th className="px-4 py-3 font-medium">License</th>
              <th className="px-4 py-3 font-medium">License Expiry</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Current Trip</th>
              <th className="px-4 py-3 font-medium"></th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-charcoal-muted">
                  Loading drivers...
                </td>
              </tr>
            ) : drivers.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-charcoal-muted">
                  No drivers found
                </td>
              </tr>
            ) : (
              drivers.map((driver) => (
                <tr key={driver.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 font-medium">{driver.name}</td>
                  <td className="px-4 py-3">{driver.phone}</td>
                  <td className="px-4 py-3">{driver.licenseNumber}</td>
                  <td className="px-4 py-3">
                    <div>{formatDate(driver.licenseExpiryDate)}</div>
                    <StatusBadge status={driver.licenseStatus} />
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={driver.status} />
                  </td>
                  <td className="px-4 py-3 text-charcoal-muted">
                    {driver.currentTripReference ?? "—"}
                  </td>
                  <td className="px-4 py-3">
                    <Link
                      href={`/admin/drivers/${driver.id}`}
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
