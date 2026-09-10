"use client";

import { useEffect, useRef } from "react";
import { JOURNEY_FRAME_SEQUENCE } from "@/components/cinematic/journeyFromPackages";

type JourneyCanvasSceneProps = {
  progress: number;
  enabled: boolean;
};

/**
 * Canvas frame-scrub slot. JOURNEY_FRAME_SEQUENCE is empty until real WebP/AVIF
 * frames exist; callers should keep a photographic fallback underneath.
 */
export function JourneyCanvasScene({ progress, enabled }: Readonly<JourneyCanvasSceneProps>) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const frames = JOURNEY_FRAME_SEQUENCE;

  useEffect(() => {
    if (!enabled || frames.length === 0) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const index = Math.min(frames.length - 1, Math.floor(progress * frames.length));
    const img = new window.Image();
    img.src = frames[index];
    img.onload = () => {
      const ctx = canvas.getContext("2d");
      if (!ctx) return;
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
    };
  }, [enabled, frames, progress]);

  if (!enabled || frames.length === 0) return null;

  return (
    <div aria-hidden="true" className="absolute inset-0 h-full w-full">
      <canvas
        ref={canvasRef}
        className="h-full w-full"
        width={1920}
        height={1080}
      />
    </div>
  );
}
