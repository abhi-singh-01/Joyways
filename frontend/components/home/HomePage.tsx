import { CinematicHome } from "@/components/cinematic/CinematicHome";
import type { PackageSummary } from "@/lib/api/packagesApi";

type HomePageProps = {
  packages: PackageSummary[];
};

export function HomePage({ packages }: Readonly<HomePageProps>) {
  return <CinematicHome packages={packages} />;
}
