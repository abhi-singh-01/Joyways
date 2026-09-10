import { describe, expect, it } from "vitest";
import type { CustomerInput, TravellerInput } from "@/lib/api/bookingApi";
import {
  bookingLookupSchema,
  contactEnquirySchema,
  fieldErrorsFromZod,
  resolveBookingCustomer,
  validateBookingStep,
  validateCustomer,
  validateIdDocumentReference,
  validateTraveller,
  validateTravellerDocument,
} from "./formValidation";

const customer: CustomerInput = {
  fullName: "Riya Sharma",
  email: "riya@example.com",
  mobile: "9876543210",
};

const traveller: TravellerInput = {
  fullName: "Riya Sharma",
  dateOfBirth: "1995-01-15",
  gender: "Female",
  mobile: "9876543210",
  email: "riya@example.com",
  emergencyContact: "Amit 9876501234",
  idDocumentType: "AADHAAR",
  idDocumentReference: "****1234",
};

function jpegFile(size = 100): File {
  return new File([new Uint8Array(size)], "id.jpg", { type: "image/jpeg" });
}

describe("email and lookup schemas", () => {
  it("rejects empty and invalid emails", () => {
    expect(validateCustomer({ ...customer, email: "   " }).success).toBe(false);
    expect(validateCustomer({ ...customer, email: "not-an-email" }).success).toBe(false);
    expect(validateCustomer(customer).success).toBe(true);
  });

  it("validates booking lookup and contact enquiry", () => {
    expect(
      bookingLookupSchema.safeParse({
        reference: "JW-2026-00001",
        email: "riya@example.com",
      }).success
    ).toBe(true);
    expect(
      contactEnquirySchema.safeParse({
        name: "Riya",
        email: "riya@example.com",
        phone: "9876543210",
        subject: "Trip help",
        message: "Please tell me more about Spiti dates.",
      }).success
    ).toBe(true);
  });
});

describe("ID document reference", () => {
  it("blocks a full 12-digit number", () => {
    expect(validateIdDocumentReference("AADHAAR", "123456789012")).toMatch(/full ID/);
  });

  it("requires a masked Aadhaar last-4", () => {
    expect(validateIdDocumentReference("AADHAAR", "12345")).toMatch(/last 4/);
    expect(validateIdDocumentReference("AADHAAR", "abcd")).toMatch(/masked Aadhaar/);
    expect(validateIdDocumentReference("AADHAAR", "****1234")).toBeUndefined();
    expect(validateIdDocumentReference("PASSPORT", "Z1234567")).toBeUndefined();
  });
});

describe("traveller document and booking step", () => {
  it("validates uploaded documents", () => {
    expect(validateTravellerDocument(null)).toMatch(/upload/);
    expect(
      validateTravellerDocument(new File(["x"], "id.txt", { type: "text/plain" }))
    ).toMatch(/JPEG or PDF/);
    expect(
      validateTravellerDocument(
        new File([new Uint8Array(2 * 1024 * 1024 + 1)], "id.jpg", {
          type: "image/jpeg",
        })
      )
    ).toMatch(/2MB/);
    expect(validateTravellerDocument(jpegFile())).toBeUndefined();
  });

  it("maps the first Zod issue per field", () => {
    const result = validateCustomer({ fullName: "", email: "", mobile: "" });
    expect(result.success).toBe(false);
    if (!result.success) {
      const errors = fieldErrorsFromZod(result);
      expect(errors.fullName).toBeTruthy();
      expect(errors.email).toBeTruthy();
    }
  });

  it("resolves the booking customer from the primary traveller", () => {
    expect(resolveBookingCustomer(2, customer, [traveller])).toEqual(customer);
    expect(resolveBookingCustomer(1, customer, [traveller])).toEqual({
      fullName: traveller.fullName,
      email: traveller.email,
      mobile: traveller.mobile,
    });
    expect(resolveBookingCustomer(1, customer, [])).toEqual({
      fullName: "",
      email: "",
      mobile: "",
    });
  });

  it("validates a complete booking step", () => {
    const ok = validateBookingStep(customer, [traveller], [jpegFile()], 1);
    expect(ok.valid).toBe(true);

    const bad = validateBookingStep(
      { ...customer, email: "bad" },
      [{ ...traveller, fullName: "A", idDocumentReference: "123456789012" }],
      [null],
      2
    );
    expect(bad.valid).toBe(false);
    expect(bad.customerErrors.email).toBeTruthy();
    expect(bad.travellerErrors[0]?.fullName).toBeTruthy();
    expect(bad.travellerErrors[0]?.idDocumentReference).toMatch(/full ID/);
    expect(bad.documentErrors[0]).toMatch(/upload/);
  });

  it("accepts a valid traveller", () => {
    expect(validateTraveller(traveller).success).toBe(true);
    expect(validateTraveller({ ...traveller, dateOfBirth: "2999-01-01" }).success).toBe(
      false
    );
  });
});
