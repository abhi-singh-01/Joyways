import { apiFetch } from "./client";

export type PackageSummary = {
  id: number;
  title: string;
  slug: string;
  destination: string;
  shortDescription: string;
  duration: string;
  departureCity: string;
  startDate: string;
  endDate: string;
  pricePerPerson: number;
  availableSeats: number;
  heroImageUrl: string | null;
  heroImageAlt: string | null;
};

export type PackageImage = {
  id: number;
  imageUrl: string;
  altText: string;
  sortOrder: number;
};

export type PackageDetail = {
  id: number;
  title: string;
  slug: string;
  destination: string;
  shortDescription: string;
  fullDescription: string;
  departureCity: string;
  pickupPoints: string;
  startDate: string;
  endDate: string;
  duration: string;
  pricePerPerson: number;
  totalSeats: number;
  availableSeats: number;
  hotelName: string;
  hotelDetails: string;
  vehicleType: string;
  itinerary: string;
  inclusions: string;
  exclusions: string;
  cancellationPolicy: string;
  status: string;
  images: PackageImage[];
  bookable: boolean;
};

export async function getPackages(): Promise<PackageSummary[]> {
  const response = await apiFetch<PackageSummary[]>("/api/packages");
  return response.data ?? [];
}

export async function getPackageBySlug(slug: string): Promise<PackageDetail> {
  const response = await apiFetch<PackageDetail>(`/api/packages/${slug}`);
  if (!response.data) {
    throw new Error("Package not found");
  }
  return response.data;
}

export async function getPackageById(id: number): Promise<PackageDetail> {
  const response = await apiFetch<PackageDetail>(`/api/packages/id/${id}`, {
    cache: "no-store",
  });
  if (!response.data) {
    throw new Error("Package not found");
  }
  return response.data;
}
