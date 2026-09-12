import { ArrowRight } from "lucide-react";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { ParallaxFrame } from "@/components/cinematic/ParallaxFrame";
import { CINEMATIC_IMAGES } from "@/components/cinematic/cinematicMedia";

export function FinalJourneyCTA() {
  return (
    <section className="relative min-h-[72svh] overflow-hidden bg-forest-dark text-white md:min-h-[88svh]">
      <ParallaxFrame
        src={CINEMATIC_IMAGES.closing}
        alt="Travellers looking out from a hillside shrine toward snow mountains"
        speed={0.7}
        drift={-2}
      />
      <div className="absolute inset-0 bg-gradient-to-b from-black/35 via-black/45 to-black/70" />
      <div className="relative z-10 mx-auto flex min-h-[72svh] max-w-7xl flex-col justify-end px-4 pb-[calc(env(safe-area-inset-bottom,0px)+8.5rem)] sm:px-6 sm:pb-20 md:min-h-[88svh] lg:px-8">
        <h2 className="max-w-3xl font-display text-3xl font-semibold leading-[0.95] sm:text-6xl lg:text-7xl">
          Your next journey
          <br />
          starts here.
        </h2>
        <div className="mt-8 flex w-full flex-col gap-3 sm:w-auto sm:flex-row sm:items-center">
          <ButtonLink
            href="/trips"
            size="lg"
            className="w-full bg-white text-forest hover:bg-accent-light hover:text-forest-dark sm:w-auto"
          >
            Explore trips
            <ArrowRight size={18} className="ml-2" />
          </ButtonLink>
          <ButtonLink
            href="/bookings/lookup"
            variant="outline"
            size="lg"
            className="w-full border-white/70 bg-transparent text-white hover:bg-white hover:text-forest sm:w-auto"
          >
            Find a booking
          </ButtonLink>
        </div>
      </div>
    </section>
  );
}
