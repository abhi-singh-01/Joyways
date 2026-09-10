import Image from "next/image";
import { ArrowRight } from "lucide-react";
import { ButtonLink } from "@/components/ui/ButtonLink";

const heroImageSizes =
  "(max-width: 640px) 100vw, (max-width: 1920px) 100vw, 1920px";

export function CinematicHero() {
  return (
    <section className="relative min-h-[100svh] overflow-hidden bg-forest-dark text-white">
      <Image
        src="/images/mountains-hero.webp"
        alt="Mountain landscape at dawn — the start of a JOYWAY journey"
        fill
        priority
        sizes={heroImageSizes}
        className="hero-parallax-img object-cover object-center"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-black/20 via-black/25 to-black/55" />

      <div className="relative z-10 mx-auto flex min-h-[100svh] max-w-7xl flex-col justify-end px-4 pb-[calc(env(safe-area-inset-bottom,0px)+12.5rem)] pt-28 sm:px-6 sm:pb-20 lg:justify-center lg:px-8 lg:pb-24 lg:pt-24">
        <h1 className="max-w-3xl font-display text-5xl font-semibold leading-[0.95] tracking-tight drop-shadow-[0_2px_20px_rgba(0,0,0,0.45)] sm:text-6xl lg:text-8xl">
          Journeys
          <br />
          that stay
          <br />
          with you.
        </h1>
        <p className="mt-6 max-w-xl text-base leading-relaxed text-white/90 drop-shadow-[0_1px_12px_rgba(0,0,0,0.4)] sm:text-lg">
          Curated travel experiences across India&apos;s most breathtaking destinations.
        </p>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
          <ButtonLink href="/trips" size="lg">
            Explore trips
            <ArrowRight size={18} className="ml-2" />
          </ButtonLink>
          <ButtonLink
            href="/bookings/lookup"
            variant="outline"
            size="lg"
            className="border-forest bg-white/90 text-forest hover:bg-forest hover:text-white"
          >
            Find a booking
          </ButtonLink>
        </div>
      </div>
    </section>
  );
}
