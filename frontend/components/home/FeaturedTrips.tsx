"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import { PackageCard } from "@/components/packages/PackageCard";
import { EmptyState } from "@/components/ui/EmptyState";
import type { PackageSummary } from "@/lib/api/packagesApi";

type FeaturedTripsProps = {
  packages: PackageSummary[];
};

export function FeaturedTrips({ packages }: FeaturedTripsProps) {
  if (packages.length === 0) {
    return (
      <section className="py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <h2 className="text-center font-display text-3xl font-semibold text-charcoal">
            Featured Trips
          </h2>
          <EmptyState
            title="Trips coming soon"
            message="Connect the backend to see available journeys from Delhi."
            actionLabel="Contact us"
            actionHref="/contact"
          />
        </div>
      </section>
    );
  }

  return (
    <section className="py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-12 text-center">
          <h2 className="font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            Featured Trips
          </h2>
          <p className="mt-3 text-charcoal-muted">
            Handpicked group journeys departing from Delhi
          </p>
        </div>

        <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
          {packages.slice(0, 5).map((pkg, index) => (
            <PackageCard key={pkg.id} pkg={pkg} index={index} />
          ))}
        </div>

        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="mt-12 text-center"
        >
          <Link
            href="/trips"
            className="inline-flex items-center gap-2 text-sm font-medium text-forest hover:text-forest-light"
          >
            View all trips
            <ArrowRight size={16} />
          </Link>
        </motion.div>
      </div>
    </section>
  );
}
