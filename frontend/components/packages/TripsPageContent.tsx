"use client";

import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import { PackageCard } from "@/components/packages/PackageCard";
import { PUBLIC_EMAILS } from "@/lib/publicEmails";
import { EmptyState } from "@/components/ui/EmptyState";
import type { PackageSummary } from "@/lib/api/packagesApi";
import { cn } from "@/lib/utils";

type TripsPageContentProps = {
  packages: PackageSummary[];
};

export function TripsPageContent({ packages }: TripsPageContentProps) {
  const [destination, setDestination] = useState<string | null>(null);

  const destinations = useMemo(() => {
    const unique = [...new Set(packages.map((p) => p.destination))];
    return unique.sort();
  }, [packages]);

  const filtered = useMemo(() => {
    if (!destination) return packages;
    return packages.filter((p) => p.destination === destination);
  }, [packages, destination]);

  if (packages.length === 0) {
    return (
      <EmptyState
        title="No trips available right now"
        message={`We're preparing new journeys from Delhi. Please check back soon or contact us at ${PUBLIC_EMAILS.support}`}
        actionLabel="Contact us"
        actionHref="/contact"
      />
    );
  }

  return (
    <>
      {/* Destination filter chips */}
      {destinations.length > 1 && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="mb-10 flex flex-wrap gap-2"
        >
          <button
            type="button"
            onClick={() => setDestination(null)}
            className={cn(
              "rounded-full px-4 py-2 text-sm font-medium transition-colors",
              destination === null
                ? "bg-forest text-white"
                : "bg-white text-charcoal-muted ring-1 ring-border hover:text-charcoal"
            )}
          >
            All destinations
          </button>
          {destinations.map((dest) => (
            <button
              key={dest}
              type="button"
              onClick={() => setDestination(dest)}
              className={cn(
                "rounded-full px-4 py-2 text-sm font-medium transition-colors",
                destination === dest
                  ? "bg-forest text-white"
                  : "bg-white text-charcoal-muted ring-1 ring-border hover:text-charcoal"
              )}
            >
              {dest}
            </button>
          ))}
        </motion.div>
      )}

      {filtered.length === 0 ? (
        <div className="py-12 text-center">
          <p className="font-display text-xl font-semibold text-charcoal">
            No trips to {destination}
          </p>
          <p className="mt-2 text-charcoal-muted">
            Try selecting a different destination.
          </p>
          <button
            type="button"
            onClick={() => setDestination(null)}
            className="mt-6 rounded-full bg-forest px-6 py-2.5 text-sm font-medium text-white hover:bg-forest-light"
          >
            View all trips
          </button>
        </div>
      ) : (
        <>
          <p className="mb-6 text-sm text-charcoal-muted">
            {filtered.length} trip{filtered.length !== 1 ? "s" : ""} available
          </p>
          <div className="grid gap-8 sm:grid-cols-2 xl:grid-cols-3">
            {filtered.map((pkg, index) => (
              <PackageCard key={pkg.id} pkg={pkg} index={index} />
            ))}
          </div>
        </>
      )}
    </>
  );
}
