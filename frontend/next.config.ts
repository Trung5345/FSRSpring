import type { NextConfig } from "next";

const springApiBaseUrl = process.env.SPRING_API_BASE_URL || "http://localhost:8080";

const nextConfig: NextConfig = {
  turbopack: {
    root: __dirname
  },
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${springApiBaseUrl}/api/:path*`
      },
      {
        source: "/oauth2/:path*",
        destination: `${springApiBaseUrl}/oauth2/:path*`
      },
      {
        source: "/login/:path*",
        destination: `${springApiBaseUrl}/login/:path*`
      },
      {
        source: "/logout",
        destination: `${springApiBaseUrl}/logout`
      },
      {
        source: "/generated-images/:path*",
        destination: `${springApiBaseUrl}/generated-images/:path*`
      }
    ];
  }
};

export default nextConfig;
