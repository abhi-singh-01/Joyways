"use client";

import { useSearchParams } from "next/navigation";
import { TripsPageContent } from "@/components/packages/TripsPageContent";
import type { PackageSummary } from "@/lib/api/packagesApi";

type TripsPageClientProps = {
  packages: PackageSummary[];
};

export function TripsPageClient({ packages }: Readonly<TripsPageClientProps>) {
  const searchParams = useSearchParams();
  const initialQuery = searchParams.get("q") ?? "";

  return (
    <TripsPageContent key={initialQuery} packages={packages} initialQuery={initialQuery} />
  );
}
