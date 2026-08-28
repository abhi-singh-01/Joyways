import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Cancellation & Refund Policy | JOYWAY",
};

export default function CancellationPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-20 sm:px-6 lg:px-8">
      <h1 className="font-display text-4xl font-semibold text-charcoal">
        Cancellation & Refund Policy
      </h1>
      <p className="mt-6 text-charcoal-muted">
        Full cancellation and refund policy will be published before launch. Demo packages
        include sample cancellation terms on their detail pages.
      </p>
    </div>
  );
}
