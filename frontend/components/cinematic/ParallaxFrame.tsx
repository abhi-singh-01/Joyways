"use client";

import { useRef } from "react";
import Image from "next/image";
import { useGSAP } from "@gsap/react";
import { cn } from "@/lib/utils";
import { usePrefersReducedMotion } from "@/components/cinematic/usePrefersReducedMotion";
import { gsap, registerCinematicGsap } from "@/components/cinematic/gsapSetup";
import { CINEMATIC_IMAGE_SIZES } from "@/components/cinematic/cinematicMedia";

type ParallaxFrameProps = {
  src: string;
  alt: string;
  priority?: boolean;
  sizes?: string;
  className?: string;
  /** Vertical travel strength. */
  speed?: number;
  /** Horizontal 2D drift in percent. */
  drift?: number;
  /** Extra classes for the Next/Image (object-position). */
  imageClassName?: string;
  /** Keep the top of the photo visible (hero under a fixed navbar). */
  pinTop?: boolean;
};

export function ParallaxFrame({
  src,
  alt,
  priority = false,
  sizes = CINEMATIC_IMAGE_SIZES,
  className,
  speed = 1,
  drift = 0,
  imageClassName,
  pinTop = false,
}: Readonly<ParallaxFrameProps>) {
  const rootRef = useRef<HTMLDivElement>(null);
  const mediaRef = useRef<HTMLDivElement>(null);
  const reducedMotion = usePrefersReducedMotion();

  useGSAP(
    () => {
      registerCinematicGsap();
      const root = rootRef.current;
      const media = mediaRef.current;
      if (!root || !media || reducedMotion) return;

      const mediaQuery = gsap.matchMedia();
      mediaQuery.add("(min-width: 768px)", () => {
        const xDrift = drift;
        const yTravel = 11 * speed;
        const startY = pinTop ? 0 : -yTravel;

        gsap.fromTo(
          media,
          { yPercent: startY, xPercent: -xDrift, scale: pinTop ? 1.12 : 1.2 },
          {
            yPercent: yTravel,
            xPercent: xDrift,
            scale: 1.06,
            ease: "none",
            scrollTrigger: {
              trigger: root,
              start: "top bottom",
              end: "bottom top",
              scrub: 0.75,
            },
          }
        );

        return () => {
          gsap.set(media, { clearProps: "transform" });
        };
      });

      return () => mediaQuery.revert();
    },
    { scope: rootRef, dependencies: [reducedMotion, src, speed, drift, pinTop] }
  );

  return (
    <div
      ref={rootRef}
      className={cn("absolute inset-0 overflow-hidden bg-forest-dark", className)}
    >
      <div
        ref={mediaRef}
        className={cn(
          "absolute inset-0 will-change-transform md:inset-[-8%]",
          pinTop && "md:inset-x-[-8%] md:top-0 md:-bottom-[8%] md:origin-top"
        )}
      >
        <Image
          src={src}
          alt={alt}
          fill
          priority={priority}
          loading={priority ? "eager" : "lazy"}
          sizes={sizes}
          className={cn("h-full w-full object-cover object-center", imageClassName)}
        />
      </div>
    </div>
  );
}
