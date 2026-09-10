import { describe, expect, it } from "vitest";
import type { PackageSummary } from "@/lib/api/packagesApi";
import {
  LANDSCAPE_SCENES,
  bookableTrips,
  isBookablePrice,
  uniqueDestinations,
} from "./journeyFromPackages";

function pkg(overrides: Partial<PackageSummary>): PackageSummary {
  return {
    id: 1,
    title: "Spiti",
    slug: "spiti",
    destination: "Spiti",
    shortDescription: "",
    duration: "6D",
    departureCity: "Delhi",
    startDate: "2026-10-01",
    endDate: "2026-10-06",
    pricePerPerson: 24999,
    availableSeats: 8,
    heroImageUrl: null,
    heroImageAlt: null,
    waitlistable: false,
    ...overrides,
  };
}

describe("journeyFromPackages", () => {
  it("exposes landscape scenes", () => {
    expect(LANDSCAPE_SCENES.length).toBeGreaterThan(0);
    expect(LANDSCAPE_SCENES[0]?.image).toMatch(/^\//);
  });

  it("treats only finite positive amounts as bookable", () => {
    expect(isBookablePrice(1)).toBe(true);
    expect(isBookablePrice(0)).toBe(false);
    expect(isBookablePrice(null)).toBe(false);
    expect(isBookablePrice(Number.NaN)).toBe(false);
  });

  it("dedupes destinations case-insensitively and skips blanks", () => {
    expect(
      uniqueDestinations([
        pkg({ destination: " Spiti " }),
        pkg({ id: 2, destination: "spiti" }),
        pkg({ id: 3, destination: "  " }),
        pkg({ id: 4, destination: "Ladakh" }),
      ])
    ).toEqual(["Spiti", "Ladakh"]);
  });

  it("filters to bookable trips", () => {
    expect(
      bookableTrips([
        pkg({ id: 1, pricePerPerson: 100 }),
        pkg({ id: 2, pricePerPerson: 0 }),
      ]).map((item) => item.id)
    ).toEqual([1]);
  });
});
