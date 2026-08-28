"use client";

import { motion } from "framer-motion";

const destinations = [
  { name: "Nainital", emoji: "🏔️", rotate: -3 },
  { name: "Mussoorie", emoji: "⛰️", rotate: 2 },
  { name: "Rishikesh", emoji: "🌊", rotate: -2 },
  { name: "Haridwar", emoji: "🪔", rotate: 3 },
  { name: "Dalhousie", emoji: "❄️", rotate: -1 },
];

export function DestinationSection() {
  return (
    <section className="overflow-hidden bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-12 text-center">
          <h2 className="font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            Where will you go next?
          </h2>
          <p className="mt-3 text-charcoal-muted">
            Explore India&apos;s most loved hill stations and spiritual destinations
          </p>
        </div>

        <div className="flex flex-wrap items-center justify-center gap-6">
          {destinations.map((dest, index) => (
            <motion.div
              key={dest.name}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.08 }}
              whileHover={{ y: -8, rotate: 0 }}
              style={{ rotate: `${dest.rotate}deg` }}
              className="flex h-36 w-36 cursor-default flex-col items-center justify-center rounded-2xl border border-border bg-white shadow-sm transition-shadow hover:shadow-md sm:h-40 sm:w-40"
            >
              <span className="text-3xl">{dest.emoji}</span>
              <span className="mt-2 font-display text-lg font-medium text-charcoal">
                {dest.name}
              </span>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
