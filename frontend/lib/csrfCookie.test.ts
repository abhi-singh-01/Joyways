import { describe, expect, it } from "vitest";
import { readCsrfTokenFromCookie } from "./csrfCookie";

describe("readCsrfTokenFromCookie", () => {
  it("reads the token from the start of the cookie string", () => {
    expect(readCsrfTokenFromCookie("XSRF-TOKEN=abc123; other=1")).toBe("abc123");
  });

  it("reads a later cookie and decodes the value", () => {
    expect(
      readCsrfTokenFromCookie("session=keep; XSRF-TOKEN=a%2Fb%3D; theme=dark")
    ).toBe("a/b=");
  });

  it("returns null when the cookie is missing", () => {
    expect(readCsrfTokenFromCookie("session=keep")).toBeNull();
  });
});
