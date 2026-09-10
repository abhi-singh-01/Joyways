import { HomePage } from "@/components/home/HomePage";
import { getPackages, type PackageSummary } from "@/lib/api/packagesApi";

export const revalidate = 60;

export default async function Page() {
  let packages: PackageSummary[] = [];

  try {
    packages = await getPackages();
  } catch {
    // Backend may not be running during static build — homepage still renders
    packages = [];
  }

  return <HomePage packages={packages} />;
}
