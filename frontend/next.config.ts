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
  images: {
    formats: ["image/avif", "image/webp"],
    remotePatterns: [
      {
        protocol: "https",
        hostname: "api.joyway.in",
      },
    ],
  },
  async headers() {
    return [
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
              "img-src 'self' data: blob: https://api.joyway.in https://*.blob.core.windows.net",
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
