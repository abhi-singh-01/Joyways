"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { getVehicle, updateVehicleStatus, type VehicleDetail } from "@/lib/api/adminVehiclesApi";
import { VehicleForm } from "@/components/admin/VehicleForm";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";

export default function AdminVehicleDetailPage() {
  const params = useParams<{ id: string }>();
  const vehicleId = Number(params.id);
  const [vehicle, setVehicle] = useState<VehicleDetail | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getVehicle(vehicleId)
      .then(setVehicle)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [vehicleId]);

  if (loading) return <LoadingState message="Loading vehicle..." fullPage />;
  if (error || !vehicle) return <ErrorState message={error || "Vehicle not found"} fullPage />;

  return (
    <div>
      <Link href="/admin/vehicles" className="text-sm text-forest hover:underline">
        ← Back to vehicles
      </Link>
      <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-3xl font-semibold text-charcoal">
            {vehicle.registrationNumber}
          </h1>
          <p className="mt-1 text-charcoal-muted">{vehicle.vehicleType}</p>
        </div>
        <StatusBadge status={vehicle.status} />
      </div>
      <div className="mt-8">
        <VehicleForm initial={vehicle} />
      </div>
      <div className="mt-8 flex flex-wrap gap-2">
        {vehicle.status !== "INACTIVE" && (
          <button
            type="button"
            onClick={() =>
              updateVehicleStatus(vehicle.id, "INACTIVE").then(setVehicle)
            }
            className="rounded-xl border border-border px-4 py-2 text-sm"
          >
            Deactivate
          </button>
        )}
        {vehicle.status === "INACTIVE" && (
          <button
            type="button"
            onClick={() =>
              updateVehicleStatus(vehicle.id, "AVAILABLE").then(setVehicle)
            }
            className="rounded-xl border border-border px-4 py-2 text-sm"
          >
            Activate
          </button>
        )}
      </div>
    </div>
  );
}
