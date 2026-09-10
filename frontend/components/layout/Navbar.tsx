"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { Menu, X } from "lucide-react";
import { motion, useScroll, useMotionValueEvent } from "framer-motion";
import { JoywayLogo } from "@/components/layout/JoywayLogo";
import { cn } from "@/lib/utils";

const navLinks = [
  { href: "/trips", label: "Trips" },
  { href: "/about", label: "About" },
  { href: "/contact", label: "Contact" },
  { href: "/bookings/lookup", label: "Find booking" },
];

export function Navbar() {
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const { scrollY } = useScroll();
  const onHome = pathname === "/";
  const overlay = onHome && !mobileOpen && !scrolled;

  useMotionValueEvent(scrollY, "change", (value) => {
    setScrolled(value > 24);
  });

  function isActive(href: string) {
    if (href === "/") return pathname === "/";
    return pathname === href || pathname.startsWith(`${href}/`);
  }

  function desktopLinkClass(active: boolean): string {
    if (overlay) {
      return active ? "text-white" : "text-white/75 hover:text-white";
    }
    return active ? "text-forest" : "text-charcoal-muted hover:text-forest";
  }

  function mobileLinkClass(active: boolean): string {
    if (active) {
      return overlay ? "bg-white/10 text-white" : "bg-forest/10 text-forest";
    }
    return overlay ? "text-white hover:bg-white/10" : "text-charcoal hover:bg-surface";
  }

  return (
    <motion.header
      initial={false}
      animate={
        overlay
          ? {
              backgroundColor: "rgba(8, 28, 21, 0)",
              boxShadow: "0 0 0 rgba(0,0,0,0)",
            }
          : {
              backgroundColor: "rgba(250, 249, 247, 0.94)",
              boxShadow: scrolled ? "0 4px 24px rgba(27, 67, 50, 0.06)" : "0 0 0 rgba(0,0,0,0)",
            }
      }
      transition={{ duration: 0.25 }}
      className={cn(
        "z-50 border-b",
        onHome ? "fixed inset-x-0 top-0" : "sticky top-0",
        overlay ? "border-white/10" : "border-border/60 backdrop-blur-lg"
      )}
    >
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <JoywayLogo
          height={40}
          priority
          layout="compact"
          showTagline
          variant={overlay ? "onDark" : "default"}
        />

        <nav className="hidden items-center gap-1 md:flex">
          {navLinks.map((link) => {
            const active = isActive(link.href);
            return (
              <Link
                key={link.href}
                href={link.href}
                className={cn(
                  "group relative rounded-full px-4 py-2 text-sm font-medium transition-colors",
                  desktopLinkClass(active)
                )}
              >
                {link.label}
                <span
                  className={cn(
                    "absolute bottom-1 left-4 right-4 h-0.5 rounded-full transition-all duration-300",
                    overlay ? "bg-white" : "bg-forest",
                    active ? "opacity-100" : "opacity-0 group-hover:opacity-60"
                  )}
                />
              </Link>
            );
          })}
          <Link href="/trips" className="ml-4">
            <motion.span
              whileHover={{ scale: 1.03 }}
              whileTap={{ scale: 0.98 }}
              className={cn(
                "inline-block rounded-full px-5 py-2.5 text-sm font-medium shadow-sm transition-colors",
                overlay
                  ? "bg-white text-forest hover:bg-accent-light"
                  : "bg-forest text-white hover:bg-forest-light"
              )}
            >
              Book a trip
            </motion.span>
          </Link>
        </nav>

        <button
          type="button"
          className={cn("rounded-lg p-2 md:hidden", overlay ? "text-white" : "text-charcoal")}
          onClick={() => setMobileOpen(!mobileOpen)}
          aria-label={mobileOpen ? "Close menu" : "Open menu"}
        >
          {mobileOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      <div
        className={cn(
          "border-t md:hidden",
          mobileOpen ? "block min-h-[calc(100svh-4rem)]" : "hidden",
          overlay ? "border-white/10 bg-forest-dark" : "border-border bg-off-white"
        )}
      >
        <nav className="flex flex-col gap-1 px-4 py-4">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={cn(
                "rounded-lg px-3 py-3 text-base font-medium",
                mobileLinkClass(isActive(link.href))
              )}
              onClick={() => setMobileOpen(false)}
            >
              {link.label}
            </Link>
          ))}
          <Link
            href="/trips"
            className={cn(
              "mt-2 rounded-full px-5 py-3 text-center text-base font-medium",
              overlay ? "bg-white text-forest" : "bg-forest text-white"
            )}
            onClick={() => setMobileOpen(false)}
          >
            Book a trip
          </Link>
        </nav>
      </div>
    </motion.header>
  );
}
