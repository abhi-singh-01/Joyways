import type { Metadata } from "next";
import { Clock, Mail, MessageCircle, Phone } from "lucide-react";
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
  description: "Get in touch with JOYWAY for group travel enquiries.",
};

const contacts = [
  {
    icon: Mail,
    title: "Customer Support",
    description: "Help, complaints, refunds, and general queries.",
    href: `mailto:${PUBLIC_EMAILS.support}`,
    label: PUBLIC_EMAILS.support,
  },
  {
    icon: Mail,
    title: "Booking Assistance",
    description: "Booking status, payments, confirmations, and trip changes.",
    href: `mailto:${PUBLIC_EMAILS.bookings}`,
    label: PUBLIC_EMAILS.bookings,
  },
  {
    icon: Mail,
    title: "General Contact",
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
        eyebrow="We&apos;re here to help"
        title="Plan your next group journey"
        description="Questions about a trip, group size, or booking? Reach out — we typically respond within one business day."
      />

      <section className="mx-auto max-w-4xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="mb-8">
          <p className="mb-4 text-sm font-medium uppercase tracking-wider text-charcoal-muted">
            For more queries
          </p>
          <div className="flex flex-col gap-4 sm:flex-row">
            <a
              href={telUrl(PUBLIC_CALL_NUMBER)}
              className="flex flex-1 items-center gap-4 rounded-2xl border border-border bg-white p-6 transition-shadow hover:shadow-md"
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-forest/10 text-forest">
                <Phone size={24} />
              </div>
              <div>
                <p className="font-semibold text-charcoal">Call us</p>
                <p className="text-sm font-medium text-forest">{callDisplay}</p>
              </div>
            </a>

            <a
              href={waLink}
              target="_blank"
              rel="noopener noreferrer"
              className="flex flex-1 items-center gap-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-6 transition-shadow hover:shadow-md"
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-600 text-white">
                <MessageCircle size={24} />
              </div>
              <div>
                <p className="font-semibold text-charcoal">WhatsApp us</p>
                <p className="text-sm font-medium text-emerald-700">{whatsappDisplay}</p>
              </div>
            </a>

            <div className="flex flex-1 items-center gap-4 rounded-2xl border border-border bg-white p-6">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-forest/10 text-forest">
                <Clock size={24} />
              </div>
              <div>
                <p className="font-semibold text-charcoal">Response time</p>
                <p className="text-sm text-charcoal-muted">Within 24 hours on business days</p>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-12">
          <ContactEnquiryForm />
        </div>

        <div className="mt-12 space-y-4">
          {contacts.map(({ icon: Icon, title, description, href, label }) => (
            <div
              key={title}
              className="rounded-2xl border border-border bg-white p-6 transition-shadow hover:shadow-sm"
            >
              <div className="flex items-start gap-4">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-forest/10 text-forest">
                  <Icon size={22} />
                </div>
                <div>
                  <p className="font-semibold text-charcoal">{title}</p>
                  <p className="mt-1 text-sm text-charcoal-muted">{description}</p>
                  <a
                    href={href}
                    className="mt-3 inline-block text-sm font-medium text-forest hover:text-forest-light"
                  >
                    {label}
                  </a>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}
