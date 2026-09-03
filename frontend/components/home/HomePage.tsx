import { BrandSection } from "@/components/home/BrandSection";
import { DestinationSection } from "@/components/home/DestinationSection";
import { FeaturedTrips } from "@/components/home/FeaturedTrips";
import { HeroSection } from "@/components/home/HeroSection";
import { HowItWorks } from "@/components/home/HowItWorks";
import { JourneyMoments } from "@/components/home/JourneyMoments";
import { TestimonialsSection } from "@/components/home/TestimonialsSection";
import { TrustStrip } from "@/components/home/TrustStrip";
import { WhyJoyway } from "@/components/home/WhyJoyway";
import type { PackageSummary } from "@/lib/api/packagesApi";

type HomePageProps = {
  packages: PackageSummary[];
};

export function HomePage({ packages }: HomePageProps) {
  return (
    <>
      <HeroSection />
      <TrustStrip />
      <FeaturedTrips packages={packages} />
      <JourneyMoments />
      <TestimonialsSection />
      <DestinationSection />
      <HowItWorks />
      <WhyJoyway />
      <BrandSection />
    </>
  );
}
