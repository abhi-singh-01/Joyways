"use client";

import { motion } from "framer-motion";
import { MapPin, Ticket, CheckCircle, Heart } from "lucide-react";

const steps = [
  {
    number: "01",
    title: "Choose Your Trip",
    description: "Browse our curated group packages from Delhi to beautiful destinations.",
    icon: MapPin,
  },
  {
    number: "02",
    title: "Book Your Seat",
    description: "Enter traveller details and secure your spot with easy online payment.",
    icon: Ticket,
  },
  {
    number: "03",
    title: "Get Confirmation",
    description: "Receive instant booking confirmation and trip details via email.",
    icon: CheckCircle,
  },
  {
    number: "04",
    title: "Travel & Enjoy",
    description: "Show up at the pickup point and let the journey begin.",
    icon: Heart,
  },
];

export function HowItWorks() {
  return (
    <section className="py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-12 text-center">
          <h2 className="font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            How JOYWAY Works
          </h2>
        </div>

        <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map((step, index) => (
            <motion.div
              key={step.number}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
              className="relative rounded-2xl border border-border bg-white p-6"
            >
              <span className="font-display text-4xl font-bold text-forest/20">
                {step.number}
              </span>
              <step.icon size={24} className="mt-4 text-forest" />
              <h3 className="mt-4 font-display text-lg font-semibold text-charcoal">
                {step.title}
              </h3>
              <p className="mt-2 text-sm leading-relaxed text-charcoal-muted">
                {step.description}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
