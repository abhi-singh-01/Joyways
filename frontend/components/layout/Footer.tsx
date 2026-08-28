import Link from "next/link";
import { Mail, MapPin } from "lucide-react";
import { PUBLIC_EMAILS } from "@/lib/publicEmails";

const footerLinks = {
  explore: [
    { href: "/trips", label: "All Trips" },
    { href: "/about", label: "About Us" },
    { href: "/contact", label: "Contact" },
  ],
  legal: [
    { href: "/terms", label: "Terms & Conditions" },
    { href: "/privacy", label: "Privacy Policy" },
    { href: "/cancellation", label: "Cancellation Policy" },
  ],
};

export function Footer() {
  return (
    <footer className="border-t border-border bg-charcoal text-white">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="grid gap-12 md:grid-cols-2 lg:grid-cols-4">
          <div className="lg:col-span-2">
            <p className="font-display text-3xl font-semibold">JOYWAY</p>
            <p className="mt-2 text-accent-light">Find Joy in Every Journey.</p>
            <p className="mt-4 max-w-md text-sm leading-relaxed text-white/70">
              Group travel packages from Delhi to India&apos;s most beautiful
              destinations. Transport, stay, and memories — all in one package.
            </p>
            <div className="mt-6 space-y-3 text-sm text-white/70">
              <p className="flex items-start gap-2">
                <Mail size={16} className="mt-0.5 shrink-0 text-accent" />
                <span>
                  <span className="block text-white/50">Customer Support</span>
                  <a href={`mailto:${PUBLIC_EMAILS.support}`} className="hover:text-white">
                    {PUBLIC_EMAILS.support}
                  </a>
                </span>
              </p>
              <p className="flex items-start gap-2">
                <Mail size={16} className="mt-0.5 shrink-0 text-accent" />
                <span>
                  <span className="block text-white/50">Booking Assistance</span>
                  <a href={`mailto:${PUBLIC_EMAILS.bookings}`} className="hover:text-white">
                    {PUBLIC_EMAILS.bookings}
                  </a>
                </span>
              </p>
              <p className="flex items-start gap-2">
                <Mail size={16} className="mt-0.5 shrink-0 text-accent" />
                <span>
                  <span className="block text-white/50">General Contact</span>
                  <a href={`mailto:${PUBLIC_EMAILS.general}`} className="hover:text-white">
                    {PUBLIC_EMAILS.general}
                  </a>
                </span>
              </p>
              <p className="flex items-center gap-2">
                <MapPin size={16} className="text-accent" />
                Delhi, India
              </p>
            </div>
          </div>

          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white/50">
              Explore
            </h3>
            <ul className="mt-4 space-y-3">
              {footerLinks.explore.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-white/70 transition-colors hover:text-white"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white/50">
              Legal
            </h3>
            <ul className="mt-4 space-y-3">
              {footerLinks.legal.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-white/70 transition-colors hover:text-white"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="mt-12 flex flex-col items-center justify-between gap-4 border-t border-white/10 pt-8 sm:flex-row">
          <p className="text-sm text-white/50">
            &copy; {new Date().getFullYear()} JOYWAY. All rights reserved.
          </p>
          <p className="text-sm text-white/50">
            <a href="https://joyway.in" className="hover:text-white">
              joyway.in
            </a>
          </p>
        </div>
      </div>
    </footer>
  );
}
