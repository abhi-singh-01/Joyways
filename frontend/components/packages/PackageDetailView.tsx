import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import {
  Bus,
  Calendar,
  Check,
  Hotel,
  MapPin,
  Users,
  X,
  AlertCircle,
  HelpCircle,
} from "lucide-react";
import { PackageGallery } from "@/components/packages/PackageGallery";
import { PackageItinerary } from "@/components/packages/PackageItinerary";
import { StickyBookBar } from "@/components/packages/StickyBookBar";
import { Button } from "@/components/ui/Button";
import {
  formatDateRange,
  formatPrice,
  parsePickupPoints,
  splitLines,
} from "@/lib/format";
import type { PackageDetail, PackageReviews } from "@/lib/api/packagesApi";
import { WaitlistForm } from "@/components/packages/WaitlistForm";
import { PackageReviewsSection } from "@/components/packages/PackageReviewsSection";

type PackageDetailViewProps = {
  pkg: PackageDetail;
  reviews: PackageReviews;
};

function InfoBlock({
  icon: Icon,
  title,
  children,
}: Readonly<{
  icon: LucideIcon;
  title: string;
  children: React.ReactNode;
}>) {
  return (
    <div className="rounded-2xl border border-border bg-white p-6">
      <div className="mb-4 flex items-center gap-2">
        <Icon size={20} className="text-forest" />
        <h3 className="font-display text-lg font-semibold text-charcoal">{title}</h3>
      </div>
      {children}
    </div>
  );
}

function BulletList({
  items,
  variant,
}: Readonly<{
  items: string[];
  variant: "check" | "cross";
}>) {
  const Icon = variant === "check" ? Check : X;
  const iconClass = variant === "check" ? "text-forest" : "text-charcoal-muted";

  return (
    <ul className="space-y-2">
      {items.map((item) => (
        <li key={item} className="flex gap-2 text-sm leading-relaxed text-charcoal-muted">
          <Icon size={16} className={cnIcon(iconClass)} />
          <span>{item}</span>
        </li>
      ))}
    </ul>
  );
}

function cnIcon(className: string) {
  return `mt-0.5 shrink-0 ${className}`;
}

const faqs = [
  {
    q: "What should I bring?",
    a: "Valid government ID, comfortable clothing, personal medications, and essentials for the trip duration.",
  },
  {
    q: "Is food included?",
    a: "Check the inclusions section above. Meals not listed are at your own expense.",
  },
  {
    q: "Can I cancel my booking?",
    a: "Yes — refer to the cancellation policy below for refund timelines and conditions.",
  },
];

function unavailabilityReasonFor(pkg: PackageDetail): string | null {
  if (pkg.bookable) return null;
  if (pkg.waitlistable) {
    return "This trip is full. Join the waitlist and we will email you if a seat opens.";
  }
  if (pkg.availableSeats === 0) {
    return "No seats are available for this trip.";
  }
  const today = new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
  if (pkg.startDate && pkg.startDate < today) {
    return "This trip date has passed. Update the package dates or create a new departure in Admin → Trips.";
  }
  if (pkg.status && pkg.status !== "PUBLISHED") {
    return "This trip is not published yet.";
  }
  return "This trip is not available for booking.";
}

function bookingAction(pkg: PackageDetail) {
  if (pkg.bookable) {
    return (
      <Link href={`/book/${pkg.id}`}>
        <Button size="lg" className="w-full">
          Book Now
        </Button>
      </Link>
    );
  }
  if (pkg.waitlistable) {
    return (
      <a href="#waitlist">
        <Button size="lg" className="w-full">
          Join waitlist
        </Button>
      </a>
    );
  }
  return (
    <Button size="lg" className="w-full" disabled>
      Not Available
    </Button>
  );
}

