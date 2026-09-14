import { adminFetch } from "./client";

export type VehicleStatus =
  | "AVAILABLE"
  | "ASSIGNED"
  | "ON_TRIP"
  | "MAINTENANCE"
  | "INACTIVE";

export type VehicleOwnershipType = "OWNED" | "RENTED" | "PARTNER";

export type VehicleSummary = {
  id: number;
  registrationNumber: string;
  vehicleType: string;
  brandModel: string | null;
  seatingCapacity: number;
  ownershipType: VehicleOwnershipType;
  status: VehicleStatus;
  currentTripReference: string | null;
  createdAt: string;
};

export type VehicleDetail = VehicleSummary & {
  notes: string | null;
  updatedAt: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

/** Normalize paginated API payloads so callers never hit undefined `.content`. */
export function pageContent<T>(page: PageResponse<T> | null | undefined): T[] {
  return Array.isArray(page?.content) ? page.content : [];
}

export type CreateVehiclePayload = {
  registrationNumber: string;
  vehicleType: string;
  brandModel?: string;
  seatingCapacity: number;
  ownershipType: VehicleOwnershipType;
  notes?: string;
};

export async function listVehicles(params?: {
  query?: string;
  status?: string;
  ownershipType?: string;
  vehicleType?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<VehicleSummary>> {
  const search = new URLSearchParams();
  if (params?.query) search.set("query", params.query);
  if (params?.status) search.set("status", params.status);
  if (params?.ownershipType) search.set("ownershipType", params.ownershipType);
  if (params?.vehicleType) search.set("vehicleType", params.vehicleType);
  search.set("page", String(params?.page ?? 0));
  search.set("size", String(params?.size ?? 20));
  const response = await adminFetch<PageResponse<VehicleSummary>>(
    `/api/admin/vehicles?${search.toString()}`
  );
  return response.data ?? { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
}

export async function getVehicle(id: number): Promise<VehicleDetail> {
  const response = await adminFetch<VehicleDetail>(`/api/admin/vehicles/${id}`);
  if (!response.data) throw new Error("Vehicle not found");
  return response.data;
}

export async function createVehicle(payload: CreateVehiclePayload): Promise<VehicleDetail> {
  const response = await adminFetch<VehicleDetail>("/api/admin/vehicles", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to create vehicle");
  return response.data;
}

export async function updateVehicle(
  id: number,
  payload: CreateVehiclePayload
): Promise<VehicleDetail> {
  const response = await adminFetch<VehicleDetail>(`/api/admin/vehicles/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to update vehicle");
  return response.data;
}

export async function updateVehicleStatus(
  id: number,
  status: VehicleStatus
): Promise<VehicleDetail> {
  const response = await adminFetch<VehicleDetail>(`/api/admin/vehicles/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
  if (!response.data) throw new Error("Failed to update status");
  return response.data;
}
