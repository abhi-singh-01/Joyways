import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Terms & Conditions | JOYWAY",
};

export default function TermsPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-20 sm:px-6 lg:px-8">
      <h1 className="font-display text-4xl font-semibold text-charcoal">Terms & Conditions</h1>
      <p className="mt-6 text-charcoal-muted">
        Full terms and conditions will be published before launch. This is a placeholder for Phase 1.
      </p>
    </div>
  );
}
