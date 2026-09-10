"use client";

import { useRef } from "react";
import Link from "next/link";
import { motion, useScroll } from "framer-motion";
import { ArrowRight, Bus, MapPin, Shield, Sparkles } from "lucide-react";
import { MovingBorder } from "@/components/effects/MovingBorder";
import { HeroLandscape } from "@/components/home/HeroLandscape";
import { TripSearchBar } from "@/components/home/TripSearchBar";
import { ButtonLink } from "@/components/ui/ButtonLink";

const highlights = [
  { icon: MapPin, label: "Delhi Departures" },
  { icon: Sparkles, label: "Handpicked Destinations" },
  { icon: Bus, label: "Transport + Stay Packages" },
  { icon: Shield, label: "Secure Online Booking" },
];

const heroCardVariants = {
  hidden: { opacity: 0, y: 24 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.7,
      ease: "easeOut" as const,
      staggerChildren: 0.1,
      delayChildren: 0.15,
    },
  },
};

const heroItemVariants = {
  hidden: { opacity: 0, y: 12 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.6, ease: "easeOut" as const },
  },
};

const heroFadeVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { duration: 0.6, ease: "easeOut" as const },
  },
};

export function HeroSection() {
  const heroRef = useRef<HTMLElement>(null);
  const { scrollYProgress } = useScroll({
    target: heroRef,
    offset: ["start start", "end start"],
  });

  return (
    <section ref={heroRef} className="relative overflow-x-clip lg:min-h-[92vh]">
      <HeroLandscape scrollYProgress={scrollYProgress} />

      <div className="relative mx-auto flex w-full max-w-7xl flex-col items-center gap-10 px-4 pb-14 pt-20 sm:gap-12 sm:px-6 lg:min-h-[92vh] lg:justify-center lg:gap-10 lg:pb-20 lg:px-8">
        <motion.div
          variants={heroCardVariants}
          initial="hidden"
          animate="visible"
          className="relative z-20 mx-auto w-full max-w-3xl shrink-0 rounded-3xl border border-white/50 bg-white/82 px-5 py-8 text-center shadow-2xl shadow-forest/15 backdrop-blur-xl sm:px-10 sm:py-12"
        >
          <motion.p
            variants={heroItemVariants}
            className="mb-3 text-sm font-semibold uppercase tracking-[0.2em] text-forest"
          >
            Group Travel from Delhi
          </motion.p>
          <motion.h1
            variants={heroFadeVariants}
            className="font-display text-4xl font-semibold leading-tight text-forest-dark sm:text-5xl lg:text-6xl"
          >
            Find Joy in Every Journey.
          </motion.h1>
          <motion.p
            variants={heroItemVariants}
            className="mx-auto mt-5 max-w-xl text-lg leading-relaxed text-charcoal"
          >
            Discover beautiful destinations, join unforgettable group trips, and
            let us take care of the journey.
          </motion.p>
          <TripSearchBar />
          <motion.div
            variants={heroItemVariants}
            className="mt-6 flex w-full flex-col items-stretch justify-center gap-4 sm:mt-8 sm:flex-row sm:items-center sm:gap-4"
          >
            <Link href="/trips" className="block w-full shrink-0 sm:w-auto">
              <MovingBorder containerClassName="block w-full sm:inline-block">
                <span className="group inline-flex w-full min-w-0 items-center justify-center rounded-full bg-forest px-8 py-3.5 text-base font-medium text-white transition-colors hover:bg-forest-light sm:min-w-[200px] sm:py-4 sm:text-lg">
                  Explore Trips
                  <ArrowRight
                    size={18}
                    className="ml-2 transition-transform group-hover:translate-x-1"
                  />
                </span>
              </MovingBorder>
            </Link>
            <ButtonLink
              href="/contact"
              variant="outline"
              size="lg"
              className="block w-full shrink-0 bg-white sm:min-w-[200px] sm:w-auto"
            >
              Plan with us
            </ButtonLink>
          </motion.div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.45 }}
          className="relative z-10 mx-auto grid w-full max-w-4xl shrink-0 grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-3"
        >
          {highlights.map(({ icon: Icon, label }) => (
            <div
              key={label}
              className="flex flex-col items-center rounded-2xl border border-white/50 bg-white/85 p-3 text-center shadow-md backdrop-blur-md transition-transform duration-200 hover:-translate-y-1 hover:scale-[1.02] sm:p-4"
            >
              <Icon size={20} className="mb-1.5 text-forest sm:mb-2" />
              <span className="text-[11px] font-medium leading-snug text-charcoal sm:text-sm">
                {label}
              </span>
            </div>
          ))}
        </motion.div>
      </div>
    </section>
  );
}
