import path from "node:path";
import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: {
    alias: {
      "@": path.resolve(__dirname),
    },
  },
  test: {
    environment: "node",
    include: ["**/*.test.ts"],
    coverage: {
      provider: "v8",
      reporter: ["text", "lcov"],
      include: [
        "lib/slug.ts",
        "lib/csrfCookie.ts",
        "lib/publicContact.ts",
        "lib/formValidation.ts",
        "lib/format.ts",
        "components/cinematic/journeyFromPackages.ts",
      ],
      reportsDirectory: "coverage",
    },
  },
});
