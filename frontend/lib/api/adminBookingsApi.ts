import { adminFetch } from "./client";
import type { PageResponse } from "./adminVehiclesApi";
import type { BookingResult, CustomerInput, TravellerInput } from "./bookingApi";

export type AdminBookingSummary = {
  bookingId: number;
  bookingReference: string;
  customerName: string;
  customerEmail: string;
  packageTitle: string;
  destination: string;
  numberOfTravellers: number;
  totalAmount: number;
  currency: string;
  bookingStatus: string;
  paymentStatus: string;
  razorpayOrderId: string | null;
  razorpayPaymentId: string | null;
  createdAt: string;
};

export type AdminBookingDetail = AdminBookingSummary & {
  customerMobile: string;
  startDate: string;
  endDate: string;
  signatureVerified: boolean;
  paymentDate: string | null;
  razorpayRefundId: string | null;
  paymentRecordStatus: string | null;
  refundedAmount: number;
  travellers: { fullName: string; idDocumentType: string }[];
  pickupPoint?: string | null;
  createdByAdmin?: boolean;
  adminNotes?: string | null;
  offlinePaymentReference?: string | null;
};

export type AdminRefundResult = {
  bookingId: number;
  bookingReference: string;
  bookingStatus: string;
  paymentStatus: string;
  paymentRecordStatus: string;
  razorpayRefundId: string | null;
  refundState: string;
  refundedAmount: number;
  totalAmount: number;
};

export async function listAdminBookings(params?: {
  query?: string;
  bookingStatus?: string;
  paymentStatus?: string;
  destination?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<AdminBookingSummary>> {
  const search = new URLSearchParams();
  if (params?.query) search.set("query", params.query);
  if (params?.bookingStatus) search.set("bookingStatus", params.bookingStatus);
  if (params?.paymentStatus) search.set("paymentStatus", params.paymentStatus);
  if (params?.destination) search.set("destination", params.destination);
  search.set("page", String(params?.page ?? 0));
  search.set("size", String(params?.size ?? 20));
  const response = await adminFetch<PageResponse<AdminBookingSummary>>(
    `/api/admin/bookings?${search.toString()}`,
    { cache: "no-store" },
  );
  return response.data ?? { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
}

export async function getAdminBooking(id: number): Promise<AdminBookingDetail> {
  const response = await adminFetch<AdminBookingDetail>(`/api/admin/bookings/${id}`, {
    cache: "no-store",
  });
  if (!response.data) throw new Error("Booking not found");
  return response.data;
}

export async function cancelAdminBooking(id: number): Promise<void> {
  await adminFetch(`/api/admin/bookings/${id}/cancel`, {
    method: "POST",
  });
}

export async function refundAdminBooking(
  id: number,
  amount?: number
): Promise<AdminRefundResult> {
  const response = await adminFetch<AdminRefundResult>(`/api/admin/bookings/${id}/refund`, {
    method: "POST",
    body: JSON.stringify(amount != null ? { amount } : {}),
  });
  if (!response.data) throw new Error("Refund failed");
  return response.data;
}

export type AdminBookingPaymentMode = "PENDING_ONLINE" | "PAID_OFFLINE";

export type AdminCreateBookingPayload = {
  packageId: number;
  numberOfTravellers: number;
  customer: CustomerInput;
  travellers: TravellerInput[];
  pickupPoint?: string;
  paymentMode: AdminBookingPaymentMode;
  offlinePaymentReference?: string;
  notes?: string;
};

export async function createAdminBooking(
  payload: AdminCreateBookingPayload
): Promise<BookingResult> {
  const response = await adminFetch<BookingResult>("/api/admin/bookings", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to create booking");
  return response.data;
}
