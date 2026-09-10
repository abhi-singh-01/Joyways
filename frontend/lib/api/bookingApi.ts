import { apiFetch } from "./client";
import { getApiBaseUrl } from "./config";

export type CustomerInput = {
  fullName: string;
  email: string;
  mobile: string;
};

export type TravellerInput = {
  fullName: string;
  dateOfBirth: string;
  gender: string;
  mobile: string;
  email: string;
  emergencyContact: string;
  idDocumentType: string;
  idDocumentReference: string;
};

export type CreateBookingPayload = {
  packageId: number;
  numberOfTravellers: number;
  customer: CustomerInput;
  travellers: TravellerInput[];
  consentAccepted: boolean;
  pickupPoint?: string;
};

export type BookingAccessInput = {
  reference: string;
  email: string;
};

export type BookingReview = {
  packageId: number;
  packageTitle: string;
  destination: string;
  startDate: string;
  endDate: string;
  duration: string;
  numberOfTravellers: number;
  pricePerPerson: number;
  subtotal: number;
  taxesAndFees: number;
  totalAmount: number;
  currency: string;
  availableSeats: number;
  bookable: boolean;
  pickupPoint?: string | null;
};

export type BookingResult = {
  bookingReference: string;
  bookingId: number;
  packageTitle: string;
  destination: string;
  startDate: string;
  endDate: string;
  numberOfTravellers: number;
  totalAmount: number;
  currency: string;
  status: string;
  paymentStatus: string;
  createdAt: string;
  paymentExpiresAt?: string | null;
  pickupPoint?: string | null;
  travellers: { fullName: string; idDocumentType: string }[];
};

export type BookingReceipt = {
  brandName: string;
  title: string;
  bookingReference: string;
  customerName: string;
  packageTitle: string;
  destination: string;
  travelDate: string;
  endDate: string;
  numberOfTravellers: number;
  packageAmount: number;
  currency: string;
  paymentStatus: string;
  razorpayPaymentId: string | null;
  bookingDate: string;
  paymentDate: string | null;
};

export type BookingDetail = BookingResult & {
  customerName: string;
  customerEmail: string;
  customerMobile: string;
  razorpayOrderId: string | null;
  razorpayPaymentId: string | null;
  paymentDate: string | null;
  reviewEligible?: boolean;
  reviewSubmitted?: boolean;
};

export type PackageReviewSubmission = {
  customerName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
};

export async function previewBooking(
  payload: CreateBookingPayload
): Promise<BookingReview> {
  const response = await apiFetch<BookingReview>("/api/bookings/preview", {
    method: "POST",
    body: JSON.stringify(payload),
    cache: "no-store",
  });
  if (!response.data) throw new Error("Preview failed");
  return response.data;
}

export async function createBooking(
  payload: CreateBookingPayload,
  documents: File[]
): Promise<BookingResult> {
  const formData = new FormData();
  formData.append("request", JSON.stringify(payload));
  documents.forEach((file) => formData.append("documents", file));

  let response: Response;
  const requestId =
    typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
      ? crypto.randomUUID()
      : `jw-${Date.now().toString(36)}`;
  try {
    response = await fetch(`${getApiBaseUrl()}/api/bookings`, {
      method: "POST",
      body: formData,
      credentials: "same-origin",
      headers: { "X-Request-ID": requestId },
    });
  } catch {
    throw new Error("Unable to reach the server. Please try again.");
  }

  const json = await response.json();
  if (!response.ok) {
    const suffix = json.requestId && response.status >= 500 ? ` (ref ${json.requestId})` : "";
    throw new Error((json.message || "Booking failed") + suffix);
  }
  return json.data as BookingResult;
}

export async function getBooking(
  bookingReference: string,
  email: string
): Promise<BookingDetail> {
  const response = await apiFetch<BookingDetail>("/api/bookings/lookup", {
    method: "POST",
    body: JSON.stringify({ reference: bookingReference, email }),
    cache: "no-store",
  });
  if (!response.data) throw new Error("Booking not found");
  return response.data;
}

export async function getBookingReceipt(
  bookingReference: string,
  email: string
): Promise<BookingReceipt> {
  const response = await apiFetch<BookingReceipt>("/api/bookings/receipt", {
    method: "POST",
    body: JSON.stringify({ reference: bookingReference, email }),
    cache: "no-store",
  });
  if (!response.data) throw new Error("Receipt not found");
  return response.data;
}

export async function downloadBookingReceiptPdf(
  bookingReference: string,
  email: string
): Promise<void> {
  let response: Response;
  try {
    response = await fetch(`${getApiBaseUrl()}/api/bookings/receipt.pdf`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reference: bookingReference, email }),
    });
  } catch {
    throw new Error("Unable to reach the server. Please try again.");
  }
  if (!response.ok) {
    let message = "Unable to download receipt.";
    try {
      const json = (await response.json()) as { message?: string };
      if (json.message) message = json.message;
    } catch {
      // keep default
    }
    throw new Error(message);
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `joyway-receipt-${bookingReference}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export async function cancelBooking(
  bookingReference: string,
  email: string
): Promise<BookingResult> {
  const response = await apiFetch<BookingResult>("/api/bookings/cancel", {
    method: "POST",
    body: JSON.stringify({ reference: bookingReference, email }),
    cache: "no-store",
  });
  if (!response.data) throw new Error("Cancellation failed");
  return response.data;
}

export async function submitBookingReview(payload: {
  reference: string;
  email: string;
  rating: number;
  comment?: string;
}): Promise<PackageReviewSubmission> {
  const response = await apiFetch<PackageReviewSubmission>("/api/bookings/reviews", {
    method: "POST",
    body: JSON.stringify(payload),
    cache: "no-store",
  });
  if (!response.data) throw new Error("Could not submit review");
  return response.data;
}
