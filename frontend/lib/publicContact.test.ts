import { describe, expect, it } from "vitest";
import {
  DEFAULT_WHATSAPP_MESSAGE,
  PUBLIC_CALL_NUMBER,
  PUBLIC_EMAILS,
  PUBLIC_WHATSAPP_NUMBER,
  formatPhoneDisplay,
  paidBookingWhatsAppMessage,
  telUrl,
  whatsappUrl,
} from "./publicContact";

describe("publicContact", () => {
  it("re-exports public emails and default numbers", () => {
    expect(PUBLIC_EMAILS.support).toContain("@");
    expect(PUBLIC_CALL_NUMBER).toMatch(/^\d+$/);
    expect(PUBLIC_WHATSAPP_NUMBER).toMatch(/^\d+$/);
  });

  it("formats Indian numbers and builds tel/whatsapp URLs", () => {
    expect(formatPhoneDisplay("919696313547")).toBe("9696313547");
    expect(formatPhoneDisplay("9696313547")).toBe("9696313547");
    expect(telUrl("91-969-631-3547")).toBe("tel:+919696313547");
    expect(whatsappUrl("hello")).toBe(
      `https://wa.me/${PUBLIC_WHATSAPP_NUMBER}?text=hello`
    );
  });

  it("builds a paid-booking WhatsApp message", () => {
    expect(
      paidBookingWhatsAppMessage({
        bookingReference: "JW-2026-00001",
        packageTitle: "Spiti",
        numberOfTravellers: 2,
      })
    ).toContain("JW-2026-00001");
    expect(DEFAULT_WHATSAPP_MESSAGE).toContain("Joyway");
  });
});
