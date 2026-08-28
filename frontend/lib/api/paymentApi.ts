import { apiFetch } from "./client";
import type { BookingReceipt } from "./bookingApi";

export type PaymentOrder = {
  razorpayOrderId: string;
  razorpayKeyId: string;
  amount: number;
  currency: string;
  bookingReference: string;
  bookingId: number;
};

export type VerifyPaymentPayload = {
  bookingReference: string;
  customerEmail: string;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
};

export type VerifyPaymentResult = {
  bookingReference: string;
  status: string;
  paymentStatus: string;
  receipt: BookingReceipt;
};

export async function createPaymentOrder(
  bookingReference: string,
  customerEmail: string
): Promise<PaymentOrder> {
  const response = await apiFetch<PaymentOrder>("/api/create-order", {
    method: "POST",
    body: JSON.stringify({ bookingReference, customerEmail }),
    cache: "no-store",
  });
  if (!response.data) throw new Error("Unable to create payment order");
  return response.data;
}

export async function verifyPayment(
  payload: VerifyPaymentPayload
): Promise<VerifyPaymentResult> {
  const response = await apiFetch<VerifyPaymentResult>("/api/verify-payment", {
    method: "POST",
    body: JSON.stringify(payload),
    cache: "no-store",
  });
  if (!response.data) throw new Error("Payment verification failed");
  return response.data;
}
