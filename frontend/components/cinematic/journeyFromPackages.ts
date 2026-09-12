import type { PackageSummary } from "@/lib/api/packagesApi";
import { CINEMATIC_IMAGES } from "@/components/cinematic/cinematicMedia";

export type LandscapeScene = {
  id: string;
  kicker: string;
  line: string;
  image: string;
  alt: string;
};

/** Mood scenes from JOYWAY cinematic photography — not invented destinations. */
export const LANDSCAPE_SCENES: LandscapeScene[] = [
  {
    id: "mountains",
    kicker: "MOUNTAINS",
    line: "Find your quiet.",
    image: CINEMATIC_IMAGES.mountains,
    alt: "Sunlit mountain ridge above a green valley",
  },
  {
    id: "valleys",
    kicker: "HIGH PASSES",
    line: "Take the long way.",
    image: CINEMATIC_IMAGES.valleys,
    alt: "Snow slope and high peaks under a blue sky",
  },
  {
    id: "roads",
    kicker: "TRAILS",
    line: "Leave the noise behind.",
    image: CINEMATIC_IMAGES.roads,
    alt: "Walker on a high ridgeline above the clouds",
  },
  {
    id: "heritage",
    kicker: "HERITAGE",
    line: "Walk through stories.",
    image: CINEMATIC_IMAGES.heritage,
    alt: "Hilltop monastery against snow mountains",
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
