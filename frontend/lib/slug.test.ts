import { describe, expect, it } from "vitest";
import { toSlug } from "./slug";

describe("toSlug", () => {
  it("lowercases, trims, and joins words with hyphens", () => {
    expect(toSlug("  Spiti Valley 2026  ")).toBe("spiti-valley-2026");
  });

  it("strips quotes and collapses punctuation", () => {
    expect(toSlug(`Manali's "Hidden" Lakes!!!`)).toBe("manalis-hidden-lakes");
  });

  it("drops leading and trailing separators", () => {
    expect(toSlug("---Leh---Ladakh---")).toBe("leh-ladakh");
  });

  it("returns an empty string for punctuation-only input", () => {
    expect(toSlug("   ---   ")).toBe("");
  });
});
