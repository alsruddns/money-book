import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
    if (!apiBaseUrl) return [];

    return [{
      source: "/api/:path*",
      destination: `${apiBaseUrl.replace(/\/$/, "")}/:path*`,
    }];
  },
};

export default nextConfig;
