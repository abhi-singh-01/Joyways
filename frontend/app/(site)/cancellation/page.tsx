import type { Metadata } from "next";
import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import { PUBLIC_EMAILS } from "@/lib/publicEmails";
import {
  PUBLIC_CALL_NUMBER,
  formatPhoneDisplay,
  telUrl,
} from "@/lib/publicContact";

export const metadata: Metadata = {
  title: "Cancellation & Refund Policy | JOYWAY",
  description:
    "Cancellation and refund terms for JOYWAY group travel bookings — timelines, eligibility, and how to request a refund.",
};

const lastUpdated = "2 September 2026";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="scroll-mt-24">
      <h2 className="font-display text-xl font-semibold text-charcoal">{title}</h2>
      <div className="mt-3 space-y-3 text-base leading-relaxed text-charcoal-muted">
        {children}
      </div>
    </section>
  );
}

export default function CancellationPage() {
  return (
    <>
      <PageHero
        eyebrow="Legal"
        title="Cancellation & Refund Policy"
        description="How cancellations and refunds work for group travel bookings on JOYWAY."
      />

      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
        <p className="text-sm text-charcoal-muted">Last updated: {lastUpdated}</p>

        <div className="mt-10 space-y-10">
          <Section title="1. Overview">
            <p>
              This Cancellation &amp; Refund Policy (&quot;Policy&quot;) applies to bookings made on
              JOYWAY (&quot;we&quot;, &quot;us&quot;, or &quot;our&quot;) at{" "}
              <a href="https://joyway.in" className="text-forest hover:text-forest-light">
                joyway.in
              </a>
              . The Website is developed and managed by{" "}
              <strong>AJS Technologies Pvt Ltd.</strong>
            </p>
            <p>
              Cancellation terms can vary by trip. The policy shown on each package detail page and
              in your booking confirmation is part of your booking agreement. This page explains
              general rules that apply across JOYWAY unless a package states otherwise.
            </p>
          </Section>

          <Section title="2. Package-specific cancellation terms">
            <p>
              Every trip on JOYWAY includes a cancellation policy on its detail page under
              &quot;Cancellation Policy&quot;. That section shows the refund percentages or charges
              that apply based on how many days before departure you cancel.
            </p>
            <p>
              A typical group trip on JOYWAY may use a schedule like the following unless your
              trip page states different terms:
            </p>
            <ul className="list-disc space-y-2 pl-5">
              <li>
                <strong className="text-charcoal">7 or more days</strong> before departure:{" "}
                <strong className="text-charcoal">80% refund</strong> of the amount paid
              </li>
              <li>
                <strong className="text-charcoal">3 to 7 days</strong> before departure:{" "}
                <strong className="text-charcoal">50% refund</strong> of the amount paid
              </li>
              <li>
                <strong className="text-charcoal">Less than 3 days</strong> before departure:{" "}
                <strong className="text-charcoal">no refund</strong>
              </li>
            </ul>
            <p>
              Always check the cancellation policy on your chosen trip before booking. If there is
              any conflict between this general Policy and the policy on a specific package, the
              package policy prevails for that booking.
            </p>
          </Section>

          <Section title="3. How to cancel a booking">
            <p>The cancellation process depends on your booking and payment status:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li>
                <strong className="text-charcoal">Unpaid bookings:</strong> you may cancel online
                using{" "}
                <Link href="/bookings/lookup" className="text-forest hover:text-forest-light">
                  booking lookup
                </Link>{" "}
                with your booking reference and email address, provided payment has not been
                completed.
              </li>
              <li>
                <strong className="text-charcoal">Paid or confirmed bookings:</strong> online
                self-cancellation is not available once payment is confirmed. Please contact our
                support team to request cancellation and discuss applicable refunds.
              </li>
              <li>
                <strong className="text-charcoal">By email or phone:</strong> you may also write to
                us at{" "}
                <a
                  href={`mailto:${PUBLIC_EMAILS.bookings}`}
                  className="text-forest hover:text-forest-light"
                >
                  {PUBLIC_EMAILS.bookings}
                </a>{" "}
                or call{" "}
                <a href={telUrl(PUBLIC_CALL_NUMBER)} className="text-forest hover:text-forest-light">
                  {formatPhoneDisplay(PUBLIC_CALL_NUMBER)}
                </a>{" "}
                with your booking reference, registered email, and reason for cancellation.
              </li>
            </ul>
            <p>
              Cancellations are effective from the date and time we acknowledge them in writing
              (email or booking status update), not from the date you first contact us.
            </p>
          </Section>

          <Section title="4. Refund eligibility">
            <p>Refunds, where applicable, are calculated according to the package cancellation policy in effect at the time of your booking. The following general rules also apply:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li>
                Refunds apply only to amounts actually paid to JOYWAY for the booking. Optional
                add-ons, third-party costs already incurred, or non-refundable supplier charges may
                be deducted.
              </li>
              <li>
                <strong className="text-charcoal">No-shows</strong> (failure to arrive at the
                departure point on time), late arrivals, or missing required ID documents may be
                treated as cancellation by the traveler and may not qualify for any refund.
              </li>
              <li>
                Partial cancellations (reducing the number of travelers on a group booking) may not
                always be possible once a trip is confirmed. Contact support to check availability
                and any fare difference.
              </li>
              <li>
                Bookings made using promotional offers, special fares, or limited-seat departures
                may carry stricter non-refundable conditions stated at checkout.
              </li>
              <li>
                If a traveler is removed from a trip due to misconduct, safety concerns, or breach
                of our{" "}
                <Link href="/terms" className="text-forest hover:text-forest-light">
                  Terms &amp; Conditions
                </Link>
                , no refund may be due.
              </li>
            </ul>
          </Section>

          <Section title="5. Refund processing">
            <p>
              Approved refunds are processed to the <strong className="text-charcoal">original payment method</strong> used at checkout (for example, the same card or UPI account via Razorpay), unless we agree otherwise in writing.
            </p>
            <ul className="list-disc space-y-2 pl-5">
              <li>
                Refund requests are reviewed by our team after cancellation is confirmed. You will
                receive email confirmation when a refund is approved or if additional information
                is needed.
              </li>
              <li>
                Once initiated, refunds typically appear within{" "}
                <strong className="text-charcoal">5 to 10 business days</strong>, depending on your
                bank, card issuer, or payment provider. JOYWAY does not control bank-side
                processing times.
              </li>
              <li>
                Payment gateway charges, currency conversion fees, or bank fees levied by third
                parties may be non-refundable where permitted by law and stated in the applicable
                package policy.
              </li>
              <li>
                If a refund cannot be returned to the original payment method (for example, an
                expired card), we will contact you to arrange an alternative verified method.
              </li>
            </ul>
          </Section>

          <Section title="6. Cancellation by JOYWAY">
            <p>
              We may cancel or reschedule a trip due to insufficient group size, operational
              requirements, safety concerns, or events beyond our control (see force majeure below).
            </p>
            <p>If we cancel a trip before departure, we will generally offer one of the following:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li>A full refund of amounts paid to JOYWAY for that booking, or</li>
              <li>
                Transfer to an alternative departure date or comparable trip, subject to your
                agreement.
              </li>
            </ul>
            <p>
              We are not responsible for incidental costs you may have incurred separately (such as
              personal travel to the departure city, leave from work, or pre-booked accommodation
              outside the package) unless explicitly stated otherwise.
            </p>
          </Section>

          <Section title="7. Force majeure">
            <p>
              In cases of natural disasters, government restrictions, strikes, road blockades,
              pandemics, or other events beyond our reasonable control, trips may be modified,
              rescheduled, or cancelled. Refunds or credits in such situations will be handled
              fairly based on costs recovered from suppliers and the specific circumstances. We
              will communicate available options to affected travelers as soon as practicable.
            </p>
          </Section>

          <Section title="8. Changes to departure date or itinerary">
            <p>
              Minor itinerary adjustments (timing, route, or hotel of comparable standard) may occur
              for operational reasons. These do not automatically entitle you to a full refund.
            </p>
            <p>
              If a <strong className="text-charcoal">material change</strong> is made to your
              confirmed trip (such as a significant date change or removal of a major inclusion),
              we will notify you and offer a suitable alternative or refund in line with applicable
              law and the package terms.
            </p>
          </Section>

          <Section title="9. Booking expiry">
            <p>
              Bookings that remain unpaid beyond the allowed payment window may be automatically
              expired or cancelled, and reserved seats released. No refund applies in such cases
              because payment was not completed.
            </p>
          </Section>

          <Section title="10. Disputes and chargebacks">
            <p>
              If you have a concern about a cancellation or refund, please contact us first so we
              can resolve it. Initiating a payment chargeback without contacting us may delay
              resolution and affect your ability to book future trips. We will cooperate with
              legitimate dispute processes through our payment partner.
            </p>
          </Section>

          <Section title="11. Changes to this policy">
            <p>
              We may update this Policy from time to time. The &quot;Last updated&quot; date at the
              top reflects the current version. Changes apply to new bookings made after the update
              unless otherwise stated. The cancellation terms on your booking confirmation remain
              binding for that booking.
            </p>
          </Section>

          <Section title="12. Contact us">
            <p>For cancellation requests, refund status, or questions about this Policy:</p>
            <ul className="list-none space-y-2 pl-0">
              <li>
                <strong className="text-charcoal">Bookings &amp; refunds:</strong>{" "}
                <a
                  href={`mailto:${PUBLIC_EMAILS.bookings}`}
                  className="text-forest hover:text-forest-light"
                >
                  {PUBLIC_EMAILS.bookings}
                </a>
              </li>
              <li>
                <strong className="text-charcoal">Support:</strong>{" "}
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
                <strong className="text-charcoal">Online:</strong>{" "}
                <Link href="/bookings/lookup" className="text-forest hover:text-forest-light">
                  Manage your booking
                </Link>
                {" · "}
                <Link href="/contact" className="text-forest hover:text-forest-light">
                  Contact page
                </Link>
                {" · "}
                <Link href="/terms" className="text-forest hover:text-forest-light">
                  Terms &amp; Conditions
                </Link>
              </li>
            </ul>
          </Section>
        </div>
      </div>
    </>
  );
}
