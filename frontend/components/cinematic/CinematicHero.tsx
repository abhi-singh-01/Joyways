import { ArrowRight } from "lucide-react";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { ParallaxFrame } from "@/components/cinematic/ParallaxFrame";
import { CINEMATIC_IMAGES } from "@/components/cinematic/cinematicMedia";

export function CinematicHero() {
  return (
    <section className="relative min-h-[100svh] overflow-hidden bg-forest-dark text-white">
      <ParallaxFrame
        src={CINEMATIC_IMAGES.hero}
        alt="Hilltop monastery glowing at dusk above a Himalayan valley"
        priority
        speed={0.85}
        drift={-3}
        imageClassName="object-[center_40%] sm:object-center md:object-top"
        pinTop
      />
      <div className="absolute inset-0 bg-gradient-to-b from-black/25 via-black/30 to-black/60" />

      <div className="relative z-10 mx-auto flex min-h-[100svh] max-w-7xl flex-col justify-end px-4 pb-[calc(env(safe-area-inset-bottom,0px)+7.5rem)] pt-24 sm:px-6 sm:pb-20 lg:justify-center lg:px-8 lg:pb-24 lg:pt-24">
        <h1 className="max-w-3xl font-display text-4xl font-semibold leading-[0.95] tracking-tight drop-shadow-[0_2px_20px_rgba(0,0,0,0.45)] sm:text-6xl lg:text-8xl">
          Journeys
          <br />
          that stay
          <br />
          with you.
        </h1>
        <p className="mt-6 max-w-xl text-base leading-relaxed text-white/90 drop-shadow-[0_1px_12px_rgba(0,0,0,0.4)] sm:text-lg">
          Curated travel experiences across India&apos;s most breathtaking destinations.
        </p>
        <div className="mt-8 flex w-full flex-col gap-3 sm:w-auto sm:flex-row sm:items-center">
          <ButtonLink href="/trips" size="lg" className="w-full sm:w-auto">
            Explore trips
            <ArrowRight size={18} className="ml-2" />
          </ButtonLink>
          <ButtonLink
            href="/bookings/lookup"
            variant="outline"
            size="lg"
            className="w-full border-forest bg-white/90 text-forest hover:bg-forest hover:text-white sm:w-auto"
          >
            Find a booking
          </ButtonLink>
        </div>
      </div>
    </section>
  );
}
