"use client";

import { motion } from "framer-motion";
import { JoywayLogo } from "@/components/layout/JoywayLogo";

export function BrandSection() {
  return (
    <section className="py-24 sm:py-32">
      <div className="mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7 }}
        >
          <h2 className="font-display text-3xl font-semibold leading-tight text-charcoal sm:text-4xl lg:text-5xl">
            The journey matters too.
          </h2>
          <p className="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-charcoal-muted">
            We believe travel isn&apos;t just about reaching a destination.
            It&apos;s about the people you meet, the roads you take and the
            memories you bring back.
          </p>
          <div className="mt-12 flex flex-col items-center">
            <JoywayLogo href={null} height={56} />
            <p className="mt-3 text-lg text-accent">Find Joy in Every Journey.</p>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
