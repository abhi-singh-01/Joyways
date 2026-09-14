"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  listCoupons,
  type CouponSummary,
} from "@/lib/api/adminCouponsApi";
import { pageContent } from "@/lib/api/adminVehiclesApi";
import { formatDate, formatPrice } from "@/lib/format";
import { ErrorState } from "@/components/ui/ErrorState";
import { AdminTableStatusRow } from "@/components/admin/AdminTableStatusRow";

function discountLabel(coupon: CouponSummary): string {
  if (coupon.discountType === "PERCENT") {
    return `${coupon.discountValue}% off`;
  }
  return `${formatPrice(coupon.discountValue)} off`;
}

function statusLabel(coupon: CouponSummary): string {
  if (!coupon.active) return "Inactive";
  if (coupon.expired) return "Expired";
  return "Active";
}

export function AdminCouponsView() {
  const [coupons, setCoupons] = useState<CouponSummary[]>([]);
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    listCoupons({
      query,
      active: activeFilter === "" ? undefined : activeFilter === "true",
      page,
      size: 20,
    })
      .then((result) => {
        if (cancelled) return;
        setCoupons(pageContent(result));
        setTotalPages(result.totalPages ?? 0);
        setError("");
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Failed to load coupons");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [query, activeFilter, page]);

  if (error && coupons.length === 0) {
    return <ErrorState message={error} fullPage />;
  }

  return (
    <div>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-display text-3xl font-semibold text-charcoal">Coupons</h1>
          <p className="mt-1 text-sm text-charcoal-muted">
            Package-scoped offer codes. One redemption per customer email or mobile.
          </p>
        </div>
        <Link
          href="/admin/coupons/new"
          className="inline-flex rounded-xl bg-forest px-4 py-2.5 text-sm font-medium text-white"
        >
          New coupon
        </Link>
      </div>

      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <input
          value={query}
          onChange={(e) => {
            setPage(0);
            setQuery(e.target.value);
          }}
          placeholder="Search code or package"
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm sm:max-w-xs"
        />
        <select
          value={activeFilter}
          onChange={(e) => {
            setPage(0);
            setActiveFilter(e.target.value);
          }}
          className="rounded-xl border border-border px-4 py-2.5 text-sm"
        >
          <option value="">All statuses</option>
          <option value="true">Active only</option>
          <option value="false">Inactive only</option>
        </select>
      </div>

      <div className="mt-6 overflow-x-auto rounded-2xl border border-border bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-border bg-sand/40 text-charcoal-muted">
            <tr>
              <th className="px-4 py-3 font-medium">Code</th>
              <th className="px-4 py-3 font-medium">Package</th>
              <th className="px-4 py-3 font-medium">Discount</th>
              <th className="px-4 py-3 font-medium">Expires</th>
              <th className="px-4 py-3 font-medium">Uses</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium" />
            </tr>
          </thead>
          <tbody>
            {loading && <AdminTableStatusRow colSpan={7} message="Loading coupons..." />}
            {!loading && coupons.length === 0 && (
              <AdminTableStatusRow colSpan={7} message="No coupons yet" />
            )}
            {!loading &&
              coupons.map((coupon) => (
                <tr key={coupon.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 font-medium text-forest">{coupon.code}</td>
                  <td className="px-4 py-3">
                    {coupon.packageTitle}
                    <span className="block text-xs text-charcoal-muted">{coupon.destination}</span>
                  </td>
                  <td className="px-4 py-3">{discountLabel(coupon)}</td>
                  <td className="px-4 py-3">{formatDate(coupon.expiresOn)}</td>
                  <td className="px-4 py-3">{coupon.redemptionCount}</td>
                  <td className="px-4 py-3">{statusLabel(coupon)}</td>
                  <td className="px-4 py-3">
                    <Link
                      href={`/admin/coupons/${coupon.id}`}
                      className="font-medium text-forest hover:underline"
                    >
                      Edit
                    </Link>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="rounded-xl border border-border px-3 py-2 text-sm disabled:opacity-50"
          >
            Previous
          </button>
          <button
            type="button"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
            className="rounded-xl border border-border px-3 py-2 text-sm disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
