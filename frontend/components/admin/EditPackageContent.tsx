"use client";

import { useEffect, useState } from "react";
import { getAdminPackage, type AdminPackageDetail } from "@/lib/api/adminPackagesApi";
import { PackageForm } from "@/components/admin/PackageForm";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";

export function EditPackageContent({ id }: Readonly<{ id: number }>) {
  const [pkg, setPkg] = useState<AdminPackageDetail | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    getAdminPackage(id)
      .then((data) => {
        if (!cancelled) setPkg(data);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to load");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  if (loading) return <LoadingState message="Loading package..." fullPage />;
  if (error) return <ErrorState message={error} fullPage />;
  if (!pkg) return null;

  return (
    <div>
      <h1 className="mb-6 font-display text-2xl font-semibold text-charcoal">
        Edit Package
      </h1>
      <PackageForm mode="edit" initial={pkg} />
    </div>
  );
}
