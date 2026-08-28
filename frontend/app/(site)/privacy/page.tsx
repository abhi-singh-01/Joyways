import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Privacy Policy | JOYWAY",
};

export default function PrivacyPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-20 sm:px-6 lg:px-8">
      <h1 className="font-display text-4xl font-semibold text-charcoal">Privacy Policy</h1>
      <p className="mt-6 text-charcoal-muted">
        Full privacy policy will be published before launch. This is a placeholder for Phase 1.
      </p>
    </div>
  );
}
