"use client";

import { useRef } from "react";
import Link from "next/link";
import { motion, useScroll, useTransform } from "framer-motion";
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

  const heroContentY = useTransform(scrollYProgress, [0, 1], [0, 56]);

  return (
    <section ref={heroRef} className="relative min-h-[92vh] overflow-hidden">
      <HeroLandscape scrollYProgress={scrollYProgress} />

      <motion.div
        style={{ y: heroContentY }}
        className="mx-auto flex min-h-[92vh] max-w-7xl items-center px-4 pb-24 pt-20 sm:px-6 lg:px-8"
      >
        <motion.div
          variants={heroCardVariants}
          initial="hidden"
          animate="visible"
          className="mx-auto w-full max-w-3xl rounded-3xl border border-white/50 bg-white/82 px-6 py-10 text-center shadow-2xl shadow-forest/15 backdrop-blur-xl sm:px-10 sm:py-12"
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
            className="mt-8 flex flex-col items-center justify-center gap-4 sm:flex-row"
          >
            <Link href="/trips">
              <MovingBorder containerClassName="inline-block">
                <span className="group inline-flex min-w-[200px] items-center justify-center rounded-full bg-forest px-8 py-4 text-lg font-medium text-white transition-colors hover:bg-forest-light">
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
              className="min-w-[200px] bg-white"
            >
              Plan with us
            </ButtonLink>
          </motion.div>
        </motion.div>
      </motion.div>

      <div className="absolute bottom-8 left-0 right-0 z-10 px-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.45 }}
          className="mx-auto grid max-w-4xl grid-cols-2 gap-3 sm:grid-cols-4"
        >
          {highlights.map(({ icon: Icon, label }) => (
            <div
              key={label}
              className="flex flex-col items-center rounded-2xl border border-white/50 bg-white/85 p-4 text-center shadow-md backdrop-blur-md transition-transform duration-200 hover:-translate-y-1 hover:scale-[1.02]"
            >
              <Icon size={22} className="mb-2 text-forest" />
              <span className="text-xs font-medium text-charcoal sm:text-sm">
                {label}
              </span>
            </div>
          ))}
        </motion.div>
      </div>
    </section>
  );
}
