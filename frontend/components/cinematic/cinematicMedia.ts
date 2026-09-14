export const CINEMATIC_IMAGES = {
  hero: "/images/cinematic/monastery-dusk.webp",
  landscape: "/images/cinematic/cliff-sunrise.webp",
  mountains: "/images/cinematic/green-peaks.webp",
  valleys: "/images/cinematic/snow-pass.webp",
  roads: "/images/cinematic/ridge-walker.webp",
  heritage: "/images/cinematic/monastery-day.webp",
  closing: "/images/cinematic/valley-buddha.webp",
} as const;

/** Prefer smaller decoded widths on phones; desktop caps near source width. */
export const CINEMATIC_IMAGE_SIZES =
  "(max-width: 640px) 100vw, (max-width: 1024px) 100vw, 1280px";

/** Delivery quality for Next/Image (source files are already compressed WebP). */
export const CINEMATIC_IMAGE_QUALITY = 65;
