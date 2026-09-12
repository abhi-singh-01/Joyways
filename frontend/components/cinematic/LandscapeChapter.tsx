"use client";

import { useRef } from "react";
import Image from "next/image";
import { useGSAP } from "@gsap/react";
import { CINEMATIC_IMAGES, CINEMATIC_IMAGE_SIZES } from "@/components/cinematic/cinematicMedia";
import { usePrefersReducedMotion } from "@/components/cinematic/usePrefersReducedMotion";
import { gsap, registerCinematicGsap } from "@/components/cinematic/gsapSetup";
import { cn } from "@/lib/utils";

const LINES = [
  "Some journeys begin with a destination.",
  "They begin with a feeling.",
  "Freedom.",
];

export function LandscapeChapter() {
  const reducedMotion = usePrefersReducedMotion();
  const sectionRef = useRef<HTMLElement>(null);
  const pinRef = useRef<HTMLDivElement>(null);
  const mediaRef = useRef<HTMLDivElement>(null);
  const lineRefs = useRef<Array<HTMLParagraphElement | null>>([]);

  useGSAP(
    () => {
      registerCinematicGsap();
      const section = sectionRef.current;
      const pin = pinRef.current;
      const media = mediaRef.current;
      const lines = lineRefs.current.filter(Boolean) as HTMLParagraphElement[];
      if (!section || !pin || !media || reducedMotion) return;

      const mediaQuery = gsap.matchMedia();
      mediaQuery.add("(min-width: 768px)", () => {
        gsap.set(lines, { autoAlpha: 0, y: 28 });
        if (lines[0]) gsap.set(lines[0], { autoAlpha: 1, y: 0 });

        const timeline = gsap.timeline({
          scrollTrigger: {
            trigger: section,
            start: "top top",
            end: "+=220%",
            pin,
            scrub: 0.85,
            anticipatePin: 1,
          },
        });

        timeline.fromTo(
          media,
          { yPercent: -8, scale: 1.16, xPercent: -4 },
          { yPercent: 10, scale: 1.05, xPercent: 4, ease: "none" },
          0
        );

        lines.forEach((line, index) => {
          if (index === 0) return;
          const at = index / lines.length;
          const previous = lines[index - 1];
          if (previous) {
            timeline.to(previous, { autoAlpha: 0, y: -22, duration: 0.18, ease: "power1.out" }, at);
          }
          timeline.to(line, { autoAlpha: 1, y: 0, duration: 0.18, ease: "power1.out" }, at);
        });

        return () => {
          gsap.set(lines, { clearProps: "all" });
          gsap.set(media, { clearProps: "all" });
        };
      });

      return () => mediaQuery.revert();
    },
    { scope: sectionRef, dependencies: [reducedMotion] }
  );

  return (
    <section ref={sectionRef} className="relative bg-forest-dark" aria-label="Into the landscape">
      <div ref={pinRef} className="relative flex min-h-[70svh] items-end overflow-hidden md:min-h-[100svh]">
        <div className="absolute inset-0 overflow-hidden">
          <div ref={mediaRef} className="absolute inset-0 will-change-transform md:inset-[-10%]">
            <Image
              src={CINEMATIC_IMAGES.landscape}
              alt="Sunlit cliffs rising through cloud at sunrise"
              fill
              loading="lazy"
              sizes={CINEMATIC_IMAGE_SIZES}
              className="h-full w-full object-cover object-[center_45%] md:object-center"
            />
          </div>
        </div>
        <div className="absolute inset-0 bg-gradient-to-t from-black/75 via-black/30 to-black/15" />
        <div className="relative z-10 mx-auto w-full max-w-7xl px-4 pb-[calc(env(safe-area-inset-bottom,0px)+6.5rem)] pt-20 sm:px-6 sm:py-16 md:pb-[calc(env(safe-area-inset-bottom,0px)+9rem)] lg:px-8">
          <div className="relative max-w-2xl">
            {LINES.map((line, index) => (
              <p
                key={line}
                ref={(node) => {
                  lineRefs.current[index] = node;
                }}
                className={cn(
                  "font-display text-2xl font-semibold leading-tight text-white sm:text-3xl md:text-5xl",
                  !reducedMotion && "md:absolute md:inset-x-0 md:top-0",
                  index > 0 && (reducedMotion ? "mt-3" : "mt-3 md:mt-0"),
                  index > 0 && !reducedMotion && "md:opacity-0"
                )}
              >
                {line}
              </p>
            ))}
            {!reducedMotion && (
              <p
                className="invisible hidden font-display text-3xl font-semibold leading-tight sm:text-5xl md:block"
                aria-hidden
              >
                {LINES[0]}
              </p>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
