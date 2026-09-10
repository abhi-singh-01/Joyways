import type { PackageSummary } from "@/lib/api/packagesApi";

export type LandscapeScene = {
  id: string;
  kicker: string;
  line: string;
  image: string;
  alt: string;
};

/** Mood scenes from existing JOYWAY photography — not invented destinations. */
export const LANDSCAPE_SCENES: LandscapeScene[] = [
  {
    id: "mountains",
    kicker: "MOUNTAINS",
    line: "Find your quiet.",
    image: "/images/mountains-peaks.webp",
    alt: "Himalayan mountain peaks",
  },
  {
    id: "valleys",
    kicker: "VALLEYS",
    line: "Take the long way.",
    image: "/images/mountains-lake.webp",
    alt: "Alpine lake in a mountain valley",
  },
  {
    id: "roads",
    kicker: "ROADS",
    line: "Leave the noise behind.",
    image: "/images/mountains-road.webp",
    alt: "Highland road through the mountains",
  },
  {
    id: "heritage",
    kicker: "HERITAGE",
    line: "Walk through stories.",
    image: "/images/mountains-hero.webp",
    alt: "Mountain landscape at first light",
  },
];

/**
 * Frame-sequence slot for a future canvas scrub.
 * Empty until real WebP/AVIF frames are produced.
 */
export const JOURNEY_FRAME_SEQUENCE: readonly string[] = [];

export function uniqueDestinations(packages: PackageSummary[]): string[] {
  const seen = new Set<string>();
  const list: string[] = [];
  for (const pkg of packages) {
    const destination = pkg.destination?.trim();
    if (!destination) continue;
    const key = destination.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    list.push(destination);
  }
  return list;
}

export function isBookablePrice(amount: number | null | undefined): boolean {
  return typeof amount === "number" && Number.isFinite(amount) && amount > 0;
}

export function bookableTrips(packages: PackageSummary[]): PackageSummary[] {
  return packages.filter((pkg) => isBookablePrice(pkg.pricePerPerson));
}
