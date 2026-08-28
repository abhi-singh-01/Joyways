import { adminFetch } from "./client";
import type { TripSummary } from "./adminTripsApi";

export type DashboardMetrics = {
  totalBookings: number;
  confirmedBookings: number;
  pendingPayments: number;
  cancelledBookings: number;
  revenue: number;
  activePackages: number;
  upcomingTrips: number;
  availableSeats: number;
  todaysTrips: number;
  upcomingOperationalTrips: number;
  availableVehicles: number;
  assignedVehicles: number;
  availableDrivers: number;
  assignedDrivers: number;
  tripsInProgress: number;
  tripsCompletedToday: number;
  paidButNotConfirmed: number;
  confirmedButNotPaid: number;
  upcomingTripList: TripSummary[];
};

export async function getDashboardMetrics(): Promise<DashboardMetrics> {
  const response = await adminFetch<DashboardMetrics>("/api/admin/dashboard");
  if (!response.data) {
    throw new Error("Failed to load dashboard");
  }
  return response.data;
}
