import type { Metadata } from "next";
import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import { PUBLIC_EMAILS } from "@/lib/publicEmails";
import { DEVELOPER_SITE_LABEL, DEVELOPER_SITE_URL } from "@/lib/publicCredits";
import {
  PUBLIC_CALL_NUMBER,
  formatPhoneDisplay,
  telUrl,
} from "@/lib/publicContact";

export const metadata: Metadata = {
  title: "Terms & Conditions | JOYWAY",
  description:
    "Terms and conditions for using JOYWAY — group travel packages, bookings, and payments on joyway.in.",
};

const lastUpdated = "9 September 2026";

function Section({ title, children }: Readonly<{ title: string; children: React.ReactNode }>) {
  return (
    <section className="scroll-mt-24">
      <h2 className="font-display text-xl font-semibold text-charcoal">{title}</h2>
      <div className="mt-3 space-y-3 text-base leading-relaxed text-charcoal-muted">
        {children}
      </div>
    </section>
  );
}

export default function TermsPage() {
  return (
    <>
      <PageHero
        eyebrow="Legal"
        title="Terms & Conditions"
        description="Please read these terms carefully before browsing trips or making a booking on JOYWAY."
      />

      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
        <p className="text-sm text-charcoal-muted">Last updated: {lastUpdated}</p>

        <div className="mt-10 space-y-10">
          <Section title="1. Introduction">
            <p>
              Welcome to JOYWAY (&quot;we&quot;, &quot;us&quot;, or &quot;our&quot;). These Terms &
              Conditions (&quot;Terms&quot;) govern your access to and use of our website at{" "}
              <a href="https://joyway.in" className="text-forest hover:text-forest-light">
                joyway.in
              </a>{" "}
              (the &quot;Website&quot;) and any bookings made through it.
            </p>
            <p>
              The Website is developed by{" "}
              <a
                href={DEVELOPER_SITE_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="text-forest hover:text-forest-light"
              >
                {DEVELOPER_SITE_LABEL}
              </a>. All rights in JOYWAY and this Website are reserved by JOYWAY. By using the Website,
              creating a booking, or making a payment, you agree to be bound by these Terms. If you
              do not agree, please do not use the Website.
            </p>
          </Section>

          <Section title="2. About our services">
            <p>
              JOYWAY is a group travel platform that helps travelers from Delhi and nearby regions
              discover and book curated group tour packages. Packages may include transport,
              accommodation, meals, sightseeing, and other inclusions as described on each trip
              page.
            </p>
            <p>
              We act as an organizer and facilitator of group travel experiences. Specific
              inclusions, exclusions, departure points, dates, and pricing are listed on each package
              detail page and form part of your booking agreement.
            </p>
          </Section>

          <Section title="3. Eligibility">
            <p>
              You must be at least 18 years old to make a booking on JOYWAY. If you book on behalf
              of others (including minors), you confirm that you have authority to accept these
              Terms on their behalf and that all traveler information you provide is accurate.
            </p>
          </Section>

          <Section title="4. Account information & bookings">
            <p>When you book a trip, you agree to provide accurate and complete information, including:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li>Full name, contact number, and email address</li>
              <li>Number of travelers and any details required for the trip</li>
              <li>Valid government-issued ID details where requested for travel compliance</li>
            </ul>
            <p>
              A booking is confirmed only after successful payment of the full trip amount and
              receipt of a booking confirmation from JOYWAY by email. We reserve the
              right to decline or cancel a booking if information is incorrect, incomplete, or if
              the trip cannot be fulfilled.
            </p>
          </Section>

          <Section title="5. Pricing & payments">
            <p>
              All prices displayed on the Website are in Indian Rupees (INR) unless stated otherwise.
              Prices are subject to availability and may change before you complete payment. The
              price shown at checkout is the price that applies to your booking.
            </p>
            <p>
              Payments are processed securely through our payment partners (including Razorpay). By
              completing payment, you authorize us to charge the selected amount for the booked
              package. JOYWAY does not store your full card or UPI credentials on our servers.
            </p>
            <p>
              Any taxes, permits, personal expenses, optional activities, or items marked as
              &quot;not included&quot; on the package page are payable separately by you unless
              explicitly stated otherwise.
            </p>
          </Section>

          <Section title="6. Package inclusions & changes">
            <p>
              Each package page lists what is included and excluded. Itineraries, hotels, transport
              arrangements, and schedules are indicative and may be adjusted due to weather, road
              conditions, local regulations, safety concerns, or operational requirements.
            </p>
            <p>
              If a material change is required before departure, we will make reasonable efforts to
              inform you in advance and offer a suitable alternative of comparable value where
              possible.
            </p>
          </Section>

          <Section title="7. Traveler responsibilities">
            <p>As a traveler, you are responsible for:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li>Arriving at the designated departure point on time</li>
              <li>Carrying valid photo ID and any documents required for the trip</li>
              <li>Following instructions from trip coordinators, drivers, and local guides</li>
              <li>Behaving respectfully toward fellow travelers, staff, and local communities</li>
              <li>Ensuring you are medically fit to travel and disclosing relevant health concerns</li>
            </ul>
            <p>
              JOYWAY may refuse participation or remove a traveler from a trip without refund if
              their conduct endangers others, violates law, or disrupts the group experience.
            </p>
          </Section>

          <Section title="8. Cancellation & refunds">
            <p>
              Cancellation terms vary by package. The applicable cancellation and refund policy for
              your booking is shown on the package detail page and in your booking confirmation.
            </p>
            <p>
              For general guidance, please also read our{" "}
              <Link href="/cancellation" className="text-forest hover:text-forest-light">
                Cancellation & Refund Policy
              </Link>
              . Refund processing times depend on the payment method and our payment partner&apos;s
              timelines. Approved refunds are typically processed to the original payment method.
            </p>
            <p>
              No-shows, late arrivals, or failure to carry required documents may be treated as
              cancellation by the traveler and may not qualify for a refund.
            </p>
          </Section>

          <Section title="9. Force majeure">
            <p>
              JOYWAY is not liable for failure or delay in performing obligations caused by events
              beyond our reasonable control, including natural disasters, strikes, government
              restrictions, pandemics, road blockades, or other unforeseen circumstances. In such
              cases, we may reschedule, modify, or cancel a trip and will communicate available
              options to affected travelers.
            </p>
          </Section>

          <Section title="10. Limitation of liability">
            <p>
              To the fullest extent permitted by applicable law, JOYWAY shall not be liable for
              indirect, incidental, or consequential losses, including
              loss of enjoyment, missed connections, or personal belongings left unattended during
              a trip.
            </p>
            <p>
              Our total liability for any claim relating to a booking shall not exceed the amount
              paid by you to JOYWAY for that specific booking. Nothing in these Terms limits
              liability that cannot be excluded under Indian law.
            </p>
          </Section>

          <Section title="11. Third-party services">
            <p>
              Some parts of a trip may be delivered by independent hotels, transport operators,
              activity providers, or guides. While we select partners carefully, JOYWAY is not
              responsible for acts or omissions of third parties beyond our reasonable control.
            </p>
          </Section>

          <Section title="12. Website use">
            <p>You agree not to:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li>Use the Website for unlawful, fraudulent, or abusive purposes</li>
              <li>Attempt to gain unauthorized access to our systems or other users&apos; data</li>
              <li>Scrape, copy, or reproduce Website content without written permission</li>
              <li>Interfere with the normal operation or security of the Website</li>
            </ul>
            <p>
              All content on the Website — including text, logos, images, and design — is owned by
              JOYWAY or its licensors and is protected by applicable intellectual property laws.
            </p>
          </Section>

          <Section title="13. Privacy">
            <p>
              Your personal data is handled in accordance with our{" "}
              <Link href="/privacy" className="text-forest hover:text-forest-light">
                Privacy Policy
              </Link>
              . By using the Website, you consent to the collection and use of information as
              described there.
            </p>
          </Section>

          <Section title="14. Governing law & disputes">
            <p>
              These Terms are governed by the laws of India. Any dispute arising from these Terms or
              your use of the Website shall be subject to the exclusive jurisdiction of the courts
              in Delhi, India, unless otherwise required by applicable consumer protection law.
            </p>
            <p>
              We encourage you to contact us first so we can try to resolve concerns promptly and
              amicably.
            </p>
          </Section>

          <Section title="15. Changes to these Terms">
            <p>
              We may update these Terms from time to time. The &quot;Last updated&quot; date at the
              top of this page will reflect the latest version. Continued use of the Website after
              changes are posted constitutes acceptance of the revised Terms.
            </p>
          </Section>

          <Section title="16. Contact us">
            <p>For questions about these Terms, bookings, or your trip, contact us:</p>
            <ul className="list-none space-y-2 pl-0">
              <li>
                <strong className="text-charcoal">Email:</strong>{" "}
                <a
                  href={`mailto:${PUBLIC_EMAILS.support}`}
                  className="text-forest hover:text-forest-light"
                >
                  {PUBLIC_EMAILS.support}
                </a>
              </li>
              <li>
                <strong className="text-charcoal">Phone:</strong>{" "}
                <a href={telUrl(PUBLIC_CALL_NUMBER)} className="text-forest hover:text-forest-light">
                  {formatPhoneDisplay(PUBLIC_CALL_NUMBER)}
                </a>
              </li>
              <li>
                <strong className="text-charcoal">Website:</strong>{" "}
                <Link href="/contact" className="text-forest hover:text-forest-light">
                  Contact page
                </Link>
              </li>
            </ul>
          </Section>
        </div>
      </div>
    </>
  );
}
