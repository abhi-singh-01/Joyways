"use client";

import dynamic from "next/dynamic";
import Link from "next/link";
import { motion } from "framer-motion";
import { ArrowRight, Bus, MapPin, Shield, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { FeaturedTrips } from "@/components/home/FeaturedTrips";
import { DestinationSection } from "@/components/home/DestinationSection";
import { HowItWorks } from "@/components/home/HowItWorks";
import { WhyJoyway } from "@/components/home/WhyJoyway";
import { BrandSection } from "@/components/home/BrandSection";
import { JoywayLogo } from "@/components/layout/JoywayLogo";
import type { PackageSummary } from "@/lib/api/packagesApi";

const HeroMountains = dynamic(
  () => import("@/components/home/HeroMountains").then((m) => m.HeroMountains),
  {
    ssr: false,
    loading: () => (
      <div className="absolute inset-0 bg-gradient-to-b from-forest/5 to-transparent" />
    ),
  }
);

const highlights = [
  { icon: MapPin, label: "Delhi Departures" },
  { icon: Sparkles, label: "Handpicked Destinations" },
  { icon: Bus, label: "Transport + Stay Packages" },
  { icon: Shield, label: "Secure Online Booking" },
];

type HomePageProps = {
  packages: PackageSummary[];
};

export function HomePage({ packages }: HomePageProps) {
  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10">
          <HeroMountains />
          <div className="absolute inset-0 bg-gradient-to-b from-off-white/40 via-off-white/80 to-off-white" />
        </div>

        <div className="mx-auto max-w-7xl px-4 pb-20 pt-24 sm:px-6 sm:pb-28 sm:pt-32 lg:px-8">
          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: "easeOut" }}
            className="mx-auto max-w-3xl text-center"
          >
            <p className="mb-4 text-sm font-semibold uppercase tracking-[0.2em] text-forest">
              Group Travel from Delhi
            </p>
            <div className="flex justify-center">
              <JoywayLogo href={null} height={80} priority />
            </div>
            <p className="mt-4 font-display text-xl text-forest sm:text-2xl">
              Find Joy in Every Journey.
            </p>
            <p className="mx-auto mt-6 max-w-xl text-lg leading-relaxed text-charcoal-muted">
              Discover beautiful destinations, join unforgettable group trips, and
              let us take care of the journey.
            </p>
            <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
              <Link href="/trips">
                <Button size="lg" className="group min-w-[200px]">
                  Explore Trips
                  <ArrowRight
                    size={18}
                    className="ml-2 transition-transform group-hover:translate-x-1"
                  />
                </Button>
              </Link>
              <Link href="/trips">
                <Button variant="outline" size="lg" className="min-w-[200px]">
                  Book Your Journey
                </Button>
              </Link>
            </div>
          </motion.div>

          {/* Highlights */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.3 }}
            className="mx-auto mt-16 grid max-w-4xl grid-cols-2 gap-4 sm:grid-cols-4"
          >
            {highlights.map(({ icon: Icon, label }) => (
              <div
                key={label}
                className="flex flex-col items-center rounded-2xl border border-border bg-white/60 p-4 text-center backdrop-blur-sm"
              >
                <Icon size={22} className="mb-2 text-forest" />
                <span className="text-xs font-medium text-charcoal-muted sm:text-sm">
                  {label}
                </span>
              </div>
            ))}
          </motion.div>
        </div>
      </section>

      <FeaturedTrips packages={packages} />
      <DestinationSection />
      <HowItWorks />
      <WhyJoyway />
      <BrandSection />
    </>
  );
}
