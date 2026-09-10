import Image from "next/image";
import { LANDSCAPE_SCENES } from "@/components/cinematic/journeyFromPackages";

type IndiaInMotionProps = {
  destinations: string[];
};

export function IndiaInMotion({ destinations }: Readonly<IndiaInMotionProps>) {
  return (
    <section className="bg-charcoal text-white">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        <h2 className="max-w-2xl font-display text-3xl font-semibold sm:text-5xl">
          Landscapes that change as you travel.
        </h2>
        {destinations.length > 0 && (
          <p className="mt-4 max-w-xl text-sm text-white/70">
            Currently departing toward {destinations.join(", ")}.
          </p>
        )}
      </div>

      <div>
        {LANDSCAPE_SCENES.map((scene) => (
          <article key={scene.id} className="relative min-h-[70svh] overflow-hidden md:min-h-[85svh]">
            <Image
              src={scene.image}
              alt={scene.alt}
              fill
              loading="lazy"
              sizes="100vw"
              className="object-cover journey-kenburns"
            />
            <div className="absolute inset-0 bg-gradient-to-r from-black/65 via-black/25 to-transparent" />
            <div className="relative z-10 mx-auto flex min-h-[70svh] max-w-7xl items-end px-4 py-16 sm:px-6 md:min-h-[85svh] lg:px-8">
              <div>
                <p className="cinematic-kicker text-xs font-semibold uppercase text-accent-light">
                  {scene.kicker}
                </p>
                <h3 className="mt-3 font-display text-4xl font-semibold sm:text-6xl">
                  {scene.line}
                </h3>
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
