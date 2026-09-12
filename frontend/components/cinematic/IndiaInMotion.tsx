"use client";

import { useRef } from "react";
import { useGSAP } from "@gsap/react";
import { LANDSCAPE_SCENES } from "@/components/cinematic/journeyFromPackages";
import { ParallaxFrame } from "@/components/cinematic/ParallaxFrame";
import { usePrefersReducedMotion } from "@/components/cinematic/usePrefersReducedMotion";
import { gsap, registerCinematicGsap } from "@/components/cinematic/gsapSetup";

type IndiaInMotionProps = {
  destinations: string[];
};

export function IndiaInMotion({ destinations }: Readonly<IndiaInMotionProps>) {
  const rootRef = useRef<HTMLElement>(null);
  const reducedMotion = usePrefersReducedMotion();

  useGSAP(
    () => {
      registerCinematicGsap();
      if (reducedMotion) return;
      const copies = rootRef.current?.querySelectorAll<HTMLElement>("[data-scene-copy]");
      copies?.forEach((copy) => {
        gsap.from(copy, {
          y: 48,
          autoAlpha: 0,
          duration: 1.05,
          ease: "power2.out",
          scrollTrigger: {
            trigger: copy,
            start: "top 82%",
            toggleActions: "play none none reverse",
          },
        });
      });
    },
    { scope: rootRef, dependencies: [reducedMotion] }
  );

  return (
    <section ref={rootRef} className="bg-charcoal text-white">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        <h2 className="max-w-2xl font-display text-3xl font-semibold sm:text-5xl">
          Landscapes that change as you travel.
        </h2>
        {destinations.length > 0 && (
          <p className="mt-4 max-w-xl text-sm text-white/70">
            Currently departing toward {destinations.join(", ")}.
          </p>
        )}
      </div>

      <div>
        {LANDSCAPE_SCENES.map((scene, index) => (
          <article key={scene.id} className="relative min-h-[58svh] overflow-hidden sm:min-h-[70svh] md:min-h-[85svh]">
            <ParallaxFrame
              src={scene.image}
              alt={scene.alt}
              speed={0.9}
              drift={index % 2 === 0 ? 3.5 : -3.5}
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/75 via-black/25 to-transparent md:bg-gradient-to-r md:from-black/70 md:via-black/30 md:to-transparent" />
            <div className="relative z-10 mx-auto flex min-h-[58svh] max-w-7xl items-end px-4 py-12 sm:min-h-[70svh] sm:px-6 sm:py-16 md:min-h-[85svh] lg:px-8">
              <div data-scene-copy>
                <p className="cinematic-kicker text-xs font-semibold uppercase text-accent-light">
                  {scene.kicker}
                </p>
                <h3 className="mt-3 font-display text-3xl font-semibold sm:text-6xl">
                  {scene.line}
                </h3>
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
