"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { TripSearchBar } from "@/components/home/TripSearchBar";
import { TripShowcase } from "@/components/cinematic/TripShowcase";
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
    <>
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

          <Link
            href="/trips"
            className="mt-10 inline-flex items-center gap-2 text-sm font-semibold text-forest hover:text-forest-light"
          >
            See all trips
            <ArrowRight size={16} />
          </Link>
        </div>
      </section>
      <TripShowcase packages={visible} />
    </>
  );
}
