import { MapPin, Ticket, CheckCircle, Heart } from "lucide-react";
import { InViewReveal } from "@/components/motion/InViewReveal";

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
        <div className="mb-14 text-center">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-forest">Simple process</p>
          <h2 className="mt-2 font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            How JOYWAY Works
          </h2>
          <p className="mt-3 text-charcoal-muted">From discovery to departure in 4 easy steps</p>
        </div>

        {/* Steps grid with connecting line */}
        <div className="relative">
          {/* Desktop connecting line */}
          <div
            className="absolute left-0 right-0 hidden lg:block"
            style={{ top: "3.25rem" }}
            aria-hidden
          >
            <div className="mx-auto flex max-w-5xl items-center px-[calc(12.5%+1rem)]">
              {steps.slice(0, -1).map((step) => (
                <div key={step.number} className="flex flex-1 items-center">
                  <div className="h-px flex-1 border-t-2 border-dashed border-forest/25" />
                  <div className="mx-1 h-1.5 w-1.5 shrink-0 rounded-full bg-forest/30" />
                </div>
              ))}
            </div>
          </div>

          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {steps.map((step, index) => (
              <InViewReveal
                key={step.number}
                delay={index * 0.12}
                className="relative flex flex-col rounded-2xl border border-border bg-white p-6 transition-shadow hover:shadow-md"
              >
                {/* Step number badge */}
                <div className="relative mb-5 flex items-start justify-between">
                  <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-forest/8 transition-colors group-hover:bg-forest/15">
                    <step.icon size={22} className="text-forest" />
                  </div>
                  <span className="font-display text-4xl font-bold leading-none text-forest/15">
                    {step.number}
                  </span>
                </div>

                {/* Mobile connecting line (vertical) */}
                {index < steps.length - 1 && (
                  <div
                    className="absolute -bottom-4 left-9 z-10 block h-4 w-px border-l-2 border-dashed border-forest/25 sm:hidden"
                    aria-hidden
                  />
                )}

                <h3 className="font-display text-lg font-semibold text-charcoal">
                  {step.title}
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-charcoal-muted">
                  {step.description}
                </p>
              </InViewReveal>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
