"use client";

import Link from "next/link";
import { ArrowRight, MapPin } from "lucide-react";
import { InViewReveal } from "@/components/motion/InViewReveal";

const destinations = [
  { name: "Nainital", emoji: "🏔️", slug: "nainital" },
  { name: "Mussoorie", emoji: "⛰️", slug: "mussoorie" },
  { name: "Rishikesh", emoji: "🌊", slug: "rishikesh" },
  { name: "Haridwar", emoji: "🪔", slug: "haridwar" },
  { name: "Dalhousie", emoji: "❄️", slug: "dalhousie" },
  { name: "Manali", emoji: "🌲", slug: "manali" },
  { name: "Shimla", emoji: "🚂", slug: "shimla" },
] as const;

function tripsHref(name: string) {
  return `/trips?q=${encodeURIComponent(name)}`;
}

export function DestinationSection() {
  return (
    <section className="overflow-hidden bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <InViewReveal y={16} duration={0.55} className="mb-12 text-center">
          <h2 className="font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            Where will you go next?
          </h2>
          <p className="mt-3 text-charcoal-muted">
            Explore India&apos;s most loved hill stations — all departing from Delhi
          </p>
        </InViewReveal>

        <InViewReveal y={24} duration={0.65} className="mb-10">
          <div className="rounded-3xl border border-border bg-white p-5 shadow-sm sm:p-8">
            <p className="mb-6 text-center text-xs font-semibold uppercase tracking-[0.18em] text-charcoal-muted">
              Click a destination to see trips
            </p>

            <div className="mb-8 flex justify-center">
              <div className="inline-flex items-center gap-2 rounded-full border border-forest/20 bg-forest/5 px-4 py-2 text-sm font-semibold text-forest">
                <MapPin size={16} aria-hidden />
                Departing from Delhi
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
              {destinations.map((dest) => (
                <Link
                  key={dest.slug}
                  href={tripsHref(dest.name)}
                  className="group flex min-h-28 flex-col items-center justify-center rounded-2xl border border-border bg-surface/60 px-3 py-5 text-center transition-all hover:-translate-y-0.5 hover:border-forest/30 hover:bg-white hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-forest/40"
                >
                  <span className="text-2xl" aria-hidden>
                    {dest.emoji}
                  </span>
                  <span className="mt-2 font-semibold text-charcoal">{dest.name}</span>
                  <span className="mt-1 inline-flex items-center gap-1 text-xs font-medium text-forest opacity-80 group-hover:opacity-100">
                    View trips
                    <ArrowRight
                      size={12}
                      className="transition-transform group-hover:translate-x-0.5"
                    />
                  </span>
                </Link>
              ))}
            </div>
          </div>
        </InViewReveal>

        <div className="text-center">
          <Link
            href="/trips"
            className="inline-flex items-center gap-2 rounded-full bg-forest px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-forest-light"
          >
            See all trips
            <ArrowRight size={16} />
          </Link>
        </div>
      </div>
    </section>
  );
}
