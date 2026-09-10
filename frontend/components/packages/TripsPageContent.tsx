"use client";

import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import { Search, SlidersHorizontal } from "lucide-react";
import { PackageCard } from "@/components/packages/PackageCard";
import { PUBLIC_EMAILS } from "@/lib/publicEmails";
import { EmptyState } from "@/components/ui/EmptyState";
import type { PackageSummary } from "@/lib/api/packagesApi";
import { cn } from "@/lib/utils";

type SortKey = "date" | "price-asc" | "price-desc" | "seats";

type TripsPageContentProps = {
  packages: PackageSummary[];
  initialQuery?: string;
};

export function TripsPageContent({ packages, initialQuery = "" }: Readonly<TripsPageContentProps>) {
  const [destination, setDestination] = useState<string | null>(null);
  const [query, setQuery] = useState(initialQuery);
  const [sort, setSort] = useState<SortKey>("date");

  const destinations = useMemo(() => {
    const unique = [...new Set(packages.map((p) => p.destination))];
    return unique.sort((a, b) => a.localeCompare(b));
  }, [packages]);

  const filtered = useMemo(() => {
    let list = packages;

    if (destination) {
      list = list.filter((p) => p.destination === destination);
    }

    const q = query.trim().toLowerCase();
    if (q) {
      list = list.filter(
        (p) =>
          p.destination.toLowerCase().includes(q) ||
          p.title.toLowerCase().includes(q) ||
          p.shortDescription.toLowerCase().includes(q)
      );
    }

    const sorted = [...list];
    switch (sort) {
      case "price-asc":
        sorted.sort((a, b) => a.pricePerPerson - b.pricePerPerson);
        break;
      case "price-desc":
        sorted.sort((a, b) => b.pricePerPerson - a.pricePerPerson);
        break;
      case "seats":
        sorted.sort((a, b) => b.availableSeats - a.availableSeats);
        break;
      case "date":
      default:
        sorted.sort(
          (a, b) =>
            new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
        );
    }

    return sorted;
  }, [packages, destination, query, sort]);

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
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-8 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"
      >
        <div className="relative max-w-md flex-1">
          <Search
            size={18}
            className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-charcoal-muted"
          />
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by destination or keyword…"
            className="w-full rounded-full border border-border bg-white py-3 pl-11 pr-4 text-sm outline-none ring-forest/20 transition-shadow focus:ring-2"
            aria-label="Search trips"
          />
        </div>

        <div className="flex items-center gap-2">
          <SlidersHorizontal size={16} className="text-charcoal-muted" aria-hidden />
          <label htmlFor="trip-sort" className="sr-only">
            Sort trips
          </label>
          <select
            id="trip-sort"
            value={sort}
            onChange={(e) => setSort(e.target.value as SortKey)}
            className="rounded-full border border-border bg-white px-4 py-2.5 text-sm text-charcoal outline-none focus:ring-2 focus:ring-forest/20"
          >
            <option value="date">Soonest departure</option>
            <option value="price-asc">Price: low to high</option>
            <option value="price-desc">Price: high to low</option>
            <option value="seats">Most seats available</option>
          </select>
        </div>
      </motion.div>

      {destinations.length > 1 && (
        <div className="mb-10 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setDestination(null)}
            className={cn(
              "rounded-full px-4 py-2 text-sm font-medium transition-all",
              destination === null
                ? "bg-forest text-white shadow-sm"
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
                "rounded-full px-4 py-2 text-sm font-medium transition-all",
                destination === dest
                  ? "bg-forest text-white shadow-sm"
                  : "bg-white text-charcoal-muted ring-1 ring-border hover:text-charcoal"
              )}
            >
              {dest}
            </button>
          ))}
        </div>
      )}

      {filtered.length === 0 ? (
        <div className="rounded-2xl border border-border bg-surface/50 py-16 text-center">
          <p className="font-display text-xl font-semibold text-charcoal">
            No trips match your search
          </p>
          <p className="mt-2 text-charcoal-muted">
            Try another destination or clear your filters.
          </p>
          <button
            type="button"
            onClick={() => {
              setQuery("");
              setDestination(null);
            }}
            className="mt-6 rounded-full bg-forest px-6 py-2.5 text-sm font-medium text-white hover:bg-forest-light"
          >
            Clear filters
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
