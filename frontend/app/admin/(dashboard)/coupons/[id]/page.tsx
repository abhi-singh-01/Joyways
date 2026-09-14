"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { getCoupon, type CouponDetail } from "@/lib/api/adminCouponsApi";
import { CouponForm } from "@/components/admin/CouponForm";
import { ErrorState } from "@/components/ui/ErrorState";
import { LoadingState } from "@/components/ui/LoadingState";

export default function EditCouponPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const [coupon, setCoupon] = useState<CouponDetail | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) {
      setError("Invalid coupon ID");
      setLoading(false);
      return;
    }
    getCoupon(id)
      .then(setCoupon)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <LoadingState message="Loading coupon..." fullPage />;
  if (error || !coupon) return <ErrorState message={error || "Coupon not found"} fullPage />;

  return (
    <div>
      <Link href="/admin/coupons" className="text-sm font-medium text-forest hover:underline">
        ← Back to coupons
      </Link>
      <h1 className="mt-4 font-display text-3xl font-semibold text-charcoal">{coupon.code}</h1>
      <p className="mt-1 text-sm text-charcoal-muted">
        {coupon.packageTitle} · {coupon.redemptionCount} redemption
        {coupon.redemptionCount === 1 ? "" : "s"}
      </p>
      <div className="mt-8">
        <CouponForm initial={coupon} />
      </div>
    </div>
  );
}
