"use client";

import Link from "next/link";
import { useState } from "react";
import { Menu, X } from "lucide-react";
import { JoywayLogo } from "@/components/layout/JoywayLogo";
import { cn } from "@/lib/utils";

const navLinks = [
  { href: "/trips", label: "Trips" },
  { href: "/about", label: "About" },
  { href: "/contact", label: "Contact" },
];

export function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 border-b border-border/60 bg-off-white/90 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <JoywayLogo height={36} priority />

        <nav className="hidden items-center gap-8 md:flex">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="text-sm font-medium text-charcoal-muted transition-colors hover:text-forest"
            >
              {link.label}
            </Link>
          ))}
          <Link
            href="/trips"
            className="rounded-full bg-forest px-5 py-2.5 text-sm font-medium text-white transition-colors hover:bg-forest-light"
          >
            Explore Trips
          </Link>
        </nav>

        <button
          type="button"
          className="rounded-lg p-2 text-charcoal md:hidden"
          onClick={() => setMobileOpen(!mobileOpen)}
          aria-label={mobileOpen ? "Close menu" : "Open menu"}
        >
          {mobileOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      <div
        className={cn(
          "border-t border-border bg-off-white md:hidden",
          mobileOpen ? "block" : "hidden"
        )}
      >
        <nav className="flex flex-col gap-1 px-4 py-4">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="rounded-lg px-3 py-3 text-base font-medium text-charcoal hover:bg-surface"
              onClick={() => setMobileOpen(false)}
            >
              {link.label}
            </Link>
          ))}
          <Link
            href="/trips"
            className="mt-2 rounded-full bg-forest px-5 py-3 text-center text-base font-medium text-white"
            onClick={() => setMobileOpen(false)}
          >
            Explore Trips
          </Link>
        </nav>
      </div>
    </header>
  );
}
