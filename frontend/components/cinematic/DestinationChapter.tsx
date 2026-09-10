"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { TripSearchBar } from "@/components/home/TripSearchBar";
import { formatPrice } from "@/lib/format";
import { isBookablePrice } from "@/components/cinematic/journeyFromPackages";
import type { PackageSummary } from "@/lib/api/packagesApi";

type DestinationChapterProps = {
  destinations: string[];
  packages: PackageSummary[];
};

export function DestinationChapter({ destinations, packages }: Readonly<DestinationChapterProps>) {
  const [active, setActive] = useState("ALL");

  const visible = useMemo(() => {
    const priced = packages.filter((pkg) => isBookablePrice(pkg.pricePerPerson));
    if (active === "ALL") return priced;
    return priced.filter(
      (pkg) => pkg.destination.trim().toLowerCase() === active.toLowerCase()
    );
  }, [active, packages]);

  return (
    <section className="bg-off-white py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2 className="max-w-3xl font-display text-3xl font-semibold text-charcoal sm:text-5xl">
          Where will you go next?
        </h2>
        <p className="mt-4 max-w-xl text-charcoal-muted">
          Destinations below come from live JOYWAY trips departing from Delhi.
        </p>

        <div className="mt-8 max-w-xl">
          <TripSearchBar className="mt-0 flex w-full flex-col gap-3 rounded-2xl border border-border bg-white p-2 shadow-sm sm:flex-row sm:items-center" />
        </div>

        {destinations.length > 0 ? (
          <div className="mt-10 flex flex-wrap gap-2">
            {["ALL", ...destinations].map((label) => {
              const selected = active === label;
              return (
                <button
                  key={label}
                  type="button"
                  onClick={() => setActive(label)}
                  className={`rounded-full px-4 py-2 text-sm font-medium transition-colors ${
                    selected
                      ? "bg-forest text-white"
                      : "border border-border bg-white text-charcoal hover:border-forest/40"
                  }`}
                  aria-pressed={selected}
                >
                  {label}
                </button>
              );
            })}
          </div>
        ) : (
          <p className="mt-10 text-sm text-charcoal-muted">
            New journeys are being prepared. Browse all trips or contact us for early dates.
          </p>
        )}

        <div className="mt-10 space-y-4">
          {visible.slice(0, 6).map((pkg) => (
            <Link
              key={pkg.id}
              href={`/trips/${pkg.slug}`}
              className="group flex flex-col justify-between gap-3 border-b border-border py-5 sm:flex-row sm:items-end"
            >
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-forest">
                  {pkg.destination}
                </p>
                <h3 className="mt-1 font-display text-2xl font-semibold text-charcoal group-hover:text-forest">
                  {pkg.title}
                </h3>
                <p className="mt-1 text-sm text-charcoal-muted">{pkg.duration}</p>
              </div>
              <span className="inline-flex items-center gap-2 text-sm font-semibold text-forest">
                From {formatPrice(pkg.pricePerPerson)}
                <ArrowRight size={16} className="transition-transform group-hover:translate-x-0.5" />
              </span>
            </Link>
          ))}
          {visible.length === 0 && destinations.length > 0 && (
            <p className="text-sm text-charcoal-muted">
              No open trips for this destination right now.{" "}
              <Link href="/trips" className="font-semibold text-forest hover:text-forest-light">
                See all trips
              </Link>
            </p>
          )}
        </div>

        <Link
          href="/trips"
          className="mt-10 inline-flex items-center gap-2 text-sm font-semibold text-forest hover:text-forest-light"
        >
          See all trips
          <ArrowRight size={16} />
        </Link>
      </div>
    </section>
  );
}
