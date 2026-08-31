import type { Metadata } from "next";
import { getPackages } from "@/lib/api/packagesApi";
import { TripsPageContent } from "@/components/packages/TripsPageContent";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "All Trips | JOYWAY",
  description:
    "Browse group travel packages from Delhi to Nainital, Mussoorie, Rishikesh, Haridwar, Dalhousie and more.",
  openGraph: {
    title: "All Trips | JOYWAY",
    description: "Browse curated group travel packages departing from Delhi.",
    url: "https://joyway.in/trips",
  },
  alternates: {
    canonical: "https://joyway.in/trips",
  },
};

export default async function TripsPage() {
  let packages: Awaited<ReturnType<typeof getPackages>> = [];

  try {
    packages = await getPackages();
  } catch {
    packages = [];
  }

  return (
    <>
      <section className="border-b border-border bg-white">
        <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
          <p className="text-sm font-semibold uppercase tracking-widest text-forest">
            Explore
          </p>
          <h1 className="mt-2 font-display text-4xl font-semibold text-charcoal sm:text-5xl">
            All Trips
          </h1>
          <p className="mt-4 max-w-2xl text-lg text-charcoal-muted">
            Curated group journeys from Delhi — transport, stay, and unforgettable
            experiences in one package.
          </p>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
        <TripsPageContent packages={packages} />
      </section>
    </>
  );
}
