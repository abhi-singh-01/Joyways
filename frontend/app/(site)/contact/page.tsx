import type { Metadata } from "next";
import { PageHero } from "@/components/layout/PageHero";
import { ContactEnquiryForm } from "@/components/contact/ContactEnquiryForm";
import {
  DEFAULT_WHATSAPP_MESSAGE,
  PUBLIC_CALL_NUMBER,
  PUBLIC_EMAILS,
  PUBLIC_WHATSAPP_NUMBER,
  formatPhoneDisplay,
  telUrl,
  whatsappUrl,
} from "@/lib/publicContact";

export const metadata: Metadata = {
  title: "Contact | JOYWAY",
  description: "Get in touch with JOYWAY for group travel enquiries, bookings, and support.",
  openGraph: {
    title: "Contact | JOYWAY",
    description: "Call, WhatsApp, or write to JOYWAY. We typically respond within one business day.",
    url: "https://joyway.in/contact",
  },
  alternates: {
    canonical: "https://joyway.in/contact",
  },
};

const emails = [
  {
    title: "Customer support",
    description: "Help, complaints, refunds, and general queries.",
    href: `mailto:${PUBLIC_EMAILS.support}`,
    label: PUBLIC_EMAILS.support,
  },
  {
    title: "Booking assistance",
    description: "Booking status, payments, confirmations, and trip changes.",
    href: `mailto:${PUBLIC_EMAILS.bookings}`,
    label: PUBLIC_EMAILS.bookings,
  },
  {
    title: "General contact",
    description: "Business enquiries and partnerships.",
    href: `mailto:${PUBLIC_EMAILS.general}`,
    label: PUBLIC_EMAILS.general,
  },
];

export default function ContactPage() {
  const waLink = whatsappUrl(DEFAULT_WHATSAPP_MESSAGE);
  const callDisplay = formatPhoneDisplay(PUBLIC_CALL_NUMBER);
  const whatsappDisplay = formatPhoneDisplay(PUBLIC_WHATSAPP_NUMBER);

  return (
    <>
      <PageHero
        illustrated
        eyebrow="Contact"
        title="Plan your next group journey"
        description="Questions about a trip, group size, or booking? Reach out — we typically respond within one business day."
      />

      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8 lg:py-20">
        <div className="grid gap-10 lg:grid-cols-12 lg:gap-16">
          <div className="lg:col-span-5">
            <h2 className="font-display text-2xl font-semibold text-charcoal sm:text-3xl">
              Talk to us.
            </h2>
            <p className="mt-3 max-w-md text-charcoal-muted">
              Call or WhatsApp for a quick answer. Use the form for detailed trip or group
              enquiries — we reply by email.
            </p>

            <ul className="mt-10 divide-y divide-border border-y border-border">
              <li className="py-5">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-forest">Call</p>
                <a
                  href={telUrl(PUBLIC_CALL_NUMBER)}
                  className="mt-1 block font-display text-2xl font-semibold text-charcoal hover:text-forest"
                >
                  {callDisplay}
                </a>
              </li>
              <li className="py-5">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-forest">
                  WhatsApp
                </p>
                <a
                  href={waLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-1 block font-display text-2xl font-semibold text-charcoal hover:text-forest"
                >
                  {whatsappDisplay}
                </a>
              </li>
              <li className="py-5">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-forest">
                  Response
                </p>
                <p className="mt-1 font-display text-2xl font-semibold text-charcoal">
                  Within 24 hours
                </p>
                <p className="mt-1 text-sm text-charcoal-muted">On business days</p>
              </li>
            </ul>
          </div>

          <div className="lg:col-span-7">
            <ContactEnquiryForm />
          </div>
        </div>

        <div className="mt-20">
          <h2 className="font-display text-2xl font-semibold text-charcoal">Write by email</h2>
          <ul className="mt-8 divide-y divide-border border-y border-border">
            {emails.map((item) => (
              <li
                key={item.title}
                className="grid gap-2 py-6 sm:grid-cols-[minmax(12rem,16rem)_1fr_auto] sm:items-baseline"
              >
                <h3 className="font-display text-lg font-semibold text-charcoal">{item.title}</h3>
                <p className="text-sm text-charcoal-muted">{item.description}</p>
                <a
                  href={item.href}
                  className="text-sm font-semibold text-forest hover:text-forest-light"
                >
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </>
  );
}
