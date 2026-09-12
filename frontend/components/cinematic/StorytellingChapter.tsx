"use client";

import { useRef } from "react";
import { useGSAP } from "@gsap/react";
import { ParallaxFrame } from "@/components/cinematic/ParallaxFrame";
import { CINEMATIC_IMAGES } from "@/components/cinematic/cinematicMedia";
import { usePrefersReducedMotion } from "@/components/cinematic/usePrefersReducedMotion";
import { gsap, registerCinematicGsap } from "@/components/cinematic/gsapSetup";

const lines = [
  "Wake before sunrise.",
  "Take the road that wasn't on the map.",
  "Stay somewhere you'll remember.",
  "Come back with more than photographs.",
];

export function StorytellingChapter() {
  const sectionRef = useRef<HTMLElement>(null);
  const reducedMotion = usePrefersReducedMotion();

  useGSAP(
    () => {
      registerCinematicGsap();
      if (reducedMotion) return;
      const items = sectionRef.current?.querySelectorAll<HTMLElement>("[data-story-line]");
      if (!items?.length) return;
      gsap.from(items, {
        y: 36,
        autoAlpha: 0,
        stagger: 0.16,
        duration: 0.9,
        ease: "power2.out",
        scrollTrigger: {
          trigger: sectionRef.current,
          start: "top 68%",
        },
      });
    },
    { scope: sectionRef, dependencies: [reducedMotion] }
  );

  return (
    <section ref={sectionRef} className="relative min-h-[72svh] overflow-hidden bg-forest-dark text-white md:min-h-[100svh]">
      <ParallaxFrame
        src={CINEMATIC_IMAGES.roads}
        alt="Walker crossing a high ridgeline above the clouds"
        speed={0.8}
        drift={2.5}
      />
      <div className="absolute inset-0 bg-black/50" />
      <div className="relative z-10 mx-auto flex min-h-[72svh] max-w-7xl flex-col justify-end px-4 pb-[calc(env(safe-area-inset-bottom,0px)+7.5rem)] pt-16 sm:px-6 sm:py-20 md:min-h-[100svh] lg:px-8">
        <ul className="max-w-xl space-y-4 sm:space-y-6">
          {lines.map((line) => (
            <li
              key={line}
              data-story-line
              className="font-display text-xl font-semibold leading-snug sm:text-3xl md:text-4xl"
            >
              {line}
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
