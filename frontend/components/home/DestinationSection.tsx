"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { InViewReveal } from "@/components/motion/InViewReveal";
import Link from "next/link";
import { ArrowRight } from "lucide-react";

const destinations = [
  { name: "Nainital", emoji: "🏔️", rotate: -3, slug: "nainital" },
  { name: "Mussoorie", emoji: "⛰️", rotate: 2, slug: "mussoorie" },
  { name: "Rishikesh", emoji: "🌊", rotate: -2, slug: "rishikesh" },
  { name: "Haridwar", emoji: "🪔", rotate: 3, slug: "haridwar" },
  { name: "Dalhousie", emoji: "❄️", rotate: -1, slug: "dalhousie" },
  { name: "Manali", emoji: "🌲", rotate: 2, slug: "manali" },
  { name: "Shimla", emoji: "🚂", rotate: -2, slug: "shimla" },
];

const marqueeItems = [...destinations, ...destinations];

// SVG route map: Delhi at center-bottom, destinations spread above
// Approximate positions in a 600×340 viewBox
const DELHI = { x: 300, y: 290, label: "Delhi" };

const destNodes: Record<string, { x: number; y: number }> = {
  Nainital: { x: 450, y: 150 },
  Mussoorie: { x: 320, y: 80 },
  Rishikesh: { x: 380, y: 110 },
  Haridwar: { x: 200, y: 130 },
  Dalhousie: { x: 80, y: 90 },
  Manali: { x: 120, y: 50 },
  Shimla: { x: 200, y: 60 },
};

function RouteMap() {
  const [hovered, setHovered] = useState<string | null>(null);

  return (
    <div className="relative mx-auto w-full max-w-2xl">
      <svg
        viewBox="0 0 600 320"
        className="h-auto w-full drop-shadow-sm"
        aria-label="Route map from Delhi to destinations"
      >
        {/* Background subtle grid */}
        <defs>
          <pattern id="mapgrid" width="30" height="30" patternUnits="userSpaceOnUse">
            <path d="M 30 0 L 0 0 0 30" fill="none" stroke="#1b4332" strokeWidth="0.3" opacity="0.08" />
          </pattern>
          <radialGradient id="delhiGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#1b4332" stopOpacity="0.25" />
            <stop offset="100%" stopColor="#1b4332" stopOpacity="0" />
          </radialGradient>
        </defs>
        <rect width="600" height="320" fill="url(#mapgrid)" rx="16" />

        {/* Route lines */}
        {destinations.map((dest) => {
          const node = destNodes[dest.name];
          const isHov = hovered === dest.name;
          return (
            <motion.line
              key={dest.name}
              x1={DELHI.x}
              y1={DELHI.y}
              x2={node.x}
              y2={node.y}
              stroke={isHov ? "#d4a574" : "#1b4332"}
              strokeWidth={isHov ? 2.5 : 1.5}
              strokeDasharray="6 4"
              opacity={hovered && !isHov ? 0.2 : 0.7}
              initial={false}
              animate={{
                stroke: isHov ? "#d4a574" : "#1b4332",
                strokeWidth: isHov ? 2.5 : 1.5,
                opacity: hovered && !isHov ? 0.2 : 0.7,
              }}
              transition={{ duration: 0.2 }}
            />
          );
        })}

        {/* Delhi hub */}
        <circle cx={DELHI.x} cy={DELHI.y} r={24} fill="url(#delhiGlow)" />
        <circle cx={DELHI.x} cy={DELHI.y} r={12} fill="#1b4332" />
        <circle cx={DELHI.x} cy={DELHI.y} r={6} fill="#fff" />
        <text
          x={DELHI.x}
          y={DELHI.y + 26}
          textAnchor="middle"
          fontSize="11"
          fontWeight="700"
          fill="#1b4332"
          fontFamily="var(--font-dm-sans, system-ui)"
        >
          Delhi
        </text>

        {/* Destination nodes */}
        {destinations.map((dest) => {
          const node = destNodes[dest.name];
          const isHov = hovered === dest.name;
          return (
            <g
              key={dest.name}
              style={{ cursor: "pointer" }}
              onMouseEnter={() => setHovered(dest.name)}
              onMouseLeave={() => setHovered(null)}
            >
              <motion.circle
                cx={node.x}
                cy={node.y}
                r={isHov ? 16 : 11}
                fill={isHov ? "#d4a574" : "#fff"}
                stroke={isHov ? "#d4a574" : "#1b4332"}
                strokeWidth="2"
                animate={{ r: isHov ? 16 : 11 }}
                transition={{ type: "spring", stiffness: 300, damping: 20 }}
              />
              <text
                x={node.x}
                y={node.y + 1}
                textAnchor="middle"
                dominantBaseline="middle"
                fontSize="12"
              >
                {dest.emoji}
              </text>
              <text
                x={node.x}
                y={node.y + (isHov ? 28 : 24)}
                textAnchor="middle"
                fontSize={isHov ? "11" : "10"}
                fontWeight={isHov ? "700" : "500"}
                fill={isHov ? "#d4a574" : "#1b4332"}
                fontFamily="var(--font-dm-sans, system-ui)"
              >
                {dest.name}
              </text>
            </g>
          );
        })}
      </svg>

      {/* Hover tooltip */}
      <AnimatePresence>
        {hovered && (
          <motion.div
            key={hovered}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 8 }}
            transition={{ duration: 0.18 }}
            className="pointer-events-none absolute bottom-2 left-1/2 -translate-x-1/2 rounded-xl border border-border bg-white px-4 py-2 text-center shadow-lg"
          >
            <p className="text-sm font-semibold text-charcoal">
              {destinations.find((d) => d.name === hovered)?.emoji} {hovered}
            </p>
            <p className="text-xs text-charcoal-muted">from Delhi</p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export function DestinationSection() {
  return (
    <section className="overflow-hidden bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <InViewReveal y={16} duration={0.55} className="mb-12 text-center">
          <h2 className="font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            Where will you go next?
          </h2>
          <p className="mt-3 text-charcoal-muted">
            Explore India&apos;s most loved hill stations — all departing from Delhi
          </p>
        </InViewReveal>

        {/* Marquee strip */}
        <div className="relative mb-12 overflow-hidden py-2">
          <div className="pointer-events-none absolute inset-y-0 left-0 z-10 w-16 bg-gradient-to-r from-surface to-transparent" />
          <div className="pointer-events-none absolute inset-y-0 right-0 z-10 w-16 bg-gradient-to-l from-surface to-transparent" />
          <div className="destination-marquee flex w-max gap-4">
            {marqueeItems.map((dest, index) => (
              <div
                key={`${dest.name}-${index}`}
                className="flex shrink-0 items-center gap-2 rounded-full border border-border bg-white px-5 py-2.5 text-sm font-medium text-charcoal shadow-sm"
              >
                <span>{dest.emoji}</span>
                {dest.name}
              </div>
            ))}
          </div>
        </div>

        {/* Interactive SVG Map */}
        <InViewReveal y={24} duration={0.65} className="mb-10">
          <div className="overflow-hidden rounded-3xl border border-border bg-white p-6 shadow-sm sm:p-8">
            <p className="mb-6 text-center text-xs font-semibold uppercase tracking-[0.18em] text-charcoal-muted">
              Hover a destination to explore
            </p>
            <RouteMap />
          </div>
        </InViewReveal>

        <div className="text-center">
          <Link
            href="/trips"
            className="inline-flex items-center gap-2 rounded-full bg-forest px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-forest-light"
          >
            See all trips
            <ArrowRight size={16} />
          </Link>
        </div>
      </div>
    </section>
  );
}
