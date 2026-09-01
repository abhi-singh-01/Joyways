"use client";

import { motion } from "framer-motion";
import { Quote } from "lucide-react";

const testimonials = [
  {
    quote:
      "Everything was organised — bus, hotel, itinerary. We just showed up in Delhi and enjoyed Nainital.",
    name: "Priya S.",
    trip: "Nainital group trip",
  },
  {
    quote:
      "Booking online was smooth and the team responded quickly on email. Great value for a weekend escape.",
    name: "Rahul M.",
    trip: "Mussoorie package",
  },
  {
    quote:
      "Loved travelling with a fun group to Rishikesh. Joyway handled transport so we could focus on the experience.",
    name: "Ananya K.",
    trip: "Rishikesh journey",
  },
];

export function TestimonialsSection() {
  return (
    <section className="bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-12 text-center">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-forest">
            Traveler stories
          </p>
          <h2 className="mt-2 font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            Loved by group travelers
          </h2>
        </div>

        <div className="grid gap-6 md:grid-cols-3">
          {testimonials.map((item, index) => (
            <motion.blockquote
              key={item.name}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1, duration: 0.5 }}
              className="relative rounded-2xl border border-border bg-white p-6 shadow-sm"
            >
              <Quote
                size={28}
                className="mb-4 text-forest/25"
                aria-hidden
              />
              <p className="text-sm leading-relaxed text-charcoal-muted">
                &ldquo;{item.quote}&rdquo;
              </p>
              <footer className="mt-5 border-t border-border pt-4">
                <p className="font-semibold text-charcoal">{item.name}</p>
                <p className="text-xs text-charcoal-muted">{item.trip}</p>
              </footer>
            </motion.blockquote>
          ))}
        </div>
      </div>
    </section>
  );
}
