"use client";

import { useRef } from "react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { motion, useMotionValueEvent, useScroll, useTransform } from "framer-motion";
import { ArrowRight, Bus, MapPin, Shield, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { AuroraBackground } from "@/components/effects/AuroraBackground";
import { Sparkles as SparkleField } from "@/components/effects/Sparkles";
import { MovingBorder } from "@/components/effects/MovingBorder";
import { FeaturedTrips } from "@/components/home/FeaturedTrips";
import { DestinationSection } from "@/components/home/DestinationSection";
import { HowItWorks } from "@/components/home/HowItWorks";
import { WhyJoyway } from "@/components/home/WhyJoyway";
import { BrandSection } from "@/components/home/BrandSection";
import { JourneyMoments } from "@/components/home/JourneyMoments";
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
  const heroRef = useRef<HTMLElement>(null);
  const scrollRef = useRef(0);
  const { scrollYProgress } = useScroll({
    target: heroRef,
    offset: ["start start", "end start"],
  });

  useMotionValueEvent(scrollYProgress, "change", (value) => {
    scrollRef.current = value;
  });

  const overlayForest = useTransform(scrollYProgress, [0, 1], [0.35, 0.85]);
  const overlayAccent = useTransform(scrollYProgress, [0, 0.5, 1], [0, 0.15, 0.35]);
  const heroContentY = useTransform(scrollYProgress, [0, 1], [0, 80]);
  const heroContentOpacity = useTransform(scrollYProgress, [0, 0.75, 1], [1, 0.85, 0.4]);

  return (
    <>
      <section ref={heroRef} className="relative min-h-[92vh] overflow-hidden">
        <div className="absolute inset-0 -z-10">
          <HeroMountains scrollRef={scrollRef} />
          <AuroraBackground />
          <SparkleField count={20} />
          <motion.div
            className="absolute inset-0 bg-gradient-to-b from-off-white/30 via-off-white/70 to-off-white"
            style={{ opacity: overlayForest }}
          />
          <motion.div
            className="absolute inset-0 bg-gradient-to-br from-accent/10 via-transparent to-forest/10"
            style={{ opacity: overlayAccent }}
          />
        </div>

        <motion.div
          style={{ y: heroContentY, opacity: heroContentOpacity }}
          className="mx-auto max-w-7xl px-4 pb-20 pt-24 sm:px-6 sm:pb-28 sm:pt-32 lg:px-8"
        >
          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: "easeOut" }}
            className="mx-auto max-w-3xl text-center"
          >
            <motion.p
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.15, duration: 0.6 }}
              className="mb-4 text-sm font-semibold uppercase tracking-[0.2em] text-forest"
            >
              Group Travel from Delhi
            </motion.p>
            <div className="flex justify-center">
              <JoywayLogo href={null} height={80} priority />
            </div>
            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.35, duration: 0.6 }}
              className="mt-4 bg-gradient-to-r from-forest via-forest-light to-accent bg-clip-text font-display text-xl text-transparent sm:text-2xl"
            >
              Find Joy in Every Journey.
            </motion.p>
            <motion.p
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.45, duration: 0.6 }}
              className="mx-auto mt-6 max-w-xl text-lg leading-relaxed text-charcoal-muted"
            >
              Discover beautiful destinations, join unforgettable group trips, and
              let us take care of the journey.
            </motion.p>
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.55, duration: 0.6 }}
              className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row"
            >
              <Link href="/trips">
                <MovingBorder containerClassName="inline-block">
                  <Button size="lg" className="group min-w-[200px] rounded-full bg-forest hover:bg-forest-light">
                    Explore Trips
                    <ArrowRight
                      size={18}
                      className="ml-2 transition-transform group-hover:translate-x-1"
                    />
                  </Button>
                </MovingBorder>
              </Link>
              <Link href="/trips">
                <Button variant="outline" size="lg" className="min-w-[200px] backdrop-blur-sm">
                  Book Your Journey
                </Button>
              </Link>
            </motion.div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.3 }}
            className="mx-auto mt-16 grid max-w-4xl grid-cols-2 gap-4 sm:grid-cols-4"
          >
            {highlights.map(({ icon: Icon, label }, index) => (
              <motion.div
                key={label}
                whileHover={{ y: -4, scale: 1.02 }}
                transition={{ type: "spring", stiffness: 320, damping: 22 }}
                className="flex flex-col items-center rounded-2xl border border-white/40 bg-white/55 p-4 text-center shadow-sm backdrop-blur-md"
                style={{ animationDelay: `${index * 0.08}s` }}
              >
                <Icon size={22} className="mb-2 text-forest" />
                <span className="text-xs font-medium text-charcoal-muted sm:text-sm">
                  {label}
                </span>
              </motion.div>
            ))}
          </motion.div>
        </motion.div>

        <motion.div
          aria-hidden
          className="pointer-events-none absolute bottom-6 left-1/2 -translate-x-1/2"
          animate={{ y: [0, 8, 0] }}
          transition={{ repeat: Infinity, duration: 2.2, ease: "easeInOut" }}
        >
          <div className="h-10 w-6 rounded-full border border-forest/30 p-1">
            <motion.div
              className="mx-auto h-2 w-1 rounded-full bg-forest/60"
              animate={{ y: [0, 12, 0], opacity: [1, 0.3, 1] }}
              transition={{ repeat: Infinity, duration: 2.2, ease: "easeInOut" }}
            />
          </div>
        </motion.div>
      </section>

      <FeaturedTrips packages={packages} />
      <JourneyMoments />
      <DestinationSection />
      <HowItWorks />
      <WhyJoyway />
      <BrandSection />
    </>
  );
}
