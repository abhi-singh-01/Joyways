import { apiFetch } from "./client";

export type ContactEnquiryPayload = {
  name: string;
  email: string;
  phone: string;
  subject: string;
  message: string;
};

export async function submitContactEnquiry(payload: ContactEnquiryPayload): Promise<void> {
  const response = await apiFetch<null>("/api/contact/enquiry", {
    method: "POST",
    body: JSON.stringify(payload),
    cache: "no-store",
    next: { revalidate: 0 },
  });
  if (!response.success) {
    throw new Error(response.message || "Unable to send your message. Please try again.");
  }
}