export function PackageDetailView({ pkg, reviews }: Readonly<PackageDetailViewProps>) {
  const inclusions = splitLines(pkg.inclusions);
  const exclusions = splitLines(pkg.exclusions);
  const pickupPoints = parsePickupPoints(pkg.pickupPoints);

  const unavailabilityReason = unavailabilityReasonFor(pkg);

  return (
    <>
      {/* Hero header */}
      <section className="border-b border-border bg-white">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 sm:py-14 lg:px-8">
          <Link
            href="/trips"
            className="text-sm font-medium text-forest hover:text-forest-light"
          >
            ← All trips
          </Link>
          <p className="mt-4 text-sm font-semibold uppercase tracking-widest text-forest">
            {pkg.departureCity} → {pkg.destination}
          </p>
          <h1 className="mt-2 font-display text-3xl font-semibold leading-tight text-charcoal sm:text-4xl lg:text-5xl">
            {pkg.title}
          </h1>
          <p className="mt-3 max-w-2xl text-lg text-charcoal-muted">
            {pkg.shortDescription}
          </p>

          <div className="mt-6 flex flex-wrap gap-4 text-sm text-charcoal-muted">
            <span className="flex items-center gap-1.5 rounded-full bg-surface px-3 py-1.5">
              <Calendar size={15} className="text-forest" />
              {formatDateRange(pkg.startDate, pkg.endDate)}
            </span>
            <span className="flex items-center gap-1.5 rounded-full bg-surface px-3 py-1.5">
              <Users size={15} className="text-forest" />
              {pkg.availableSeats} of {pkg.totalSeats} seats left
            </span>
            <span className="flex items-center gap-1.5 rounded-full bg-surface px-3 py-1.5">
              <MapPin size={15} className="text-forest" />
              {pkg.duration}
            </span>
          </div>
        </div>
      </section>

      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="grid gap-10 lg:grid-cols-3 lg:gap-12">
          {/* Main content */}
          <div className="space-y-10 lg:col-span-2">
            {pkg.waitlistable && (
              <WaitlistForm slug={pkg.slug} destination={pkg.destination} />
            )}

            <PackageGallery images={pkg.images} destination={pkg.destination} />

            {pkg.fullDescription && (
              <section>
                <h2 className="font-display text-2xl font-semibold text-charcoal">
                  About this trip
                </h2>
                <p className="mt-4 whitespace-pre-line leading-relaxed text-charcoal-muted">
                  {pkg.fullDescription}
                </p>
              </section>
            )}

            {pkg.itinerary && (
              <section>
                <h2 className="font-display text-2xl font-semibold text-charcoal">
                  Itinerary
                </h2>
                <div className="mt-6 rounded-2xl border border-border bg-white p-6">
                  <PackageItinerary itinerary={pkg.itinerary} />
                </div>
              </section>
            )}

            <div className="grid gap-6 sm:grid-cols-2">
              {pkg.hotelName && (
                <InfoBlock icon={Hotel} title="Accommodation">
                  <p className="font-medium text-charcoal">{pkg.hotelName}</p>
                  {pkg.hotelDetails && (
                    <p className="mt-2 text-sm leading-relaxed text-charcoal-muted">
                      {pkg.hotelDetails}
                    </p>
                  )}
                </InfoBlock>
              )}
              {pkg.vehicleType && (
                <InfoBlock icon={Bus} title="Transport">
                  <p className="text-sm leading-relaxed text-charcoal-muted">
                    {pkg.vehicleType} from {pkg.departureCity}
                  </p>
                  {pickupPoints.length > 0 && (
                    <div className="mt-3">
                      <p className="text-xs font-semibold uppercase tracking-wide text-charcoal-muted">
                        Pickup points
                      </p>
                      <ul className="mt-2 space-y-1">
                        {pickupPoints.map((point) => (
                          <li
                            key={point}
                            className="flex items-center gap-1.5 text-sm text-charcoal-muted"
                          >
                            <MapPin size={12} className="shrink-0 text-forest" />
                            {point.trim()}
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </InfoBlock>
              )}
            </div>

            <div className="grid gap-6 sm:grid-cols-2">
              {inclusions.length > 0 && (
                <InfoBlock icon={Check} title="Inclusions">
                  <BulletList items={inclusions} variant="check" />
                </InfoBlock>
              )}
              {exclusions.length > 0 && (
                <InfoBlock icon={X} title="Exclusions">
                  <BulletList items={exclusions} variant="cross" />
                </InfoBlock>
              )}
            </div>

            {pkg.cancellationPolicy && (
              <InfoBlock icon={AlertCircle} title="Cancellation Policy">
                <p className="whitespace-pre-line text-sm leading-relaxed text-charcoal-muted">
                  {pkg.cancellationPolicy}
                </p>
              </InfoBlock>
            )}

            <InfoBlock icon={HelpCircle} title="FAQ">
              <div className="space-y-4">
                {faqs.map((faq) => (
                  <div key={faq.q}>
                    <p className="font-medium text-charcoal">{faq.q}</p>
                    <p className="mt-1 text-sm text-charcoal-muted">{faq.a}</p>
                  </div>
                ))}
              </div>
            </InfoBlock>

            <PackageReviewsSection reviews={reviews} />
          </div>

          {/* Sidebar booking card — desktop */}
          <div className="hidden lg:block">
            <div className="sticky top-24">
              <div className="rounded-2xl border border-border bg-white p-6 shadow-lg">
                <p className="text-sm text-charcoal-muted">Price per person</p>
                <p className="mt-1 font-display text-3xl font-semibold text-forest">
                  {formatPrice(pkg.pricePerPerson)}
                </p>

                <div className="mt-4 space-y-2 border-t border-border pt-4 text-sm">
                  <div className="flex justify-between">
                    <span className="text-charcoal-muted">Travel date</span>
                    <span className="font-medium text-charcoal">
                      {formatDateRange(pkg.startDate, pkg.endDate)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-charcoal-muted">Seats available</span>
                    <span className="font-medium text-charcoal">
                      {pkg.availableSeats}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-charcoal-muted">Departure</span>
                    <span className="font-medium text-charcoal">
                      {pkg.departureCity}
                    </span>
                  </div>
                </div>

                {unavailabilityReason && (
                  <p className="mt-4 rounded-lg bg-surface px-3 py-2 text-sm text-charcoal-muted">
                    {unavailabilityReason}
                  </p>
                )}

                <div className="mt-6">{bookingAction(pkg)}</div>

                <p className="mt-4 text-center text-xs text-charcoal-muted">
                  Secure online booking · Instant confirmation
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Mobile sticky bar + bottom padding */}
      <div className="pb-24 lg:pb-0">
        <StickyBookBar
          pricePerPerson={pkg.pricePerPerson}
          bookable={pkg.bookable}
          waitlistable={pkg.waitlistable}
          packageId={pkg.id}
          destination={pkg.destination}
        />
      </div>
    </>
  );
}
