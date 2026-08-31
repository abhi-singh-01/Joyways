"use client";

import { useRef } from "react";
import { motion, useInView } from "framer-motion";
import { cn } from "@/lib/utils";
import { useReducedMotion } from "@/lib/hooks/useReducedMotion";

type JourneyClip = {
  title: string;
  subtitle: string;
  /** Silent loop clip (~5–15s). Replace with self-hosted /public/videos/ files in production. */
  src: string;
  poster: string;
  theme: "forest" | "river" | "road";
};

const clips: JourneyClip[] = [
  {
    title: "Mountain trails",
    subtitle: "Himalayan escapes from Delhi",
    src: "https://videos.pexels.com/video-files/1526909/1526909-hd_1280_720_24fps.mp4",
    poster: "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&q=80",
    theme: "forest",
  },
  {
    title: "River journeys",
    subtitle: "Rishikesh & spiritual routes",
    src: "https://videos.pexels.com/video-files/3571264/3571264-hd_1280_720_25fps.mp4",
    poster: "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80",
    theme: "river",
  },
  {
    title: "Open roads",
    subtitle: "Group travel, shared memories",
    src: "https://videos.pexels.com/video-files/4763824/4763824-hd_1280_720_25fps.mp4",
    poster: "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800&q=80",
    theme: "road",
  },
];

const themeGlow: Record<JourneyClip["theme"], string> = {
  forest: "from-forest/40 to-transparent",
  river: "from-teal-600/35 to-transparent",
  road: "from-accent/35 to-transparent",
};

function VideoCard({ clip, index }: { clip: JourneyClip; index: number }) {
  const ref = useRef<HTMLDivElement>(null);
  const inView = useInView(ref, { amount: 0.45, once: false });
  const reducedMotion = useReducedMotion();

  return (
    <motion.article
      ref={ref}
      initial={{ opacity: 0, y: 32 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-40px" }}
      transition={{ duration: 0.55, delay: index * 0.12 }}
      whileHover={{ y: -6 }}
      className="group relative overflow-hidden rounded-3xl border border-border bg-charcoal shadow-lg"
    >
      <div className="relative aspect-[4/5] sm:aspect-[3/4]">
        {reducedMotion ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={clip.poster}
            alt={clip.title}
            className="h-full w-full object-cover"
          />
        ) : (
          <video
            className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
            src={clip.src}
            poster={clip.poster}
            muted
            loop
            playsInline
            autoPlay={inView}
            preload="metadata"
            aria-label={clip.title}
          />
        )}
        <div
          className={cn(
            "pointer-events-none absolute inset-0 bg-gradient-to-t",
            themeGlow[clip.theme]
          )}
        />
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-charcoal/80 via-charcoal/20 to-transparent" />
      </div>

      <div className="absolute bottom-0 left-0 right-0 p-5 text-white">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent-light">
          {clip.subtitle}
        </p>
        <h3 className="mt-1 font-display text-xl font-semibold">{clip.title}</h3>
      </div>

      <div className="pointer-events-none absolute left-4 top-4 rounded-full border border-white/20 bg-black/30 px-3 py-1 text-[10px] font-medium uppercase tracking-wider text-white/90 backdrop-blur-sm">
        Silent preview
      </div>
    </motion.article>
  );
}

export function JourneyMoments() {
  return (
    <section className="relative overflow-hidden bg-charcoal py-20 text-white sm:py-28">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--color-forest)_0%,_transparent_55%)] opacity-40" />

      <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mx-auto mb-12 max-w-2xl text-center"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-accent">
            Feel the journey
          </p>
          <h2 className="mt-3 font-display text-3xl font-semibold sm:text-4xl">
            Moments on the road
          </h2>
          <p className="mt-4 text-white/70">
            Short silent previews — mountains, rivers, and open highways that mirror
            the Joyway experience.
          </p>
        </motion.div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {clips.map((clip, index) => (
            <VideoCard key={clip.title} clip={clip} index={index} />
          ))}
        </div>
      </div>
    </section>
  );
}
