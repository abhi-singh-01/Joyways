import { adminFetch } from "./client";
import type { PageResponse } from "./adminVehiclesApi";

export type DriverStatus = "AVAILABLE" | "ASSIGNED" | "ON_TRIP" | "INACTIVE";

export type DriverSummary = {
  id: number;
  name: string;
  phone: string;
  licenseNumber: string;
  licenseExpiryDate: string;
  licenseStatus: string;
  status: DriverStatus;
  currentTripReference: string | null;
  createdAt: string;
};

export type DriverDetail = DriverSummary & {
  email: string | null;
  notes: string | null;
  updatedAt: string;
};

export type CreateDriverPayload = {
  name: string;
  phone: string;
  email?: string;
  licenseNumber: string;
  licenseExpiryDate: string;
  notes?: string;
};

export async function listDrivers(params?: {
  query?: string;
  status?: string;
  licenseFilter?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<DriverSummary>> {
  const search = new URLSearchParams();
  if (params?.query) search.set("query", params.query);
  if (params?.status) search.set("status", params.status);
  if (params?.licenseFilter) search.set("licenseFilter", params.licenseFilter);
  search.set("page", String(params?.page ?? 0));
  search.set("size", String(params?.size ?? 20));
  const response = await adminFetch<PageResponse<DriverSummary>>(
    `/api/admin/drivers?${search.toString()}`
  );
  return response.data ?? { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
}

export async function getDriver(id: number): Promise<DriverDetail> {
  const response = await adminFetch<DriverDetail>(`/api/admin/drivers/${id}`);
  if (!response.data) throw new Error("Driver not found");
  return response.data;
}

export async function createDriver(payload: CreateDriverPayload): Promise<DriverDetail> {
  const response = await adminFetch<DriverDetail>("/api/admin/drivers", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to create driver");
  return response.data;
}

export async function updateDriver(
  id: number,
  payload: CreateDriverPayload
): Promise<DriverDetail> {
  const response = await adminFetch<DriverDetail>(`/api/admin/drivers/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to update driver");
  return response.data;
}

export async function updateDriverStatus(
  id: number,
  status: DriverStatus
): Promise<DriverDetail> {
  const response = await adminFetch<DriverDetail>(`/api/admin/drivers/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
  if (!response.data) throw new Error("Failed to update status");
  return response.data;
}
