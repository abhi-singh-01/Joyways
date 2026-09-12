import { ArrowRight } from "lucide-react";
import { PackageCard } from "@/components/packages/PackageCard";
import { InViewReveal } from "@/components/motion/InViewReveal";
import { EmptyState } from "@/components/ui/EmptyState";
import { ButtonLink } from "@/components/ui/ButtonLink";
import type { PackageSummary } from "@/lib/api/packagesApi";

type FeaturedTripsProps = {
  packages: PackageSummary[];
};

export function FeaturedTrips({ packages }: Readonly<FeaturedTripsProps>) {
  if (packages.length === 0) {
    return (
      <section className="py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <h2 className="text-center font-display text-3xl font-semibold text-charcoal">
            Featured Trips
          </h2>
          <EmptyState
            title="Trips coming soon"
            message="We're preparing new group journeys from Delhi. Check back shortly or contact us for early enquiries."
            actionLabel="Contact us"
            actionHref="/contact"
          />
        </div>
      </section>
    );
  }

  const featured = packages.slice(0, 6);

  return (
    <section className="py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-10 flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-forest">
              Popular now
            </p>
            <h2 className="mt-2 font-display text-3xl font-semibold text-charcoal sm:text-4xl">
              Featured Trips
            </h2>
            <p className="mt-2 text-charcoal-muted">
              Handpicked group journeys departing from Delhi
            </p>
          </div>
          <ButtonLink href="/trips" variant="outline" size="sm" className="hidden sm:inline-flex">
            View all trips
            <ArrowRight size={16} className="ml-2" />
          </ButtonLink>
        </div>

        <div className="-mx-4 flex gap-6 overflow-x-auto px-4 pb-4 snap-x snap-mandatory scrollbar-hide lg:mx-0 lg:grid lg:grid-cols-3 lg:overflow-visible lg:px-0 lg:pb-0">
          {featured.map((pkg, index) => (
            <div
              key={pkg.id}
              className="w-[min(85vw,20.5rem)] shrink-0 snap-center sm:w-[min(70vw,22.5rem)] lg:w-auto"
            >
              <PackageCard pkg={pkg} index={index} />
            </div>
          ))}
        </div>

        <InViewReveal className="mt-10 text-center sm:hidden">
          <ButtonLink href="/trips" variant="primary">
            View all trips
            <ArrowRight size={16} className="ml-2" />
          </ButtonLink>
        </InViewReveal>
      </div>
    </section>
  );
}
