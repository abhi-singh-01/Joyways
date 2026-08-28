"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { getDriver, updateDriverStatus, type DriverDetail } from "@/lib/api/adminDriversApi";
import { DriverForm } from "@/components/admin/DriverForm";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";

export default function AdminDriverDetailPage() {
  const params = useParams<{ id: string }>();
  const driverId = Number(params.id);
  const [driver, setDriver] = useState<DriverDetail | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getDriver(driverId)
      .then(setDriver)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [driverId]);

  if (loading) return <LoadingState message="Loading driver..." fullPage />;
  if (error || !driver) return <ErrorState message={error || "Driver not found"} fullPage />;

  return (
    <div>
      <Link href="/admin/drivers" className="text-sm text-forest hover:underline">
        ← Back to drivers
      </Link>
      <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-3xl font-semibold text-charcoal">{driver.name}</h1>
          <p className="mt-1 text-charcoal-muted">{driver.phone}</p>
        </div>
        <div className="flex gap-2">
          <StatusBadge status={driver.status} />
          <StatusBadge status={driver.licenseStatus} />
        </div>
      </div>
      <div className="mt-8">
        <DriverForm initial={driver} />
      </div>
      <div className="mt-8 flex flex-wrap gap-2">
        {driver.status !== "INACTIVE" && (
          <button
            type="button"
            onClick={() => updateDriverStatus(driver.id, "INACTIVE").then(setDriver)}
            className="rounded-xl border border-border px-4 py-2 text-sm"
          >
            Deactivate
          </button>
        )}
        {driver.status === "INACTIVE" && (
          <button
            type="button"
            onClick={() => updateDriverStatus(driver.id, "AVAILABLE").then(setDriver)}
            className="rounded-xl border border-border px-4 py-2 text-sm"
          >
            Activate
          </button>
        )}
      </div>
    </div>
  );
}
