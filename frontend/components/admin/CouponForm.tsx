"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  createCoupon,
  updateCoupon,
  type CouponDetail,
  type CreateCouponPayload,
} from "@/lib/api/adminCouponsApi";
import { listAdminPackages, type AdminPackageSummary } from "@/lib/api/adminPackagesApi";

type CouponFormProps = {
  initial?: CouponDetail;
};

function kolkataToday(): string {
  return new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
}

export function CouponForm({ initial }: Readonly<CouponFormProps>) {
  const router = useRouter();
  const [packages, setPackages] = useState<AdminPackageSummary[]>([]);
  const [form, setForm] = useState({
    code: initial?.code ?? "",
    packageId: initial ? String(initial.packageId) : "",
    discountType: (initial?.discountType ?? "PERCENT") as "PERCENT" | "FIXED",
    discountValue: initial?.discountValue ?? 10,
    expiresOn: initial?.expiresOn ?? "",
    active: initial?.active ?? true,
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    listAdminPackages()
      .then((items) => setPackages(Array.isArray(items) ? items : []))
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Failed to load packages");
      });
  }, []);

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      if (initial) {
        const result = await updateCoupon(initial.id, {
          packageId: Number(form.packageId),
          discountType: form.discountType,
          discountValue: Number(form.discountValue),
          expiresOn: form.expiresOn,
          active: form.active,
        });
        router.push(`/admin/coupons/${result.id}`);
      } else {
        const payload: CreateCouponPayload = {
          code: form.code.trim().toUpperCase(),
          packageId: Number(form.packageId),
          discountType: form.discountType,
          discountValue: Number(form.discountValue),
          expiresOn: form.expiresOn,
        };
        const result = await createCoupon(payload);
        router.push(`/admin/coupons/${result.id}`);
      }
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save coupon");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-2xl space-y-4">
      {error && <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}

      <label className="block">
        <span className="text-sm font-medium text-charcoal">Coupon code *</span>
        <input
          required
          disabled={Boolean(initial)}
          minLength={3}
          maxLength={40}
          value={form.code}
          onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
          placeholder="JOYWAY10"
          className="mt-1 w-full rounded-xl border border-border px-4 py-2.5 text-sm uppercase disabled:bg-sand/40"
        />
        <span className="mt-1 block text-xs text-charcoal-muted">
          Letters, numbers, hyphen, underscore. Code cannot change after create.
        </span>
      </label>

      <label className="block">
        <span className="text-sm font-medium text-charcoal">Package *</span>
        <select
          required
          value={form.packageId}
          onChange={(e) => setForm({ ...form, packageId: e.target.value })}
          className="mt-1 w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        >
          <option value="">Select package</option>
          {packages.map((pkg) => (
            <option key={pkg.id} value={pkg.id}>
              {pkg.title} · {pkg.destination}
            </option>
          ))}
        </select>
        <span className="mt-1 block text-xs text-charcoal-muted">
          Valid only for this package. One use per customer email or mobile.
        </span>
      </label>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="block">
          <span className="text-sm font-medium text-charcoal">Discount type *</span>
          <select
            required
            value={form.discountType}
            onChange={(e) =>
              setForm({
                ...form,
                discountType: e.target.value as "PERCENT" | "FIXED",
              })
            }
            className="mt-1 w-full rounded-xl border border-border px-4 py-2.5 text-sm"
          >
            <option value="PERCENT">Percent (%)</option>
            <option value="FIXED">Fixed amount (₹)</option>
          </select>
        </label>
        <label className="block">
          <span className="text-sm font-medium text-charcoal">Discount value *</span>
          <input
            required
            type="number"
            min={0.01}
            max={form.discountType === "PERCENT" ? 100 : undefined}
            step="0.01"
            value={form.discountValue}
            onChange={(e) => setForm({ ...form, discountValue: Number(e.target.value) })}
            className="mt-1 w-full rounded-xl border border-border px-4 py-2.5 text-sm"
          />
        </label>
      </div>

      <label className="block">
        <span className="text-sm font-medium text-charcoal">Expires on *</span>
        <input
          required
          type="date"
          min={kolkataToday()}
          value={form.expiresOn}
          onChange={(e) => setForm({ ...form, expiresOn: e.target.value })}
          className="mt-1 w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </label>

      {initial && (
        <label className="flex items-center gap-2 text-sm text-charcoal">
          <input
            type="checkbox"
            checked={form.active}
            onChange={(e) => setForm({ ...form, active: e.target.checked })}
          />
          Active (uncheck to disable without deleting)
        </label>
      )}

      <button
        type="submit"
        disabled={saving}
        className="rounded-xl bg-forest px-5 py-2.5 text-sm font-medium text-white disabled:opacity-50"
      >
        {saving ? "Saving..." : initial ? "Update coupon" : "Create coupon"}
      </button>
    </form>
  );
}
