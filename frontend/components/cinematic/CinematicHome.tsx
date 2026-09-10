import { CinematicHero } from "@/components/cinematic/CinematicHero";
import { LandscapeChapter } from "@/components/cinematic/LandscapeChapter";
import { IndiaInMotion } from "@/components/cinematic/IndiaInMotion";
import { DestinationChapter } from "@/components/cinematic/DestinationChapter";
import { TripShowcase } from "@/components/cinematic/TripShowcase";
import { StorytellingChapter } from "@/components/cinematic/StorytellingChapter";
import { JourneyTimeline } from "@/components/cinematic/JourneyTimeline";
import { BookingExperience } from "@/components/cinematic/BookingExperience";
import { BookingLookupPreview } from "@/components/cinematic/BookingLookupPreview";
import { TrustChapter } from "@/components/cinematic/TrustChapter";
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
    <div className="overflow-x-clip">
      <CinematicHero />
      <LandscapeChapter />
      <IndiaInMotion destinations={destinations} />
      <DestinationChapter destinations={destinations} packages={packages} />
      <TripShowcase packages={packages} />
      <StorytellingChapter />
      <JourneyTimeline />
      <BookingExperience />
      <BookingLookupPreview />
      <TrustChapter />
      <StoryChapter />
      <FinalJourneyCTA />
    </div>
  );
}
