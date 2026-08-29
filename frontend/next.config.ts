/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "images.unsplash.com",
      },
      {
        protocol: "https",
        hostname: "joyway-backend-prod.livelyhill-dfc425d4.centralindia.azurecontainerapps.io",
      },
      {
        protocol: "https",
        hostname: "api.joyway.in",
      },
    ],
  },
};

export default nextConfig;
