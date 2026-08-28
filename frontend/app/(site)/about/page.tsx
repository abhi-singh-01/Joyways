import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "About | JOYWAY",
  description: "Learn about JOYWAY — a modern group travel platform from Delhi, India.",
};

export default function AboutPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-20 sm:px-6 lg:px-8">
      <h1 className="font-display text-4xl font-semibold text-charcoal">About JOYWAY</h1>
      <p className="mt-6 text-lg leading-relaxed text-charcoal-muted">
        JOYWAY is a new Indian travel startup making group travel from Delhi simple,
        affordable, and joyful. We curate complete packages with transport, stay, and
        itinerary — so you can focus on the experience.
      </p>
      <p className="mt-4 text-lg leading-relaxed text-charcoal-muted">
        Our tagline says it all: <strong className="text-forest">Find Joy in Every Journey.</strong>
      </p>
    </div>
  );
}
