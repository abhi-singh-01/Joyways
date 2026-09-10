"use client";

import { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { JourneyCanvasScene } from "@/components/cinematic/JourneyCanvasScene";
import { JOURNEY_FRAME_SEQUENCE } from "@/components/cinematic/journeyFromPackages";
import { usePrefersReducedMotion } from "@/components/cinematic/usePrefersReducedMotion";
import { cn } from "@/lib/utils";

function lineIndexFromProgress(progress: number): number {
  if (progress < 0.33) return 0;
  if (progress < 0.66) return 1;
  return 2;
}

const LINES = [
  "Some journeys begin with a destination.",
  "They begin with a feeling.",
  "Freedom.",
];

/**
 * Pinned landscape chapter. Canvas frame-scrub is ready when
 * JOURNEY_FRAME_SEQUENCE is populated; until then a still landscape is used.
 */
export function LandscapeChapter() {
  const reducedMotion = usePrefersReducedMotion();
  const trackRef = useRef<HTMLElement>(null);
  const [lineIndex, setLineIndex] = useState(0);
  const [progress, setProgress] = useState(0);
  const hasFrames = JOURNEY_FRAME_SEQUENCE.length > 0;

  useEffect(() => {
    const track = trackRef.current;
    if (!track || reducedMotion) return;
    let frame = 0;

    const update = () => {
      frame = 0;
      const rect = track.getBoundingClientRect();
      const height = track.offsetHeight - window.innerHeight;
      if (height <= 0) return;
      const next = Math.min(1, Math.max(0, -rect.top / height));
      setLineIndex((current) => {
        const index = lineIndexFromProgress(next);
        return current === index ? current : index;
      });
      if (hasFrames) setProgress(next);
    };

    const onScroll = () => {
      if (frame) return;
      frame = window.requestAnimationFrame(update);
    };

    update();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => {
      window.removeEventListener("scroll", onScroll);
      if (frame) window.cancelAnimationFrame(frame);
    };
  }, [reducedMotion, hasFrames]);

  return (
    <section
      ref={trackRef}
      className="relative bg-forest-dark md:h-[220vh]"
      aria-label="Into the landscape"
    >
      <div className="relative flex min-h-[100svh] items-end overflow-hidden md:sticky md:top-0 md:h-screen">
        <div className="absolute inset-0">
          <Image
            src="/images/mountains-peaks.webp"
            alt="Mountains opening into a high valley"
            fill
            loading="lazy"
            sizes="100vw"
            className="object-cover"
          />
          <JourneyCanvasScene progress={progress} enabled={!reducedMotion && JOURNEY_FRAME_SEQUENCE.length > 0} />
        </div>
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/25 to-black/20" />
        <div className="relative z-10 mx-auto w-full max-w-7xl px-4 pb-[calc(env(safe-area-inset-bottom,0px)+9rem)] pt-16 sm:px-6 sm:py-16 lg:px-8">
          <div className="relative max-w-2xl">
            {LINES.map((line, index) => (
              <p
                key={line}
                className={cn(
                  "font-display text-3xl font-semibold leading-tight text-white sm:text-5xl md:transition-opacity md:duration-500",
                  !reducedMotion && "md:absolute md:inset-x-0 md:top-0",
                  index > 0 && (reducedMotion ? "mt-4" : "mt-4 md:mt-0"),
                  reducedMotion || index === lineIndex ? "md:opacity-100" : "md:opacity-0"
                )}
              >
                {line}
              </p>
            ))}
            {!reducedMotion && (
              <p className="invisible hidden font-display text-3xl font-semibold leading-tight sm:text-5xl md:block" aria-hidden>
                {LINES[0]}
              </p>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
