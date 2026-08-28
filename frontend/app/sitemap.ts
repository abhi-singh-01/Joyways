import type { MetadataRoute } from "next";
import { getPackages } from "@/lib/api/packagesApi";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = "https://joyway.in";
  const staticRoutes = [
    "",
    "/trips",
    "/about",
    "/contact",
    "/terms",
    "/privacy",
    "/cancellation",
  ];

  const staticEntries: MetadataRoute.Sitemap = staticRoutes.map((route) => ({
    url: `${baseUrl}${route}`,
    lastModified: new Date(),
    changeFrequency: route === "" ? "weekly" : "monthly",
    priority: route === "" ? 1 : 0.7,
  }));

  let tripEntries: MetadataRoute.Sitemap = [];
  try {
    const packages = await getPackages();
    tripEntries = packages.map((pkg) => ({
      url: `${baseUrl}/trips/${pkg.slug}`,
      lastModified: new Date(),
      changeFrequency: "weekly" as const,
      priority: 0.8,
    }));
  } catch {
    // Backend unavailable during build — static routes only
  }

  return [...staticEntries, ...tripEntries];
}
