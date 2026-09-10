import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getPackageBySlug, getPackageReviews, getPackages } from "@/lib/api/packagesApi";
import { PackageDetailView } from "@/components/packages/PackageDetailView";

export const revalidate = 60;

type PageProps = {
  params: Promise<{ slug: string }>;
};

export async function generateStaticParams() {
  try {
    const packages = await getPackages();
    return packages.map((pkg) => ({ slug: pkg.slug }));
  } catch {
    return [];
  }
}

export async function generateMetadata({ params }: Readonly<PageProps>): Promise<Metadata> {
  const { slug } = await params;

  try {
    const pkg = await getPackageBySlug(slug);
    const title = `${pkg.destination} Trip | JOYWAY`;
    const description =
      pkg.shortDescription ||
      `Join our group trip from ${pkg.departureCity} to ${pkg.destination}.`;

    return {
      title,
      description,
      openGraph: {
        title,
        description,
        url: `https://joyway.in/trips/${slug}`,
        images: pkg.images[0]?.imageUrl
          ? [{ url: pkg.images[0].imageUrl, alt: pkg.destination }]
          : undefined,
      },
      alternates: {
        canonical: `https://joyway.in/trips/${slug}`,
      },
    };
  } catch {
    return { title: "Trip Not Found | JOYWAY" };
  }
}

export default async function TripDetailPage({ params }: Readonly<PageProps>) {
  const { slug } = await params;

  let pkg;
  try {
    pkg = await getPackageBySlug(slug);
  } catch {
    notFound();
  }

  let reviews: Awaited<ReturnType<typeof getPackageReviews>> = {
    averageRating: null,
    reviewCount: 0,
    reviews: [],
  };
  try {
    reviews = await getPackageReviews(slug);
  } catch {
    // Trip page still renders if reviews cannot be loaded.
  }

  return <PackageDetailView pkg={pkg} reviews={reviews} />;
}
