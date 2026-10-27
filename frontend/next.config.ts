/** @type {import('next').NextConfig} */
const isProd = process.env.NODE_ENV === "production";

// Public base URL for Cloudflare R2 (or other S3-compatible) storage.
// Set NEXT_PUBLIC_STORAGE_BASE_URL in Vercel env vars, e.g. https://pub-<hash>.r2.dev
const storageBaseUrl = process.env.NEXT_PUBLIC_STORAGE_BASE_URL ?? "";

const connectSrc = isProd
  ? "'self' https://api.joyway.in https://api.razorpay.com https://lumberjack.razorpay.com"
  : "'self' http://localhost:8080 https://api.joyway.in https://api.razorpay.com https://lumberjack.razorpay.com";

const scriptSrc = isProd
  ? "'self' 'unsafe-inline' https://checkout.razorpay.com"
  : "'self' 'unsafe-inline' 'unsafe-eval' https://checkout.razorpay.com";

// img-src: always allow api.joyway.in; conditionally allow the R2 public URL domain.
const r2ImgSrc = storageBaseUrl
  ? ` ${new URL(storageBaseUrl).origin}`
  : "";

const nextConfig = {
  // Vercel deploys Next.js natively — no standalone output needed.
  // Keep `output: "standalone"` only if you also run Docker (e.g. local docker-compose).
  // output: "standalone",
  async redirects() {
    return [
      {
        source: "/:path*",
        has: [{ type: "host", value: "www.joyway.in" }],
        destination: "https://joyway.in/:path*",
        permanent: true,
      },
    ];
  },
  images: {
    formats: ["image/avif", "image/webp"],
    // Cap generated widths — full-bleed hero sources are ~1280px.
    deviceSizes: [640, 750, 828, 1080, 1200, 1280],
    imageSizes: [16, 32, 48, 64, 96, 128, 256, 384],
    remotePatterns: [
      {
        protocol: "https",
        hostname: "api.joyway.in",
      },
      // Cloudflare R2 public bucket URL (set NEXT_PUBLIC_STORAGE_BASE_URL in Vercel).
      ...(storageBaseUrl
        ? [
            {
              protocol: "https" as const,
              hostname: new URL(storageBaseUrl).hostname,
            },
          ]
        : []),
    ],
  },
  async headers() {
    return [
      {
        source: "/images/:path*",
        headers: [
          {
            key: "Cache-Control",
            value: "public, max-age=31536000, immutable",
          },
        ],
      },
      {
        source: "/:path*",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
          {
            key: "Content-Security-Policy",
            value: [
              "default-src 'self'",
              `script-src ${scriptSrc}`,
              "style-src 'self' 'unsafe-inline'",
              `img-src 'self' data: blob: https://api.joyway.in${r2ImgSrc}`,
              `connect-src ${connectSrc}`,
              "frame-src https://api.razorpay.com https://checkout.razorpay.com",
              "font-src 'self'",
              "object-src 'none'",
              "base-uri 'self'",
              "form-action 'self'",
              "frame-ancestors 'none'",
            ].join("; "),
          },
        ],
      },
    ];
  },
};

export default nextConfig;
