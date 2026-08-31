"use client";

import { useMemo } from "react";
import { cn } from "@/lib/utils";
import { useReducedMotion } from "@/lib/hooks/useReducedMotion";

type SparklesProps = {
  className?: string;
  count?: number;
};

export function Sparkles({ className, count = 24 }: SparklesProps) {
  const reducedMotion = useReducedMotion();

  const particles = useMemo(
    () =>
      Array.from({ length: count }, (_, i) => ({
        id: i,
        left: `${(i * 17 + 7) % 100}%`,
        top: `${(i * 23 + 11) % 100}%`,
        size: 2 + (i % 3),
        delay: `${(i % 8) * 0.4}s`,
        duration: `${2.5 + (i % 5) * 0.6}s`,
      })),
    [count]
  );

  if (reducedMotion) {
    return null;
  }

  return (
    <div className={cn("pointer-events-none absolute inset-0 overflow-hidden", className)} aria-hidden>
      {particles.map((p) => (
        <span
          key={p.id}
          className="sparkle-particle absolute rounded-full bg-accent/80"
          style={{
            left: p.left,
            top: p.top,
            width: p.size,
            height: p.size,
            animationDelay: p.delay,
            animationDuration: p.duration,
          }}
        />
      ))}
    </div>
  );
}
