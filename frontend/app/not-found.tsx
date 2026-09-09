import Link from "next/link";
import { Compass } from "lucide-react";
import { Navbar } from "@/components/layout/Navbar";
import { Footer } from "@/components/layout/Footer";
import { Button } from "@/components/ui/Button";

export default function NotFound() {
  return (
    <>
      <Navbar />
      <main className="flex-1">
        <div className="mx-auto flex min-h-[60vh] max-w-lg flex-col items-center justify-center px-4 py-20 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-surface">
            <Compass className="h-8 w-8 text-forest/60" />
          </div>
          <h1 className="mt-6 font-display text-3xl font-semibold text-charcoal">
            Page not found
          </h1>
          <p className="mt-3 text-charcoal-muted">
            This page does not exist or the link may be incorrect.
          </p>
          <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
            <Link href="/">
              <Button>Back to home</Button>
            </Link>
            <Link href="/trips">
              <Button variant="outline">Browse trips</Button>
            </Link>
          </div>
        </div>
      </main>
      <Footer />
    </>
  );
}
