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
  title: "Privacy Policy | JOYWAY",
  description:
    "How JOYWAY collects, uses, and protects your personal information when you browse trips or make a group travel booking.",
};

const lastUpdated = "9 September 2026";

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

export default function PrivacyPage() {
  return (
    <>
      <PageHero
        eyebrow="Legal"
        title="Privacy Policy"
        description="How we collect, use, store, and protect your information when you use JOYWAY."
      />

      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
        <p className="text-sm text-charcoal-muted">Last updated: {lastUpdated}</p>

        <div className="mt-10 space-y-10">
          <Section title="1. Introduction">
            <p>
              JOYWAY (&quot;we&quot;, &quot;us&quot;, or &quot;our&quot;) respects your privacy. This
              Privacy Policy explains what personal information we collect when you visit{" "}
              <a href="https://joyway.in" className="text-forest hover:text-forest-light">
                joyway.in
              </a>{" "}
              (the &quot;Website&quot;), make a booking, upload traveller documents, or contact us.
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
              </a>
              . All rights in JOYWAY and this Website are reserved by JOYWAY. By using the Website,
              you agree to the collection and use of information as described in this policy. If you
              do not agree, please do not use the Website.
            </p>
          </Section>

          <Section title="2. Information we collect">
            <p>Depending on how you use JOYWAY, we may collect the following:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li>
                <strong className="text-charcoal">Contact details:</strong> name, email address,
                and mobile number when you enquire, book a trip, or look up a booking.
              </li>
              <li>
                <strong className="text-charcoal">Booking details:</strong> trip selected, number
                of travellers, booking reference, booking status, and payment status.
              </li>
              <li>
                <strong className="text-charcoal">Traveller information:</strong> full name, date
                of birth, gender, mobile number, email address, and emergency contact for each
                person on your booking.
              </li>
              <li>
                <strong className="text-charcoal">Identity documents:</strong> government-issued ID
                type, reference number (where applicable), and uploaded document files (such as
                Aadhaar, passport, or other ID permitted at checkout) required to confirm traveller
                details for group travel.
              </li>
              <li>
                <strong className="text-charcoal">Payment-related information:</strong> transaction
                references, order IDs, and payment status from our payment partner. We do not store
                your full card, UPI PIN, or net-banking credentials on our servers.
              </li>
              <li>
                <strong className="text-charcoal">Communications:</strong> messages you send us by
                email, phone, WhatsApp, or through the contact form.
              </li>
              <li>
                <strong className="text-charcoal">Technical data:</strong> basic server logs (such
                as IP address, browser type, and request time) used for security, fraud prevention,
                and troubleshooting.
              </li>
            </ul>
          </Section>

          <Section title="3. How we use your information">
            <p>We use personal information for legitimate business purposes, including to:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li>Process and manage your group travel bookings.</li>
              <li>Verify traveller identity and fulfil operational or regulatory requirements.</li>
              <li>Collect payments and issue booking confirmations.</li>
              <li>
                Share necessary traveller details with hotels, transport operators, and other
                service partners involved in your trip.
              </li>
              <li>
                Send booking updates, trip information, and customer support responses by email,
                phone, or WhatsApp.
              </li>
              <li>Respond to enquiries, complaints, cancellations, and refund requests.</li>
              <li>Maintain records for accounting, audit, and legal compliance.</li>
              <li>Protect the Website, our customers, and our business from fraud or misuse.</li>
            </ul>
            <p>
              We do not sell your personal information to third parties for marketing purposes.
            </p>
          </Section>

          <Section title="4. Legal basis">
            <p>
              We process your information based on one or more of the following, as applicable
              under Indian law (including the Digital Personal Data Protection Act, 2023):
            </p>
            <ul className="list-disc space-y-2 pl-5">
              <li>
                <strong className="text-charcoal">Performance of a contract:</strong> to complete
                your booking and deliver the travel services you requested.
              </li>
              <li>
                <strong className="text-charcoal">Consent:</strong> where you voluntarily provide
                information, upload documents, or contact us.
              </li>
              <li>
                <strong className="text-charcoal">Legal obligation:</strong> where we must retain
                or disclose information to comply with applicable laws, court orders, or government
                requests.
              </li>
              <li>
                <strong className="text-charcoal">Legitimate interests:</strong> to operate,
                secure, and improve our services, provided your rights are not overridden.
              </li>
            </ul>
          </Section>

          <Section title="5. Sharing your information">
            <p>We may share personal information only as needed, including with:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li>
                <strong className="text-charcoal">Payment processors:</strong> Razorpay and related
                payment service providers to process online payments securely.
              </li>
              <li>
                <strong className="text-charcoal">Travel and logistics partners:</strong> hotels,
                coach operators, guides, and other vendors who need traveller details to deliver
                your trip.
              </li>
              <li>
                <strong className="text-charcoal">Technology providers:</strong> cloud hosting,
                email delivery, and secure document storage services (including Microsoft Azure Blob
                Storage) that help us run the Website and store uploaded files.
              </li>
              <li>
                <strong className="text-charcoal">Professional advisers:</strong> accountants,
                auditors, or legal counsel where required.
              </li>
              <li>
                <strong className="text-charcoal">Authorities:</strong> law enforcement, courts, or
                regulators when required by law or to protect rights, safety, and property.
              </li>
            </ul>
            <p>
              We require service providers to handle your data only for the purposes we specify and
              to apply appropriate security measures.
            </p>
          </Section>

          <Section title="6. Document storage and security">
            <p>
              Traveller ID documents and package images are stored in secure cloud storage. Access
              is restricted to authorised personnel and systems required to operate bookings and
              trips. Uploaded documents are subject to a maximum file size limit per upload.
            </p>
            <p>
              We use technical and organisational measures such as encrypted connections (HTTPS),
              access controls, and secure payment processing. No method of transmission or storage
              is completely secure; we cannot guarantee absolute security, but we work to protect
              your information against unauthorised access, loss, or misuse.
            </p>
          </Section>

          <Section title="7. Payment information">
            <p>
              Online payments on JOYWAY are processed by Razorpay. When you pay, you enter payment
              details on Razorpay&apos;s secure checkout. We receive confirmation of payment status
              and transaction identifiers needed to confirm your booking — not your full payment
              credentials.
            </p>
            <p>
              Razorpay&apos;s handling of your payment data is governed by their privacy policy and
              terms. We recommend reviewing those before completing a payment.
            </p>
          </Section>

          <Section title="8. Cookies and similar technologies">
            <p>
              The public Website does not use advertising or third-party analytics cookies. We may
              use strictly necessary cookies or similar session mechanisms for security and admin
              access to our internal dashboard. These are not used to track general browsing on the
              public site.
            </p>
          </Section>

          <Section title="9. How long we keep your information">
            <p>
              We retain personal information for as long as needed to fulfil your booking, provide
              customer support, meet legal and tax requirements, and resolve disputes. Booking and
              payment records are typically kept for the period required under applicable Indian
              laws and our internal retention schedule.
            </p>
            <p>
              Traveller identity documents may be deleted or anonymised after the trip is completed
              and any mandatory retention period has passed, unless a longer period is required by
              law or for an ongoing dispute.
            </p>
          </Section>

          <Section title="10. Your rights and choices">
            <p>Subject to applicable law, you may have the right to:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li>Request access to personal information we hold about you.</li>
              <li>Request correction of inaccurate or incomplete information.</li>
              <li>
                Withdraw consent where processing is based on consent (this may affect our ability
                to complete a booking).
              </li>
              <li>
                Request deletion of information where we are not legally required to retain it.
              </li>
              <li>
                Raise a grievance regarding how we handle your personal data with our Grievance
                Officer (contact details below).
              </li>
            </ul>
            <p>
              To exercise these rights, email us with your booking reference (if applicable) and
              enough detail for us to verify your identity. We will respond within a reasonable
              time as required by law.
            </p>
          </Section>

          <Section title="11. Children&apos;s privacy">
            <p>
              Bookings on JOYWAY must be made by an adult aged 18 or above. We may process
              information about minors when included as travellers on a booking made by a parent or
              legal guardian. If you believe we have collected a minor&apos;s information without
              appropriate authority, please contact us so we can review and take appropriate
              action.
            </p>
          </Section>

          <Section title="12. Links to other websites">
            <p>
              Our Website may link to third-party sites (for example, payment checkout or WhatsApp).
              We are not responsible for the privacy practices of those sites. Please read their
              privacy policies before sharing information with them.
            </p>
          </Section>

          <Section title="13. Changes to this policy">
            <p>
              We may update this Privacy Policy from time to time. The &quot;Last updated&quot;
              date at the top will reflect the latest version. Material changes will be posted on
              this page. Continued use of the Website after changes are posted constitutes acceptance
              of the updated policy.
            </p>
          </Section>

          <Section title="14. Contact us">
            <p>
              For privacy-related questions, data access requests, or grievances, contact:
            </p>
            <ul className="list-none space-y-2 pl-0">
              <li>
                <strong className="text-charcoal">Data fiduciary:</strong> JOYWAY
              </li>
              <li>
                <strong className="text-charcoal">Grievance Officer:</strong> Privacy &amp; Grievance
                Officer, JOYWAY
              </li>
              <li>
                <strong className="text-charcoal">Grievance email:</strong>{" "}
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
                <strong className="text-charcoal">Location:</strong> Delhi, India
              </li>
              <li>
                <strong className="text-charcoal">More help:</strong>{" "}
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
