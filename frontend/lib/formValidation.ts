import { z } from "zod";
import type { CustomerInput, TravellerInput } from "@/lib/api/bookingApi";

/** Matches backend CustomerRequest / TravellerRequest mobile pattern */
export const INDIAN_MOBILE_REGEX = /^[6-9]\d{9}$/;

export const BOOKING_REFERENCE_REGEX = /^JW-\d{4}-\d{5}$/i;

const DOCUMENT_TYPES = ["AADHAAR", "PASSPORT", "DRIVING_LICENCE", "OTHER"] as const;

export const ALLOWED_DOCUMENT_TYPES = [
  "image/jpeg",
  "application/pdf",
] as const;

export const MAX_DOCUMENT_BYTES = 2 * 1024 * 1024;

export const customerSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(2, "Full name is required")
    .max(120, "Name is too long"),
  email: z
    .string()
    .trim()
    .min(1, "Email is required")
    .email("Enter a valid email address"),
  mobile: z
    .string()
    .trim()
    .regex(INDIAN_MOBILE_REGEX, "Enter a valid 10-digit Indian mobile number"),
});

export const travellerSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(2, "Full name is required")
    .max(120, "Name is too long"),
  dateOfBirth: z
    .string()
    .min(1, "Date of birth is required")
    .refine((value) => {
      const date = new Date(value);
      return !Number.isNaN(date.getTime()) && date < new Date();
    }, "Date of birth must be in the past"),
  gender: z.enum(["Male", "Female", "Other"], {
    error: "Gender is required",
  }),
  mobile: z
    .string()
    .trim()
    .regex(INDIAN_MOBILE_REGEX, "Enter a valid 10-digit Indian mobile number"),
  email: z
    .string()
    .trim()
    .min(1, "Email is required")
    .email("Enter a valid email address"),
  emergencyContact: z
    .string()
    .trim()
    .min(3, "Emergency contact is required")
    .max(255, "Emergency contact is too long"),
  idDocumentType: z.enum(DOCUMENT_TYPES, {
    error: "ID document type is required",
  }),
  idDocumentReference: z
    .string()
    .trim()
    .min(1, "ID document reference is required")
    .max(50, "ID reference is too long"),
});

const AADHAAR_MASKED_REGEX = /^(?:[*Xx]{1,4})?\d{4}$/;

export function validateIdDocumentReference(
  documentType: string,
  reference: string
): string | undefined {
  const normalized = reference.trim().replace(/\s/g, "");
  if (/^\d{12}$/.test(normalized)) {
    return "Enter only the last 4 digits or a masked reference, not the full ID number";
  }
  if (documentType === "AADHAAR") {
    if (/\d{5,}/.test(normalized)) {
      return "For Aadhaar, enter only the last 4 digits (e.g. 1234 or ****1234)";
    }
    if (!AADHAAR_MASKED_REGEX.test(normalized)) {
      return "Enter a valid masked Aadhaar reference (last 4 digits, e.g. ****1234)";
    }
  }
  return undefined;
}

export const bookingLookupSchema = z.object({
  reference: z
    .string()
    .trim()
    .min(1, "Booking reference is required")
    .regex(BOOKING_REFERENCE_REGEX, "Enter a valid reference (e.g. JW-2026-00001)"),
  email: z
    .string()
    .trim()
    .min(1, "Email is required")
    .email("Enter a valid email address"),
});

export const contactEnquirySchema = z.object({
  name: z.string().trim().min(2, "Name is required").max(120, "Name is too long"),
  email: z
    .string()
    .trim()
    .min(1, "Email is required")
    .email("Enter a valid email address"),
  phone: z
    .string()
    .trim()
    .regex(INDIAN_MOBILE_REGEX, "Enter a valid 10-digit Indian mobile number"),
  subject: z
    .string()
    .trim()
    .min(3, "Subject is required")
    .max(120, "Subject is too long"),
  message: z
    .string()
    .trim()
    .min(10, "Please enter at least 10 characters")
    .max(2000, "Message is too long"),
});

export type FieldErrors<T extends Record<string, unknown>> = Partial<
  Record<keyof T, string>
>;

export function fieldErrorsFromZod<T extends Record<string, unknown>>(
  result: z.ZodSafeParseError<T>
): FieldErrors<T> {
  const errors: FieldErrors<T> = {};
  for (const issue of result.error.issues) {
    const key = issue.path[0];
    if (typeof key === "string" && !(key in errors)) {
      errors[key as keyof T] = issue.message;
    }
  }
  return errors;
}

export function validateCustomer(data: CustomerInput) {
  return customerSchema.safeParse(data);
}

export function validateTraveller(data: TravellerInput) {
  return travellerSchema.safeParse(data);
}

export function validateTravellerDocument(file: File | null): string | undefined {
  if (!file) {
    return "Please upload an ID document";
  }
  if (!ALLOWED_DOCUMENT_TYPES.includes(file.type as (typeof ALLOWED_DOCUMENT_TYPES)[number])) {
    return "Use JPEG or PDF only";
  }
  if (file.size > MAX_DOCUMENT_BYTES) {
    return "File must be 2MB or smaller";
  }
  return undefined;
}

export function resolveBookingCustomer(
  count: number,
  customer: CustomerInput,
  travellers: TravellerInput[]
): CustomerInput {
  if (count > 1) {
    return customer;
  }
  const primary = travellers[0];
  return {
    fullName: primary?.fullName ?? "",
    email: primary?.email ?? "",
    mobile: primary?.mobile ?? "",
  };
}

export function validateBookingStep(
  customer: CustomerInput,
  travellers: TravellerInput[],
  documents: (File | null)[],
  count: number
): {
  valid: boolean;
  customerErrors: FieldErrors<CustomerInput>;
  travellerErrors: FieldErrors<TravellerInput>[];
  documentErrors: (string | undefined)[];
} {
  const customerResult =
    count > 1 ? validateCustomer(customer) : { success: true as const };
  const customerErrors =
    count > 1 && !customerResult.success
      ? fieldErrorsFromZod(customerResult)
      : {};

  const travellerErrors: FieldErrors<TravellerInput>[] = [];
  const documentErrors: (string | undefined)[] = [];
  let valid = customerResult.success;

  for (let i = 0; i < count; i++) {
    const traveller = travellers[i] ?? ({} as TravellerInput);
    const travellerResult = validateTraveller(traveller);
    const docError = validateTravellerDocument(documents[i] ?? null);
    const refError = validateIdDocumentReference(
      traveller.idDocumentType,
      traveller.idDocumentReference ?? ""
    );

    const fieldErrors = travellerResult.success
      ? {}
      : fieldErrorsFromZod(travellerResult);
    if (refError) {
      fieldErrors.idDocumentReference = refError;
    }

    travellerErrors.push(fieldErrors);
    documentErrors.push(docError);

    if (!travellerResult.success || docError || refError) {
      valid = false;
    }
  }

  return { valid, customerErrors, travellerErrors, documentErrors };
}
