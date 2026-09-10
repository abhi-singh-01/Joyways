"use client";

import { motion } from "framer-motion";
import { splitLines } from "@/lib/format";

type PackageItineraryProps = {
  itinerary: string;
};

export function PackageItinerary({ itinerary }: Readonly<PackageItineraryProps>) {
  const days = splitLines(itinerary);

  if (days.length === 0) {
    return null;
  }

  return (
    <div className="space-y-0">
      {days.map((day, index) => {
        const isDayHeader = /^Day\s+\d+/i.test(day);
        return (
          <motion.div
            key={`${index}-${day.slice(0, 20)}`}
            initial={{ opacity: 0, x: -12 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true, margin: "-20px" }}
            transition={{ duration: 0.4, delay: index * 0.06 }}
            className="relative flex gap-4 pb-8 last:pb-0"
          >
            <div className="flex flex-col items-center">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-forest text-xs font-bold text-white">
                {index + 1}
              </div>
              {index < days.length - 1 && (
                <div className="mt-2 w-px flex-1 bg-border" />
              )}
            </div>
            <div className="flex-1 pt-0.5">
              <p
                className={
                  isDayHeader
                    ? "font-semibold text-charcoal"
                    : "text-sm leading-relaxed text-charcoal-muted"
                }
              >
                {day}
              </p>
            </div>
          </motion.div>
        );
      })}
    </div>
  );
}
