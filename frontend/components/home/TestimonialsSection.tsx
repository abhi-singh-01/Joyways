import { Quote, Star } from "lucide-react";
import { InViewReveal } from "@/components/motion/InViewReveal";

const testimonials = [
  {
    quote:
      "Everything was organised — bus, hotel, itinerary. We just showed up in Delhi and enjoyed Nainital.",
    name: "Priya S.",
    trip: "Nainital group trip",
    rating: 5,
    initials: "PS",
    avatarColor: "bg-forest text-white",
  },
  {
    quote:
      "Booking online was smooth and the team responded quickly on email. Great value for a weekend escape.",
    name: "Rahul M.",
    trip: "Mussoorie package",
    rating: 5,
    initials: "RM",
    avatarColor: "bg-accent text-charcoal",
  },
  {
    quote:
      "Loved travelling with a fun group to Rishikesh. Joyway handled transport so we could focus on the experience.",
    name: "Ananya K.",
    trip: "Rishikesh journey",
    rating: 5,
    initials: "AK",
    avatarColor: "bg-forest-light text-white",
  },
];

function StarRow({ rating }: { rating: number }) {
  return (
    <div className="mb-3 flex items-center gap-0.5" aria-label={`${rating} out of 5 stars`}>
      {Array.from({ length: 5 }).map((_, i) => (
        <Star
          key={i}
          size={14}
          className={i < rating ? "fill-amber-400 text-amber-400" : "text-border"}
        />
      ))}
    </div>
  );
}

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
          {/* Aggregate rating */}
          <div className="mt-4 flex items-center justify-center gap-2">
            <div className="flex items-center gap-0.5">
              {Array.from({ length: 5 }).map((_, i) => (
                <Star key={i} size={16} className="fill-amber-400 text-amber-400" />
              ))}
            </div>
            <span className="text-sm font-semibold text-charcoal">4.8</span>
            <span className="text-sm text-charcoal-muted">· Based on traveler reviews</span>
          </div>
        </div>

        <div className="grid gap-6 md:grid-cols-3">
          {testimonials.map((item, index) => (
            <InViewReveal
              key={item.name}
              delay={index * 0.1}
              className="relative flex flex-col rounded-2xl border border-border bg-white p-6 shadow-sm transition-shadow hover:shadow-md"
            >
              <Quote
                size={28}
                className="mb-3 text-forest/20"
                aria-hidden
              />
              <StarRow rating={item.rating} />
              <blockquote className="flex-1">
                <p className="text-sm leading-relaxed text-charcoal-muted">
                  &ldquo;{item.quote}&rdquo;
                </p>
              </blockquote>
              <footer className="mt-5 flex items-center gap-3 border-t border-border pt-4">
                <div
                  className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-sm font-bold ${item.avatarColor}`}
                  aria-hidden
                >
                  {item.initials}
                </div>
                <div>
                  <p className="font-semibold text-charcoal">{item.name}</p>
                  <p className="text-xs text-charcoal-muted">{item.trip}</p>
                </div>
              </footer>
            </InViewReveal>
          ))}
        </div>
      </div>
    </section>
  );
}
