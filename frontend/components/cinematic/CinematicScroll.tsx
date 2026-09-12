"use client";

import { useEffect, useState } from "react";
import Lenis from "lenis";
import "lenis/dist/lenis.css";
import { usePrefersReducedMotion } from "@/components/cinematic/usePrefersReducedMotion";
import { gsap, ScrollTrigger, registerCinematicGsap } from "@/components/cinematic/gsapSetup";

type CinematicScrollProps = {
  children: React.ReactNode;
};

const DESKTOP_FINE_POINTER = "(min-width: 768px) and (pointer: fine)";

export function CinematicScroll({ children }: Readonly<CinematicScrollProps>) {
  const reducedMotion = usePrefersReducedMotion();
  const [smoothScroll, setSmoothScroll] = useState(false);

  useEffect(() => {
    const query = window.matchMedia(DESKTOP_FINE_POINTER);
    const sync = () => setSmoothScroll(query.matches && !reducedMotion);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, [reducedMotion]);

  useEffect(() => {
    registerCinematicGsap();
    document.documentElement.classList.add("overflow-x-clip");

    if (!smoothScroll) {
      ScrollTrigger.refresh();
      return () => {
        document.documentElement.classList.remove("overflow-x-clip");
      };
    }

    const lenis = new Lenis({
      lerp: 0.085,
      wheelMultiplier: 0.86,
      touchMultiplier: 1,
      autoRaf: false,
    });

    lenis.on("scroll", ScrollTrigger.update);

    const onTick = (time: number) => {
      lenis.raf(time * 1000);
    };
    gsap.ticker.add(onTick);
    gsap.ticker.lagSmoothing(0);

    const onResize = () => ScrollTrigger.refresh();
    window.addEventListener("resize", onResize);
    const refresh = window.requestAnimationFrame(() => ScrollTrigger.refresh());

    return () => {
      window.cancelAnimationFrame(refresh);
      window.removeEventListener("resize", onResize);
      gsap.ticker.remove(onTick);
      lenis.destroy();
      document.documentElement.classList.remove("overflow-x-clip");
    };
  }, [smoothScroll]);

  return <div className="cinematic-scroll">{children}</div>;
}
