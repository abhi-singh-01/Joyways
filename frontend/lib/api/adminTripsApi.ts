import { adminFetch } from "./client";
import type { PageResponse, VehicleSummary } from "./adminVehiclesApi";
import type { DriverSummary } from "./adminDriversApi";

export type TripStatus =
  | "PLANNED"
  | "ASSIGNED"
  | "READY"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";

export type TripSummary = {
  id: number;
  tripReference: string;
  packageTitle: string;
  destination: string;
  travelDate: string;
  startAt: string;
  endAt: string;
  bookingCount: number;
  totalTravellers: number;
  vehicleCapacity: number | null;
  remainingCapacity: number | null;
  vehicleRegistration: string | null;
  driverName: string | null;
  status: TripStatus;
};

export type TripBookingSummary = {
  bookingId: number;
  bookingReference: string;
  customerName: string;
  numberOfTravellers: number;
  bookingStatus: string;
  paymentStatus: string;
};

export type TripDetail = {
  id: number;
  tripReference: string;
  packageId: number;
  packageTitle: string;
  destination: string;
  travelDate: string;
  startAt: string;
  endAt: string;
  pickupLocation: string | null;
  dropLocation: string | null;
  status: TripStatus;
  notes: string | null;
  totalTravellers: number;
  vehicleCapacity: number | null;
  remainingCapacity: number | null;
  vehicle: VehicleSummary | null;
  driver: DriverSummary | null;
  bookings: TripBookingSummary[];
  createdAt: string;
  updatedAt: string;
};

export type CreateTripPayload = {
  packageId: number;
  travelDate: string;
  startAt: string;
  endAt: string;
  pickupLocation?: string;
  dropLocation?: string;
  notes?: string;
};

export async function listTrips(params?: {
  query?: string;
  status?: string;
  travelDate?: string;
  vehicleId?: number;
  driverId?: number;
  page?: number;
  size?: number;
}): Promise<PageResponse<TripSummary>> {
  const search = new URLSearchParams();
  if (params?.query) search.set("query", params.query);
  if (params?.status) search.set("status", params.status);
  if (params?.travelDate) search.set("travelDate", params.travelDate);
  if (params?.vehicleId) search.set("vehicleId", String(params.vehicleId));
  if (params?.driverId) search.set("driverId", String(params.driverId));
  search.set("page", String(params?.page ?? 0));
  search.set("size", String(params?.size ?? 20));
  const response = await adminFetch<PageResponse<TripSummary>>(
    `/api/admin/trips?${search.toString()}`
  );
  return response.data ?? { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
}

export async function getTrip(id: number): Promise<TripDetail> {
  const response = await adminFetch<TripDetail>(`/api/admin/trips/${id}`);
  if (!response.data) throw new Error("Trip not found");
  return response.data;
}

export async function listCalendarTrips(
  fromDate: string,
  toDate: string
): Promise<TripSummary[]> {
  const search = new URLSearchParams({ fromDate, toDate });
  const response = await adminFetch<TripSummary[]>(
    `/api/admin/trips/calendar?${search.toString()}`
  );
  return response.data ?? [];
}

export async function listUpcomingTrips(limit = 10): Promise<TripSummary[]> {
  const response = await adminFetch<TripSummary[]>(
    `/api/admin/trips/upcoming?limit=${limit}`
  );
  return response.data ?? [];
}

export async function createTrip(payload: CreateTripPayload): Promise<TripDetail> {
  const response = await adminFetch<TripDetail>("/api/admin/trips", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to create trip");
  return response.data;
}

export async function updateTrip(
  id: number,
  payload: CreateTripPayload
): Promise<TripDetail> {
  const response = await adminFetch<TripDetail>(`/api/admin/trips/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to update trip");
  return response.data;
}

export async function assignVehicleToTrip(
  tripId: number,
  vehicleId: number
): Promise<TripDetail> {
  const response = await adminFetch<TripDetail>(
    `/api/admin/trips/${tripId}/assign-vehicle`,
    { method: "POST", body: JSON.stringify({ vehicleId }) }
  );
  if (!response.data) throw new Error("Failed to assign vehicle");
  return response.data;
}

export async function assignDriverToTrip(
  tripId: number,
  driverId: number
): Promise<TripDetail> {
  const response = await adminFetch<TripDetail>(
    `/api/admin/trips/${tripId}/assign-driver`,
    { method: "POST", body: JSON.stringify({ driverId }) }
  );
  if (!response.data) throw new Error("Failed to assign driver");
  return response.data;
}

export async function addBookingToTrip(
  tripId: number,
  bookingId: number
): Promise<TripDetail> {
  const response = await adminFetch<TripDetail>(`/api/admin/trips/${tripId}/bookings`, {
    method: "POST",
    body: JSON.stringify({ bookingId }),
  });
  if (!response.data) throw new Error("Failed to assign booking");
  return response.data;
}

export async function removeBookingFromTrip(
  tripId: number,
  bookingId: number
): Promise<TripDetail> {
  const response = await adminFetch<TripDetail>(
    `/api/admin/trips/${tripId}/bookings/${bookingId}`,
    { method: "DELETE" }
  );
  if (!response.data) throw new Error("Failed to remove booking");
  return response.data;
}

export async function updateTripStatus(
  tripId: number,
  status: TripStatus
): Promise<TripDetail> {
  const response = await adminFetch<TripDetail>(`/api/admin/trips/${tripId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
  if (!response.data) throw new Error("Failed to update trip status");
  return response.data;
}
