import { InViewReveal } from "@/components/motion/InViewReveal";
import { MotionTiltCard } from "@/components/motion/MotionTiltCard";

const destinations = [
  { name: "Nainital", emoji: "🏔️", rotate: -3 },
  { name: "Mussoorie", emoji: "⛰️", rotate: 2 },
  { name: "Rishikesh", emoji: "🌊", rotate: -2 },
  { name: "Haridwar", emoji: "🪔", rotate: 3 },
  { name: "Dalhousie", emoji: "❄️", rotate: -1 },
  { name: "Manali", emoji: "🌲", rotate: 2 },
  { name: "Shimla", emoji: "🚂", rotate: -2 },
];

const marqueeItems = [...destinations, ...destinations];

export function DestinationSection() {
  return (
    <section className="overflow-hidden bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <InViewReveal
          y={16}
          duration={0.55}
          className="mb-12 text-center"
        >
          <h2 className="font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            Where will you go next?
          </h2>
          <p className="mt-3 text-charcoal-muted">
            Explore India&apos;s most loved hill stations and spiritual destinations
          </p>
        </InViewReveal>

        <div className="relative mb-14 overflow-hidden py-2">
          <div className="pointer-events-none absolute inset-y-0 left-0 z-10 w-16 bg-gradient-to-r from-surface to-transparent" />
          <div className="pointer-events-none absolute inset-y-0 right-0 z-10 w-16 bg-gradient-to-l from-surface to-transparent" />
          <div className="destination-marquee flex w-max gap-4">
            {marqueeItems.map((dest, index) => (
              <div
                key={`${dest.name}-${index}`}
                className="flex shrink-0 items-center gap-2 rounded-full border border-border bg-white px-5 py-2.5 text-sm font-medium text-charcoal shadow-sm"
              >
                <span>{dest.emoji}</span>
                {dest.name}
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-center gap-6">
          {destinations.slice(0, 5).map((dest, index) => (
            <MotionTiltCard
              key={dest.name}
              rotate={dest.rotate}
              delay={index * 0.08}
              className="flex h-36 w-36 cursor-default flex-col items-center justify-center rounded-2xl border border-border bg-white shadow-sm transition-shadow hover:shadow-xl sm:h-40 sm:w-40"
            >
              <span className="text-3xl">{dest.emoji}</span>
              <span className="mt-2 font-display text-lg font-medium text-charcoal">
                {dest.name}
              </span>
            </MotionTiltCard>
          ))}
        </div>
      </div>
    </section>
  );
}
