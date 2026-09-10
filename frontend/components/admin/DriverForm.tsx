"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  createDriver,
  updateDriver,
  type CreateDriverPayload,
  type DriverDetail,
} from "@/lib/api/adminDriversApi";

function driverSubmitLabel(saving: boolean, isUpdate: boolean): string {
  if (saving) return "Saving...";
  if (isUpdate) return "Update Driver";
  return "Create Driver";
}

type DriverFormProps = {
  initial?: DriverDetail;
};

export function DriverForm({ initial }: Readonly<DriverFormProps>) {
  const router = useRouter();
  const [form, setForm] = useState<CreateDriverPayload>({
    name: initial?.name ?? "",
    phone: initial?.phone ?? "",
    email: initial?.email ?? "",
    licenseNumber: initial?.licenseNumber ?? "",
    licenseExpiryDate: initial?.licenseExpiryDate ?? "",
    notes: initial?.notes ?? "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      const result = initial
        ? await updateDriver(initial.id, form)
        : await createDriver(form);
      router.push(`/admin/drivers/${result.id}`);
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save driver");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-2xl space-y-4">
      {error && <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
      <Field label="Name">
        <input
          required
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <Field label="Phone">
        <input
          required
          value={form.phone}
          onChange={(e) => setForm({ ...form, phone: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <Field label="Email">
        <input
          type="email"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <Field label="License Number">
        <input
          required
          value={form.licenseNumber}
          onChange={(e) => setForm({ ...form, licenseNumber: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <Field label="License Expiry Date">
        <input
          required
          type="date"
          value={form.licenseExpiryDate}
          onChange={(e) => setForm({ ...form, licenseExpiryDate: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <Field label="Notes">
        <textarea
          value={form.notes}
          onChange={(e) => setForm({ ...form, notes: e.target.value })}
          rows={3}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <button
        type="submit"
        disabled={saving}
        className="rounded-xl bg-forest px-5 py-2.5 text-sm font-medium text-white disabled:opacity-50"
      >
        {driverSubmitLabel(saving, Boolean(initial))}
      </button>
    </form>
  );
}

function Field({ label, children }: Readonly<{ label: string; children: React.ReactNode }>) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-charcoal">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  );
}
