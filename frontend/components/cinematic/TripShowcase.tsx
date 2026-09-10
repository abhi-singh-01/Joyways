import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { PackagePhoto } from "@/components/packages/PackagePhoto";
import { bookableTrips } from "@/components/cinematic/journeyFromPackages";
import { formatDateRange, formatPrice } from "@/lib/format";
import type { PackageSummary } from "@/lib/api/packagesApi";

type TripShowcaseProps = {
  packages: PackageSummary[];
};

export function TripShowcase({ packages }: Readonly<TripShowcaseProps>) {
  const trips = bookableTrips(packages).slice(0, 6);

  return (
    <section className="bg-charcoal text-white">
      <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6 sm:py-28 lg:px-8">
        <h2 className="max-w-3xl font-display text-3xl font-semibold sm:text-5xl">
          Curated for the journey.
        </h2>
        <p className="mt-4 max-w-xl text-white/70">
          Live departures from Delhi. Prices and dates come from current JOYWAY trips.
        </p>
      </div>

      {trips.length === 0 ? (
        <div className="mx-auto max-w-7xl px-4 pb-24 sm:px-6 lg:px-8">
          <p className="max-w-xl text-white/75">
            New journeys are being prepared. Browse upcoming dates or write to us if you
            want to be notified.
          </p>
          <div className="mt-6 flex flex-wrap gap-4">
            <Link href="/trips" className="text-sm font-semibold text-accent-light hover:text-white">
              Browse trips
            </Link>
            <Link href="/contact" className="text-sm font-semibold text-accent-light hover:text-white">
              Contact JOYWAY
            </Link>
          </div>
        </div>
      ) : (
        <div>
          {trips.map((pkg, index) => (
            <article
              key={pkg.id}
              className="border-t border-white/10"
            >
              <div className="mx-auto grid max-w-7xl items-stretch gap-8 px-4 py-10 sm:px-6 lg:grid-cols-12 lg:gap-12 lg:px-8 lg:py-16">
                <div className="lg:col-span-7">
                  <div className="relative aspect-[16/10] overflow-hidden bg-forest-dark">
                    <PackagePhoto
                      src={pkg.heroImageUrl || "/images/mountains-hero.webp"}
                      alt={pkg.heroImageAlt || pkg.title}
                      sizes="(max-width: 1024px) 100vw, 58vw"
                    />
                  </div>
                </div>
                <div className="flex flex-col justify-end lg:col-span-5 lg:py-6">
                  <p className="font-display text-5xl font-semibold text-white/20">
                    {String(index + 1).padStart(2, "0")}
                  </p>
                  <p className="mt-4 text-xs font-semibold uppercase tracking-[0.22em] text-accent-light">
                    {pkg.destination}
                  </p>
                  <h3 className="mt-3 font-display text-3xl font-semibold sm:text-4xl">
                    {pkg.title}
                  </h3>
                  <dl className="mt-6 space-y-2 text-sm text-white/75">
                    <div className="flex justify-between gap-4 border-b border-white/10 py-2">
                      <dt>Duration</dt>
                      <dd className="text-white">{pkg.duration}</dd>
                    </div>
                    <div className="flex justify-between gap-4 border-b border-white/10 py-2">
                      <dt>Dates</dt>
                      <dd className="text-white">{formatDateRange(pkg.startDate, pkg.endDate)}</dd>
                    </div>
                    <div className="flex justify-between gap-4 py-2">
                      <dt>From</dt>
                      <dd className="text-white">{formatPrice(pkg.pricePerPerson)}</dd>
                    </div>
                  </dl>
                  <Link
                    href={`/trips/${pkg.slug}`}
                    className="mt-8 inline-flex items-center gap-2 text-sm font-semibold text-accent-light hover:text-white"
                  >
                    Explore journey
                    <ArrowRight size={16} />
                  </Link>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
