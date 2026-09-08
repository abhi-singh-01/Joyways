import { adminFetch } from "./client";

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

export async function listAdminBookings(): Promise<AdminBookingSummary[]> {
  const response = await adminFetch<AdminBookingSummary[]>("/api/admin/bookings", {
    cache: "no-store",
  });
  return response.data ?? [];
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
