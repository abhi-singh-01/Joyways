import type { Metadata } from "next";
import { Suspense } from "react";
import { getPackages } from "@/lib/api/packagesApi";
import { PageHero } from "@/components/layout/PageHero";
import { TripsPageClient } from "@/components/packages/TripsPageClient";

export const revalidate = 60;

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
      <PageHero
        illustrated
        eyebrow="Explore"
        title="All Trips"
        description="Curated group journeys from Delhi — transport, stay, and unforgettable experiences in one package."
      />

      <section className="mx-auto max-w-7xl px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
        <Suspense
          fallback={
            <p className="text-center text-charcoal-muted">Loading trips…</p>
          }
        >
          <TripsPageClient packages={packages} />
        </Suspense>
      </section>
    </>
  );
}
