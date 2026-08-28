import type { Metadata } from "next";
import { DM_Sans, Playfair_Display } from "next/font/google";
import "./globals.css";

const dmSans = DM_Sans({
  variable: "--font-dm-sans",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

const playfair = Playfair_Display({
  variable: "--font-playfair",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

export const metadata: Metadata = {
  title: "JOYWAY | Find Joy in Every Journey",
  description:
    "Discover beautiful destinations, join unforgettable group trips from Delhi, and let us take care of the journey.",
  metadataBase: new URL("https://joyway.in"),
  openGraph: {
    title: "JOYWAY | Find Joy in Every Journey",
    description:
      "Discover beautiful destinations, join unforgettable group trips from Delhi.",
    url: "https://joyway.in",
    siteName: "JOYWAY",
    locale: "en_IN",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "JOYWAY | Find Joy in Every Journey",
    description:
      "Discover beautiful destinations, join unforgettable group trips from Delhi.",
  },
  alternates: {
    canonical: "https://joyway.in",
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html
      lang="en"
      className={`${dmSans.variable} ${playfair.variable} h-full scroll-smooth antialiased`}
    >
      <body className="flex min-h-full flex-col bg-off-white text-charcoal">
        {children}
      </body>
    </html>
  );
}
