import type { Metadata } from "next";
import Image from "next/image";
import { PageHero } from "@/components/layout/PageHero";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { CINEMATIC_IMAGES } from "@/components/cinematic/cinematicMedia";

export const metadata: Metadata = {
  title: "About | JOYWAY",
  description:
    "JOYWAY is a group travel platform from Delhi — curated trips, secure booking, and journeys that stay with you.",
  openGraph: {
    title: "About | JOYWAY",
    description:
      "Group travel from Delhi. Curated packages, secure checkout, and booking lookup without a customer login.",
    url: "https://joyway.in/about",
  },
  alternates: {
    canonical: "https://joyway.in/about",
  },
};

const facts = [
  { title: "Delhi departures", text: "Group packages leave from Delhi and nearby pickup points." },
  { title: "One booking", text: "Transport, stay, and itinerary are planned together." },
  { title: "Secure checkout", text: "Pay with Razorpay. We never store card numbers or UPI PINs." },
  { title: "Guest lookup", text: "Find your booking anytime with reference and email." },
];

const values = [
  {
    title: "The road matters",
    text: "We plan the journey as carefully as the destination — pickup, travel days, and the stay.",
  },
  {
    title: "Curated, not crowded",
    text: "Hill and heritage routes chosen for Delhi travelers who want a clear plan, not a catalogue of every city.",
  },
  {
    title: "Book without friction",
    text: "No customer account. Choose a trip, add travellers, pay securely, and keep the confirmation email.",
  },
  {
    title: "Real support",
    text: "Call, WhatsApp, or write to JOYWAY when you need help with a trip or an existing booking.",
  },
];

export default function AboutPage() {
  return (
    <>
      <PageHero
        illustrated
        eyebrow="About JOYWAY"
        title="Group travel made simple, joyful, and modern"
        description="JOYWAY is an Indian travel studio helping Delhi travelers explore hills, valleys, and heritage roads through curated group packages."
      />

      <section className="border-b border-border bg-off-white">
        <div className="mx-auto max-w-7xl divide-y divide-border px-4 sm:px-6 lg:grid lg:grid-cols-4 lg:divide-x lg:divide-y-0 lg:px-8">
          {facts.map((fact) => (
            <div key={fact.title} className="py-8 lg:px-6 lg:py-12 first:lg:pl-0 last:lg:pr-0">
              <h2 className="font-display text-xl font-semibold text-charcoal">{fact.title}</h2>
              <p className="mt-2 text-sm leading-relaxed text-charcoal-muted">{fact.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto grid max-w-7xl items-center gap-10 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:gap-16 lg:px-8 lg:py-24">
        <div className="relative w-full min-h-[16rem] overflow-hidden sm:min-h-[22rem] md:min-h-[28rem]">
          <Image
            src={CINEMATIC_IMAGES.roads}
            alt="Walker on a high ridgeline above the clouds"
            fill
            sizes="(max-width: 1024px) 100vw, 50vw"
            className="object-cover object-center"
          />
        </div>
        <div>
          <h2 className="font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            Find joy in every journey.
          </h2>
          <p className="mt-5 text-lg leading-relaxed text-charcoal-muted">
            We started JOYWAY to take the friction out of group travel — no chasing operators,
            no unclear pricing, no last-minute chaos. Book online, show up at pickup, and travel
            with the group.
          </p>
          <p className="mt-4 text-lg leading-relaxed text-charcoal-muted">
            From Nainital weekends to Rishikesh and Himalayan roads, every package is built for
            people who want comfort, company, and a route they will remember.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <ButtonLink href="/trips">Browse trips</ButtonLink>
            <ButtonLink href="/contact" variant="outline">
              Talk to us
            </ButtonLink>
          </div>
        </div>
      </section>

      <section className="bg-surface">
        <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8 lg:py-24">
          <h2 className="max-w-2xl font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            How we travel.
          </h2>
          <ul className="mt-10 divide-y divide-border border-y border-border">
            {values.map((item, index) => (
              <li
                key={item.title}
                className="grid gap-2 py-8 sm:grid-cols-[4rem_minmax(12rem,18rem)_1fr] sm:items-baseline"
              >
                <span className="font-display text-sm font-semibold text-forest">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <h3 className="font-display text-xl font-semibold text-charcoal">{item.title}</h3>
                <p className="text-sm leading-relaxed text-charcoal-muted">{item.text}</p>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="relative min-h-[70svh] overflow-hidden bg-forest-dark text-white">
        <Image
          src={CINEMATIC_IMAGES.closing}
          alt="Travellers looking toward snow mountains from a hillside shrine"
          fill
          sizes="100vw"
          className="object-cover object-center"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/40 to-black/25" />
        <div className="relative z-10 mx-auto flex min-h-[70svh] max-w-7xl flex-col justify-end px-4 py-16 sm:px-6 lg:px-8">
          <h2 className="max-w-2xl font-display text-3xl font-semibold sm:text-5xl">
            Ready for your next escape?
          </h2>
          <p className="mt-4 max-w-xl text-white/80">
            Join a group departure from Delhi. Transport, stay, and the days on the road are
            already planned.
          </p>
          <ButtonLink
            href="/trips"
            className="mt-8 w-fit bg-white text-forest hover:bg-accent-light hover:text-forest-dark"
          >
            Explore upcoming trips
          </ButtonLink>
        </div>
      </section>
    </>
  );
}
