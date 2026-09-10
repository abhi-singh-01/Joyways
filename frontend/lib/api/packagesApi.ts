import { cache } from "react";
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
  waitlistable: boolean;
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
  waitlistable: boolean;
};

export type PackageReview = {
  customerName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
};

export type PackageReviews = {
  averageRating: number | null;
  reviewCount: number;
  reviews: PackageReview[];
};

export type WaitlistJoinResult = {
  alreadyJoined: boolean;
  email: string;
};

export const getPackages = cache(async (): Promise<PackageSummary[]> => {
  const response = await apiFetch<PackageSummary[]>("/api/packages");
  return response.data ?? [];
});

export const getPackageBySlug = cache(async (slug: string): Promise<PackageDetail> => {
  const response = await apiFetch<PackageDetail>(`/api/packages/${slug}`);
  if (!response.data) {
    throw new Error("Package not found");
  }
  return response.data;
});

export async function getPackageById(id: number): Promise<PackageDetail> {
  const response = await apiFetch<PackageDetail>(`/api/packages/id/${id}`, {
    cache: "no-store",
  });
  if (!response.data) {
    throw new Error("Package not found");
  }
  return response.data;
}

export const getPackageReviews = cache(async (slug: string): Promise<PackageReviews> => {
  const response = await apiFetch<PackageReviews>(`/api/packages/${slug}/reviews`);
  return (
    response.data ?? {
      averageRating: null,
      reviewCount: 0,
      reviews: [],
    }
  );
});

export async function joinWaitlist(
  slug: string,
  payload: {
    fullName: string;
    email: string;
    mobile?: string;
    seatsRequested: number;
  }
): Promise<{ result: WaitlistJoinResult; message: string }> {
  const response = await apiFetch<WaitlistJoinResult>(`/api/packages/${slug}/waitlist`, {
    method: "POST",
    body: JSON.stringify(payload),
    cache: "no-store",
  });
  if (!response.data) {
    throw new Error("Could not join the waitlist.");
  }
  return {
    result: response.data,
    message: response.message || "You are on the waitlist.",
  };
}
