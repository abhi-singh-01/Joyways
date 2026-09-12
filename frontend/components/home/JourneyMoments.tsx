import Image from "next/image";
import { cn } from "@/lib/utils";
import { CINEMATIC_IMAGES } from "@/components/cinematic/cinematicMedia";
import { InViewReveal } from "@/components/motion/InViewReveal";
import { MotionHoverLift } from "@/components/motion/MotionHoverLift";

type JourneyScene = {
  title: string;
  subtitle: string;
  image: string;
  theme: "forest" | "river" | "road";
};

const scenes: JourneyScene[] = [
  {
    title: "Mountain trails",
    subtitle: "Himalayan escapes from Delhi",
    image: CINEMATIC_IMAGES.mountains,
    theme: "forest",
  },
  {
    title: "Lakes & valleys",
    subtitle: "Rishikesh & hill-station routes",
    image: CINEMATIC_IMAGES.valleys,
    theme: "river",
  },
  {
    title: "Open roads",
    subtitle: "Group travel, shared memories",
    image: CINEMATIC_IMAGES.roads,
    theme: "road",
  },
];

const themeGlow: Record<JourneyScene["theme"], string> = {
  forest: "from-forest/40 to-transparent",
  river: "from-teal-600/35 to-transparent",
  road: "from-accent/35 to-transparent",
};

/** Cards sit in max-w-7xl 3-col grid (~400px wide), not full viewport thirds. */
const journeyImageSizes =
  "(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 400px";

export function JourneyMoments() {
  return (
    <section className="relative overflow-hidden bg-charcoal py-20 text-white sm:py-28">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--color-forest)_0%,_transparent_55%)] opacity-40" />

      <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <InViewReveal
          y={20}
          duration={0.6}
          className="mx-auto mb-12 max-w-2xl text-center"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-accent">
            Feel the journey
          </p>
          <h2 className="mt-3 font-display text-3xl font-semibold sm:text-4xl">
            Moments on the road
          </h2>
          <p className="mt-4 text-white/70">
            Mountain peaks, alpine lakes, and highland roads — the landscapes that
            define a Joyway trip.
          </p>
        </InViewReveal>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {scenes.map((scene, index) => (
            <InViewReveal
              key={scene.title}
              y={32}
              duration={0.55}
              delay={index * 0.12}
              viewport={{ once: true, margin: "-120px 0px" }}
              className="group relative overflow-hidden rounded-3xl border border-white/10 bg-charcoal shadow-lg"
            >
              <MotionHoverLift className="h-full">
                <div className="relative aspect-[4/5] sm:aspect-[3/4]">
                  <Image
                    src={scene.image}
                    alt={scene.title}
                    fill
                    loading="lazy"
                    fetchPriority="low"
                    sizes={journeyImageSizes}
                    className="object-cover transition-transform duration-700 group-hover:scale-105"
                  />
                  <div
                    className={cn(
                      "pointer-events-none absolute inset-0 bg-gradient-to-t",
                      themeGlow[scene.theme]
                    )}
                  />
                  <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-charcoal/80 via-charcoal/20 to-transparent" />
                </div>
                <div className="absolute bottom-0 left-0 right-0 p-5 text-white">
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent-light">
                    {scene.subtitle}
                  </p>
                  <h3 className="mt-1 font-display text-xl font-semibold">{scene.title}</h3>
                </div>
              </MotionHoverLift>
            </InViewReveal>
          ))}
        </div>
      </div>
    </section>
  );
}
