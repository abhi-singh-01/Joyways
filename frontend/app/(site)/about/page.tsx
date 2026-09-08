import type { Metadata } from "next";
import { Bus, Heart, MapPin, Shield, Sparkles } from "lucide-react";
import { PageHero } from "@/components/layout/PageHero";
import { ButtonLink } from "@/components/ui/ButtonLink";

export const metadata: Metadata = {
  title: "About | JOYWAY",
  description: "Learn about JOYWAY — a modern group travel platform from Delhi, India.",
};

const values = [
  {
    icon: Heart,
    title: "Joy-first travel",
    text: "We believe the journey matters as much as the destination.",
  },
  {
    icon: MapPin,
    title: "Curated routes",
    text: "Hill stations and spiritual getaways handpicked for Delhi travelers.",
  },
  {
    icon: Bus,
    title: "Complete packages",
    text: "Transport, stay, and itinerary — one booking, zero hassle.",
  },
  {
    icon: Shield,
    title: "Transparent & secure",
    text: "Clear pricing, secure payments, and responsive support.",
  },
];

const stats = [
  { value: "5+", label: "Destinations" },
  { value: "Delhi", label: "Departure hub" },
  { value: "Group", label: "Travel style" },
  { value: "Email", label: "And WhatsApp support" },
];

export default function AboutPage() {
  return (
    <>
      <PageHero
        eyebrow="Our story"
        title="Group travel made simple, joyful, and modern"
        description="JOYWAY is an Indian travel startup helping Delhi travelers explore hills, rivers, and spiritual destinations through curated group packages."
      />

      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {stats.map((stat) => (
            <div
              key={stat.label}
              className="rounded-2xl border border-border bg-white p-6 text-center shadow-sm"
            >
              <p className="font-display text-3xl font-semibold text-forest">{stat.value}</p>
              <p className="mt-1 text-sm text-charcoal-muted">{stat.label}</p>
            </div>
          ))}
        </div>

        <div className="mt-16 grid gap-12 lg:grid-cols-2 lg:items-center">
          <div>
            <h2 className="font-display text-3xl font-semibold text-charcoal">
              Find Joy in Every Journey
            </h2>
            <p className="mt-4 text-lg leading-relaxed text-charcoal-muted">
              We started JOYWAY to remove the friction from group travel — no chasing
              operators, no unclear pricing, no last-minute chaos. Just well-planned
              trips you can book online and show up for.
            </p>
            <p className="mt-4 text-lg leading-relaxed text-charcoal-muted">
              From Nainital weekends to Rishikesh spiritual escapes, every package is
              designed for travelers who want comfort, community, and memorable roads.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <ButtonLink href="/trips">Browse trips</ButtonLink>
              <ButtonLink href="/contact" variant="outline">
                Talk to us
              </ButtonLink>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            {values.map(({ icon: Icon, title, text }) => (
              <div
                key={title}
                className="rounded-2xl border border-border bg-surface/60 p-5"
              >
                <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-forest/10 text-forest">
                  <Icon size={20} />
                </div>
                <h3 className="font-semibold text-charcoal">{title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-charcoal-muted">{text}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="mt-16 rounded-3xl bg-forest px-8 py-10 text-center text-white sm:px-12">
          <Sparkles className="mx-auto text-accent" size={28} />
          <h2 className="mt-4 font-display text-2xl font-semibold sm:text-3xl">
            Ready for your next escape?
          </h2>
          <p className="mx-auto mt-3 max-w-xl text-white/75">
            Join a group departure from Delhi and let us handle transport, stay, and
            the details.
          </p>
          <ButtonLink
            href="/trips"
            variant="secondary"
            className="mt-6 inline-flex"
          >
            Explore upcoming trips
          </ButtonLink>
        </div>
      </section>
    </>
  );
}
