"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  createVehicle,
  updateVehicle,
  type CreateVehiclePayload,
  type VehicleDetail,
  type VehicleOwnershipType,
} from "@/lib/api/adminVehiclesApi";

function vehicleSubmitLabel(saving: boolean, isUpdate: boolean): string {
  if (saving) return "Saving...";
  if (isUpdate) return "Update Vehicle";
  return "Create Vehicle";
}

type VehicleFormProps = {
  initial?: VehicleDetail;
};

export function VehicleForm({ initial }: Readonly<VehicleFormProps>) {
  const router = useRouter();
  const [form, setForm] = useState<CreateVehiclePayload>({
    registrationNumber: initial?.registrationNumber ?? "",
    vehicleType: initial?.vehicleType ?? "",
    brandModel: initial?.brandModel ?? "",
    seatingCapacity: initial?.seatingCapacity ?? 17,
    ownershipType: (initial?.ownershipType ?? "OWNED") as VehicleOwnershipType,
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
        ? await updateVehicle(initial.id, form)
        : await createVehicle(form);
      router.push(`/admin/vehicles/${result.id}`);
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save vehicle");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-2xl space-y-4">
      {error && <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
      <Field label="Registration Number">
        <input
          required
          value={form.registrationNumber}
          onChange={(e) => setForm({ ...form, registrationNumber: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <Field label="Vehicle Type">
        <input
          required
          value={form.vehicleType}
          onChange={(e) => setForm({ ...form, vehicleType: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <Field label="Brand / Model">
        <input
          value={form.brandModel}
          onChange={(e) => setForm({ ...form, brandModel: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <Field label="Seating Capacity">
        <input
          required
          type="number"
          min={1}
          value={form.seatingCapacity}
          onChange={(e) =>
            setForm({ ...form, seatingCapacity: Number(e.target.value) })
          }
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <Field label="Ownership">
        <select
          value={form.ownershipType}
          onChange={(e) =>
            setForm({
              ...form,
              ownershipType: e.target.value as VehicleOwnershipType,
            })
          }
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        >
          <option value="OWNED">Owned</option>
          <option value="RENTED">Rented</option>
          <option value="PARTNER">Partner</option>
        </select>
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
        {vehicleSubmitLabel(saving, Boolean(initial))}
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
