"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { Calendar, MapPin, Users, ArrowRight, Bus } from "lucide-react";
import { formatDateRange, formatPrice } from "@/lib/format";
import type { PackageSummary } from "@/lib/api/packagesApi";
import { cn } from "@/lib/utils";
import { PackagePhoto } from "@/components/packages/PackagePhoto";

type PackageCardProps = {
  pkg: PackageSummary;
  index?: number;
  className?: string;
  variant?: "default" | "compact";
};

export function PackageCard({
  pkg,
  index = 0,
  className,
  variant = "default",
}: PackageCardProps) {
  const soldOut = pkg.availableSeats <= 0;
  const lowSeats = pkg.availableSeats <= 5 && pkg.availableSeats > 0;

  return (
    <motion.article
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-60px 0px" }}
      transition={{ duration: 0.45, delay: index * 0.07, ease: "easeOut" }}
      whileHover={soldOut ? undefined : { y: -6 }}
      className={cn(
        "group overflow-hidden rounded-2xl border border-border bg-white shadow-sm transition-shadow hover:shadow-xl",
        soldOut && "opacity-90",
        className
      )}
    >
      <Link href={`/trips/${pkg.slug}`} className="block">
        <div className="relative aspect-[4/3] overflow-hidden">
          {pkg.heroImageUrl ? (
            <PackagePhoto
              src={pkg.heroImageUrl}
              alt={pkg.heroImageAlt || `${pkg.destination} trip`}
              className={cn(
                "transition-transform duration-700 ease-out group-hover:scale-105",
                soldOut && "grayscale-[0.35]"
              )}
              sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
            />
          ) : (
            <div className="flex h-full items-center justify-center bg-surface">
              <MapPin className="h-10 w-10 text-forest/30" />
            </div>
          )}
          <div className="absolute inset-0 bg-gradient-to-t from-charcoal/75 via-charcoal/15 to-transparent" />
          <div className="absolute left-4 top-4 flex flex-wrap gap-2">
            <span className="rounded-full bg-white/95 px-3 py-1 text-xs font-semibold text-forest backdrop-blur-sm">
              {pkg.duration}
            </span>
            <span className="flex items-center gap-1 rounded-full bg-forest/90 px-3 py-1 text-xs font-semibold text-white backdrop-blur-sm">
              <Bus size={12} />
              Group trip
            </span>
            {soldOut ? (
              <span className="rounded-full bg-charcoal/90 px-3 py-1 text-xs font-semibold text-white backdrop-blur-sm">
                Sold out
              </span>
            ) : lowSeats ? (
              <span className="rounded-full bg-accent/95 px-3 py-1 text-xs font-semibold text-charcoal backdrop-blur-sm">
                {pkg.availableSeats} seats left
              </span>
            ) : null}
          </div>
          <div className="absolute bottom-4 left-4 right-4">
            <p className="text-xs font-medium uppercase tracking-wider text-white/80">
              From {pkg.departureCity}
            </p>
            <h3 className="font-display text-2xl font-semibold text-white">
              {pkg.destination}
            </h3>
          </div>
        </div>

        <div className={cn("p-5", variant === "compact" && "p-4")}>
          <p className="line-clamp-2 text-sm leading-relaxed text-charcoal-muted">
            {pkg.shortDescription}
          </p>

          <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-charcoal-muted">
            <span className="flex items-center gap-1.5">
              <Calendar size={14} className="shrink-0 text-forest" />
              {formatDateRange(pkg.startDate, pkg.endDate)}
            </span>
            <span className="flex items-center gap-1.5">
              <Users size={14} className="shrink-0 text-forest" />
              {soldOut ? "Waitlist soon" : `${pkg.availableSeats} seats`}
            </span>
          </div>

          <div className="mt-4 flex items-end justify-between border-t border-border pt-4">
            <div>
              <p className="text-xs text-charcoal-muted">Per person</p>
              <p className="text-xl font-semibold text-forest">
                {formatPrice(pkg.pricePerPerson)}
              </p>
            </div>
            <span className="flex items-center gap-1 text-sm font-semibold text-forest transition-colors group-hover:text-forest-light">
              {soldOut ? "View details" : "View Trip"}
              <ArrowRight
                size={16}
                className="transition-transform group-hover:translate-x-0.5"
              />
            </span>
          </div>
        </div>
      </Link>
    </motion.article>
  );
}
