import type { Metadata } from "next";
import { Mail } from "lucide-react";
import { PUBLIC_EMAILS } from "@/lib/publicEmails";

export const metadata: Metadata = {
  title: "Contact | JOYWAY",
  description: "Get in touch with JOYWAY for group travel enquiries.",
};

export default function ContactPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-20 sm:px-6 lg:px-8">
      <h1 className="font-display text-4xl font-semibold text-charcoal">Contact Us</h1>
      <p className="mt-6 text-lg leading-relaxed text-charcoal-muted">
        Have questions about a trip or need help with a booking? We&apos;d love to hear from you.
      </p>

      <div className="mt-8 space-y-4">
        <div className="rounded-2xl border border-border bg-white p-6">
          <div className="flex items-start gap-3">
            <Mail className="mt-0.5 text-forest" size={24} />
            <div>
              <p className="font-medium text-charcoal">Customer Support</p>
              <p className="mt-1 text-sm text-charcoal-muted">
                Help, complaints, refunds, and general queries.
              </p>
              <a
                href={`mailto:${PUBLIC_EMAILS.support}`}
                className="mt-2 inline-block text-forest hover:text-forest-light"
              >
                {PUBLIC_EMAILS.support}
              </a>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-white p-6">
          <div className="flex items-start gap-3">
            <Mail className="mt-0.5 text-forest" size={24} />
            <div>
              <p className="font-medium text-charcoal">Booking Assistance</p>
              <p className="mt-1 text-sm text-charcoal-muted">
                Booking status, payments, confirmations, and trip changes.
              </p>
              <a
                href={`mailto:${PUBLIC_EMAILS.bookings}`}
                className="mt-2 inline-block text-forest hover:text-forest-light"
              >
                {PUBLIC_EMAILS.bookings}
              </a>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-white p-6">
          <div className="flex items-start gap-3">
            <Mail className="mt-0.5 text-forest" size={24} />
            <div>
              <p className="font-medium text-charcoal">General Contact</p>
              <p className="mt-1 text-sm text-charcoal-muted">
                Business enquiries and general communication.
              </p>
              <a
                href={`mailto:${PUBLIC_EMAILS.general}`}
                className="mt-2 inline-block text-forest hover:text-forest-light"
              >
                {PUBLIC_EMAILS.general}
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
