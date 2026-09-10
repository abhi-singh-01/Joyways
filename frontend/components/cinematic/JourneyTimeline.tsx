"use client";

import { useEffect, useRef, useState } from "react";
import { usePrefersReducedMotion } from "@/components/cinematic/usePrefersReducedMotion";
import { cn } from "@/lib/utils";

const STEPS = [
  {
    id: "01",
    title: "Discover",
    copy: "Browse live group journeys from Delhi — destinations, dates, and seats as they stand today.",
  },
  {
    id: "02",
    title: "Choose",
    copy: "Open a trip for the itinerary, stay, inclusions, and pickup points before you commit.",
  },
  {
    id: "03",
    title: "Book",
    copy: "Add travellers, review the fare, and pay securely with Razorpay. No customer login required.",
  },
  {
    id: "04",
    title: "Travel",
    copy: "Meet at the chosen pickup, travel with the group, and follow the planned days on the road.",
  },
  {
    id: "05",
    title: "Remember",
    copy: "Keep your booking reference. After the trip, you can leave a review from booking lookup.",
  },
];

export function JourneyTimeline() {
  const reducedMotion = usePrefersReducedMotion();
  const [active, setActive] = useState(0);
  const itemRefs = useRef<Array<HTMLLIElement | null>>([]);

  useEffect(() => {
    if (reducedMotion) return;
    const nodes = itemRefs.current.filter(Boolean) as HTMLLIElement[];
    if (!nodes.length) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
        if (!visible) return;
        const index = Number((visible.target as HTMLElement).dataset.index);
        if (Number.isFinite(index)) setActive(index);
      },
      { rootMargin: "-35% 0px -45% 0px", threshold: [0.25, 0.6] }
    );

    nodes.forEach((node) => observer.observe(node));
    return () => observer.disconnect();
  }, [reducedMotion]);

  return (
    <section className="bg-off-white py-20 sm:py-28">
      <div className="mx-auto grid max-w-7xl gap-12 px-4 sm:px-6 lg:grid-cols-12 lg:px-8">
        <div className="lg:col-span-5 lg:sticky lg:top-24 lg:self-start">
          <h2 className="font-display text-3xl font-semibold text-charcoal sm:text-5xl">
            Your journey, your way.
          </h2>
          <p className="mt-4 max-w-md text-charcoal-muted">
            JOYWAY is a guest booking platform for organised group travel — from the first
            look at a trip through confirmation on the road.
          </p>
        </div>

        <ol className="relative lg:col-span-7">
          <span
            aria-hidden
            className="absolute bottom-4 left-[11px] top-4 w-px bg-border md:left-[15px]"
          />
          {STEPS.map((step, index) => {
            const isActive = reducedMotion || index === active;
            return (
              <li
                key={step.id}
                ref={(node) => {
                  itemRefs.current[index] = node;
                }}
                data-index={index}
                className="relative pb-12 last:pb-0"
              >
                <div
                  className={cn(
                    "absolute left-0 top-1.5 h-[23px] w-[23px] rounded-full border-2 bg-off-white md:h-[31px] md:w-[31px]",
                    isActive ? "border-forest bg-forest" : "border-border"
                  )}
                />
                <div className="pl-10 md:pl-14">
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-forest">
                    {step.id}
                  </p>
                  <h3
                    className={cn(
                      "mt-2 font-display text-2xl font-semibold sm:text-3xl",
                      isActive ? "text-charcoal" : "text-charcoal/55"
                    )}
                  >
                    {step.title}
                  </h3>
                  <p className={cn("mt-2 max-w-md text-sm leading-relaxed", isActive ? "text-charcoal-muted" : "text-charcoal-muted/70")}>
                    {step.copy}
                  </p>
                </div>
              </li>
            );
          })}
        </ol>
      </div>
    </section>
  );
}
