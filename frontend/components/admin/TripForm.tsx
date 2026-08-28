"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { listAdminPackages } from "@/lib/api/adminPackagesApi";
import { createTrip, type CreateTripPayload } from "@/lib/api/adminTripsApi";

export function TripForm() {
  const router = useRouter();
  const [packages, setPackages] = useState<{ id: number; label: string }[]>([]);
  const [form, setForm] = useState({
    packageId: "",
    travelDate: "",
    startTime: "08:00",
    endTime: "20:00",
    pickupLocation: "",
    dropLocation: "",
    notes: "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    listAdminPackages().then((items) =>
      setPackages(
        items.map((p) => ({
          id: p.id,
          label: `${p.title} · ${p.destination}`,
        }))
      )
    );
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");

    const startAt = new Date(`${form.travelDate}T${form.startTime}:00+05:30`).toISOString();
    const endAt = new Date(`${form.travelDate}T${form.endTime}:00+05:30`).toISOString();

    const payload: CreateTripPayload = {
      packageId: Number(form.packageId),
      travelDate: form.travelDate,
      startAt,
      endAt,
      pickupLocation: form.pickupLocation || undefined,
      dropLocation: form.dropLocation || undefined,
      notes: form.notes || undefined,
    };

    try {
      const trip = await createTrip(payload);
      router.push(`/admin/trips/${trip.id}`);
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create trip");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-2xl space-y-4">
      {error && <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
      <Field label="Package">
        <select
          required
          value={form.packageId}
          onChange={(e) => setForm({ ...form, packageId: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        >
          <option value="">Select package</option>
          {packages.map((pkg) => (
            <option key={pkg.id} value={pkg.id}>
              {pkg.label}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Travel Date">
        <input
          required
          type="date"
          value={form.travelDate}
          onChange={(e) => setForm({ ...form, travelDate: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Start Time">
          <input
            required
            type="time"
            value={form.startTime}
            onChange={(e) => setForm({ ...form, startTime: e.target.value })}
            className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
          />
        </Field>
        <Field label="End Time">
          <input
            required
            type="time"
            value={form.endTime}
            onChange={(e) => setForm({ ...form, endTime: e.target.value })}
            className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
          />
        </Field>
      </div>
      <Field label="Pickup Location">
        <input
          value={form.pickupLocation}
          onChange={(e) => setForm({ ...form, pickupLocation: e.target.value })}
          className="w-full rounded-xl border border-border px-4 py-2.5 text-sm"
        />
      </Field>
      <Field label="Drop Location">
        <input
          value={form.dropLocation}
          onChange={(e) => setForm({ ...form, dropLocation: e.target.value })}
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
        {saving ? "Creating..." : "Create Trip"}
      </button>
    </form>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-charcoal">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  );
}
