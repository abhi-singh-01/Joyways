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

function StaticAccent() {
  return (
    <div
      className="pointer-events-none absolute -right-8 -top-8 h-48 w-48 rounded-full bg-forest/5 blur-3xl"
      aria-hidden
    />
  );
}

function AnimatedAccent() {
  return (
    <div className="pointer-events-none absolute -right-4 top-0 h-40 w-40 opacity-60 md:h-56 md:w-56">
      <CanvasScene />
    </div>
  );
}

function subscribeReducedMotion(onStoreChange: () => void) {
  const mediaQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
  mediaQuery.addEventListener("change", onStoreChange);
  return () => mediaQuery.removeEventListener("change", onStoreChange);
}

function getReducedMotionSnapshot() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function getReducedMotionServerSnapshot() {
  return true;
}

export function TripHeroAccent() {
  const mounted = useSyncExternalStore(
    () => () => {},
    () => true,
    () => false
  );
  const reducedMotion = useSyncExternalStore(
    subscribeReducedMotion,
    getReducedMotionSnapshot,
    getReducedMotionServerSnapshot
  );

  if (!mounted || reducedMotion) {
    return <StaticAccent />;
  }

  return <AnimatedAccent />;
}
