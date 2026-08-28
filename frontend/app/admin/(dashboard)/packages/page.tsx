"use client";

import { useEffect, useState } from "react";
import {
  listAdminPackages,
  type AdminPackageSummary,
} from "@/lib/api/adminPackagesApi";
import { PackageListHeader, PackageTable } from "@/components/admin/PackageTable";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";

export default function AdminPackagesPage() {
  const [packages, setPackages] = useState<AdminPackageSummary[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listAdminPackages()
      .then(setPackages)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingState message="Loading packages..." fullPage />;
  if (error) return <ErrorState message={error} fullPage />;

  return (
    <div>
      <PackageListHeader />
      <PackageTable packages={packages} />
    </div>
  );
}
