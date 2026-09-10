import Image from "next/image";
import { ArrowRight } from "lucide-react";
import { ButtonLink } from "@/components/ui/ButtonLink";

export function FinalJourneyCTA() {
  return (
    <section className="relative min-h-[88svh] overflow-hidden bg-forest-dark text-white">
      <Image
        src="/images/mountains-hero.webp"
        alt="Mountain landscape at first light"
        fill
        loading="lazy"
        sizes="100vw"
        className="object-cover"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-black/45 to-black/70" />
      <div className="relative z-10 mx-auto flex min-h-[88svh] max-w-7xl flex-col justify-end px-4 pb-[calc(env(safe-area-inset-bottom,0px)+10rem)] sm:px-6 sm:pb-20 lg:px-8">
        <h2 className="max-w-3xl font-display text-4xl font-semibold leading-[0.95] sm:text-6xl lg:text-7xl">
          Your next journey
          <br />
          starts here.
        </h2>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
          <ButtonLink
            href="/trips"
            size="lg"
            className="bg-white text-forest hover:bg-accent-light hover:text-forest-dark"
          >
            Explore trips
            <ArrowRight size={18} className="ml-2" />
          </ButtonLink>
          <ButtonLink
            href="/bookings/lookup"
            variant="outline"
            size="lg"
            className="border-white/70 bg-transparent text-white hover:bg-white hover:text-forest"
          >
            Find a booking
          </ButtonLink>
        </div>
      </div>
    </section>
  );
}
