import type { NextConfig } from "next";
import path from "path";

const springApiBaseUrl = process.env.SPRING_API_BASE_URL || "http://localhost:8080";

const nextConfig: NextConfig = {
  // Turbopack detects the root package.json at the monorepo/project level and incorrectly
  // sets the workspace root to the entire FSRSpring directory, which includes target/ (Java
  // build output). Every Maven recompile triggers thousands of file-change events, causing
  // Turbopack to hot-reload in a continuous loop and leak memory. Fix: pin the root to
  // this frontend directory so Turbopack only watches frontend source files.
  turbopack: {
    root: path.resolve(__dirname),
  },
  modularizeImports: {
    "@tabler/icons-react": {
      transform: "@tabler/icons-react/dist/esm/icons/{{member}}.mjs",
      preventFullImport: true
    }
  },
  experimental: {
    // The user UI imports many Tabler icons. In dev, Turbopack can otherwise
    // persist huge source-map/cache snapshots under .next/dev/cache.
    optimizePackageImports: ["@tabler/icons-react"],
    turbopackFileSystemCacheForDev: false,
    turbopackSourceMaps: false,
    turbopackInputSourceMaps: false,
    turbopackMemoryLimit: 512 * 1024 * 1024
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
