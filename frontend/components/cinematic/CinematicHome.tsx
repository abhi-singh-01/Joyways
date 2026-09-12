import { CinematicHero } from "@/components/cinematic/CinematicHero";
import { CinematicScroll } from "@/components/cinematic/CinematicScroll";
import { LandscapeChapter } from "@/components/cinematic/LandscapeChapter";
import { IndiaInMotion } from "@/components/cinematic/IndiaInMotion";
import { DestinationChapter } from "@/components/cinematic/DestinationChapter";
import { StorytellingChapter } from "@/components/cinematic/StorytellingChapter";
import { JourneyTimeline } from "@/components/cinematic/JourneyTimeline";
import { StoryChapter } from "@/components/cinematic/StoryChapter";
import { FinalJourneyCTA } from "@/components/cinematic/FinalJourneyCTA";
import { uniqueDestinations } from "@/components/cinematic/journeyFromPackages";
import type { PackageSummary } from "@/lib/api/packagesApi";

type CinematicHomeProps = {
  packages: PackageSummary[];
};

export function CinematicHome({ packages }: Readonly<CinematicHomeProps>) {
  const destinations = uniqueDestinations(packages);

  return (
    <CinematicScroll>
      <CinematicHero />
      <LandscapeChapter />
      <IndiaInMotion destinations={destinations} />
      <DestinationChapter destinations={destinations} packages={packages} />
      <StorytellingChapter />
      <JourneyTimeline />
      <StoryChapter />
      <FinalJourneyCTA />
    </CinematicScroll>
  );
}
