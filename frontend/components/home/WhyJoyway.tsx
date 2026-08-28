"use client";

import { motion } from "framer-motion";
import { Bus, Package, Users, IndianRupee } from "lucide-react";

const features = [
  {
    icon: Bus,
    title: "Hassle-Free Travel",
    description: "Transport arranged from Delhi. Just show up and go.",
  },
  {
    icon: Package,
    title: "Complete Packages",
    description: "Stay, transport and itinerary in one package.",
  },
  {
    icon: Users,
    title: "Group Experiences",
    description: "Travel, meet people and create memories.",
  },
  {
    icon: IndianRupee,
    title: "Transparent Pricing",
    description: "Clear pricing before booking. No hidden charges.",
  },
];

export function WhyJoyway() {
  return (
    <section className="bg-forest py-20 text-white sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-12 text-center">
          <h2 className="font-display text-3xl font-semibold sm:text-4xl">
            Why JOYWAY
          </h2>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
              className="rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur-sm"
            >
              <feature.icon size={28} className="text-accent" />
              <h3 className="mt-4 font-display text-lg font-semibold">
                {feature.title}
              </h3>
              <p className="mt-2 text-sm leading-relaxed text-white/70">
                {feature.description}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
