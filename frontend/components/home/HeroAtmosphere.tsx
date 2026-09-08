"use client";

import dynamic from "next/dynamic";
import { useSyncExternalStore } from "react";
import { useReducedMotion } from "@/lib/hooks/useReducedMotion";

const HeroScene = dynamic(
  () => import("./HeroScene").then((mod) => mod.HeroScene),
  { ssr: false, loading: () => null }
);

function subscribeDesktop(onChange: () => void) {
  const media = window.matchMedia("(min-width: 1024px)");
  media.addEventListener("change", onChange);
  return () => media.removeEventListener("change", onChange);
}

function getDesktopSnapshot() {
  return window.matchMedia("(min-width: 1024px)").matches;
}

export function HeroAtmosphere() {
  const mounted = useSyncExternalStore(
    () => () => {},
    () => true,
    () => false
  );
  const desktop = useSyncExternalStore(
    subscribeDesktop,
    getDesktopSnapshot,
    () => false
  );
  const reducedMotion = useReducedMotion();

  if (!mounted || !desktop || reducedMotion) {
    return null;
  }

  return (
    <div
      className="pointer-events-none absolute inset-0 z-[1] hidden lg:block"
      aria-hidden
    >
      <HeroScene />
    </div>
  );
}
