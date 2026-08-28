"use client";

import dynamic from "next/dynamic";
import { useSyncExternalStore } from "react";

const CanvasScene = dynamic(
  () => import("./TripHeroVisual").then((m) => m.TripHeroVisual),
  {
    ssr: false,
    loading: () => (
      <div className="h-full w-full bg-gradient-to-br from-forest/5 to-transparent" />
    ),
  }
);

function subscribeReducedMotion(cb: () => void) {
  const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
  mq.addEventListener("change", cb);
  return () => mq.removeEventListener("change", cb);
}

function getReducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

export function TripHeroAccent() {
  const reducedMotion = useSyncExternalStore(
    subscribeReducedMotion,
    getReducedMotion,
    () => true
  );

  if (reducedMotion) {
    return (
      <div
        className="pointer-events-none absolute -right-8 -top-8 h-48 w-48 rounded-full bg-forest/5 blur-3xl"
        aria-hidden
      />
    );
  }

  return (
    <div className="pointer-events-none absolute -right-4 top-0 h-40 w-40 opacity-60 md:h-56 md:w-56">
      <CanvasScene />
    </div>
  );
}
