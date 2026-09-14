/** @type {import('next').NextConfig} */
const isProd = process.env.NODE_ENV === "production";

const connectSrc = isProd
  ? "'self' https://api.joyway.in https://api.razorpay.com https://lumberjack.razorpay.com"
  : "'self' http://localhost:8080 https://api.joyway.in https://api.razorpay.com https://lumberjack.razorpay.com";

const scriptSrc = isProd
  ? "'self' 'unsafe-inline' https://checkout.razorpay.com"
  : "'self' 'unsafe-inline' 'unsafe-eval' https://checkout.razorpay.com";

const nextConfig = {
  output: "standalone",
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
    // Cap generated widths below 3840 — full-bleed hero rarely needs larger delivery.
    deviceSizes: [640, 750, 828, 1080, 1200, 1920, 2048],
    imageSizes: [16, 32, 48, 64, 96, 128, 256, 384],
    remotePatterns: [
      {
        protocol: "https",
        hostname: "api.joyway.in",
      },
      {
        protocol: "https",
        hostname: "*.azurefd.net",
      },
      {
        protocol: "https",
        hostname: "joyway-images-prod-gye6a8dff4dgfub3.z03.azurefd.net",
      },
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
              "img-src 'self' data: blob: https://api.joyway.in https://*.azurefd.net https://*.blob.core.windows.net",
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
