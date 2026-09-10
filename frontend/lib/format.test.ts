import { describe, expect, it } from "vitest";
import { formatDate, formatDateRange, formatPrice, parsePickupPoints, splitLines } from "./format";

describe("format helpers", () => {
  it("formats INR without paise", () => {
    expect(formatPrice(24999)).toMatch(/24,999/);
  });

  it("formats dates and ranges", () => {
    expect(formatDate("2026-09-11")).toMatch(/11/);
    expect(formatDateRange("2026-09-11", "2026-09-14")).toContain("–");
  });

  it("splits multiline text", () => {
    expect(splitLines(null)).toEqual([]);
    expect(splitLines("  one\n\ntwo  \r\nthree")).toEqual(["one", "two", "three"]);
  });

  it("parses unique pickup points", () => {
    expect(parsePickupPoints(undefined)).toEqual([]);
    expect(parsePickupPoints("Kashmere Gate,  ISBT,\nKashmere Gate")).toEqual([
      "Kashmere Gate",
      "ISBT",
    ]);
  });
});
