"use client";

import { useRef } from "react";
import { useGSAP } from "@gsap/react";
import { ParallaxFrame } from "@/components/cinematic/ParallaxFrame";
import { CINEMATIC_IMAGES } from "@/components/cinematic/cinematicMedia";
import { usePrefersReducedMotion } from "@/components/cinematic/usePrefersReducedMotion";
import { gsap, registerCinematicGsap } from "@/components/cinematic/gsapSetup";

const statements = [
  {
    image: CINEMATIC_IMAGES.landscape,
    alt: "Sunlit mountain cliffs above a sea of cloud",
    line: "Every road has a story.",
  },
  {
    image: CINEMATIC_IMAGES.mountains,
    alt: "Green ridgeline toward a sharp mountain peak",
    line: "Every destination leaves something behind.",
  },
  {
    image: CINEMATIC_IMAGES.heritage,
    alt: "Monastery terraces on a Himalayan hillside",
    line: "Every journey changes the way you see home.",
  },
];

export function StoryChapter() {
  const rootRef = useRef<HTMLElement>(null);
  const reducedMotion = usePrefersReducedMotion();

  useGSAP(
    () => {
      registerCinematicGsap();
      if (reducedMotion) return;
      const cards = rootRef.current?.querySelectorAll<HTMLElement>("[data-story-card]");
      if (!cards?.length) return;
      gsap.from(cards, {
        y: 40,
        autoAlpha: 0,
        stagger: 0.12,
        duration: 0.85,
        ease: "power2.out",
        scrollTrigger: {
          trigger: rootRef.current,
          start: "top 75%",
        },
      });
    },
    { scope: rootRef, dependencies: [reducedMotion] }
  );

  return (
    <section ref={rootRef} className="bg-off-white py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2 className="max-w-2xl font-display text-3xl font-semibold text-charcoal sm:text-5xl">
          The road is the story.
        </h2>
        <p className="mt-4 max-w-xl text-charcoal-muted">
          Editorial notes from the landscapes we travel — not attributed customer reviews.
        </p>

        <div className="mt-14 grid gap-6 md:grid-cols-3">
          {statements.map((item) => (
            <figure key={item.line} data-story-card className="relative w-full min-h-[16rem] overflow-hidden sm:min-h-[22rem]">
              <ParallaxFrame src={item.image} alt={item.alt} speed={0.55} sizes="(max-width: 768px) 100vw, 33vw" />
              <figcaption className="absolute inset-x-0 bottom-0 z-10 bg-gradient-to-t from-black/75 to-transparent p-6">
                <p className="font-display text-xl font-semibold text-white">{item.line}</p>
              </figcaption>
            </figure>
          ))}
        </div>
      </div>
    </section>
  );
}
