import { adminFetch } from "./client";
import type { PackageImage } from "./packagesApi";

export type PackageStatus =
  | "DRAFT"
  | "PUBLISHED"
  | "PAUSED"
  | "COMPLETED"
  | "CANCELLED";

export type AdminPackageSummary = {
  id: number;
  title: string;
  slug: string;
  destination: string;
  startDate: string;
  endDate: string;
  duration: string;
  pricePerPerson: number;
  totalSeats: number;
  availableSeats: number;
  status: PackageStatus;
  heroImageUrl: string | null;
  imageCount: number;
};

export type AdminPackageDetail = {
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
  status: PackageStatus;
  images: PackageImage[];
};

export type CreatePackagePayload = {
  title: string;
  slug?: string;
  destination: string;
  shortDescription?: string;
  fullDescription?: string;
  departureCity: string;
  pickupPoints?: string;
  startDate: string;
  endDate: string;
  duration: string;
  pricePerPerson: number;
  totalSeats: number;
  hotelName?: string;
  hotelDetails?: string;
  vehicleType?: string;
  itinerary?: string;
  inclusions?: string;
  exclusions?: string;
  cancellationPolicy?: string;
};

export type UpdatePackagePayload = Partial<
  CreatePackagePayload & { availableSeats: number }
>;

export async function listAdminPackages(): Promise<AdminPackageSummary[]> {
  const response = await adminFetch<AdminPackageSummary[]>("/api/admin/packages");
  return response.data ?? [];
}

export async function getAdminPackage(id: number): Promise<AdminPackageDetail> {
  const response = await adminFetch<AdminPackageDetail>(`/api/admin/packages/${id}`);
  if (!response.data) throw new Error("Package not found");
  return response.data;
}

export async function createAdminPackage(
  payload: CreatePackagePayload
): Promise<AdminPackageDetail> {
  const response = await adminFetch<AdminPackageDetail>("/api/admin/packages", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to create package");
  return response.data;
}

export async function updateAdminPackage(
  id: number,
  payload: UpdatePackagePayload
): Promise<AdminPackageDetail> {
  const response = await adminFetch<AdminPackageDetail>(`/api/admin/packages/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to update package");
  return response.data;
}

export async function updatePackageStatus(
  id: number,
  status: PackageStatus
): Promise<AdminPackageDetail> {
  const response = await adminFetch<AdminPackageDetail>(
    `/api/admin/packages/${id}/status`,
    {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }
  );
  if (!response.data) throw new Error("Failed to update status");
  return response.data;
}

export async function deleteAdminPackage(id: number): Promise<void> {
  await adminFetch<void>(`/api/admin/packages/${id}`, { method: "DELETE" });
}

export async function addPackageImageUrl(
  packageId: number,
  imageUrl: string,
  altText?: string
): Promise<PackageImage> {
  const response = await adminFetch<PackageImage>(
    `/api/admin/packages/${packageId}/images/url`,
    {
      method: "POST",
      body: JSON.stringify({ imageUrl, altText }),
    }
  );
  if (!response.data) throw new Error("Failed to add image");
  return response.data;
}

export async function uploadPackageImage(
  packageId: number,
  file: File,
  altText?: string
): Promise<PackageImage> {
  const formData = new FormData();
  formData.append("file", file);
  if (altText) formData.append("altText", altText);

  const response = await adminFetch<PackageImage>(
    `/api/admin/packages/${packageId}/images/upload`,
    {
      method: "POST",
      body: formData,
    }
  );
  if (!response.data) throw new Error("Failed to upload image");
  return response.data;
}

export async function deletePackageImage(
  packageId: number,
  imageId: number
): Promise<void> {
  await adminFetch<void>(`/api/admin/packages/${packageId}/images/${imageId}`, {
    method: "DELETE",
  });
}
