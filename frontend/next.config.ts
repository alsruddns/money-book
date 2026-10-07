import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  devIndicators: false,

  async rewrites() {
    const configuredUrl = process.env.BACKEND_API_URL;
    const backendApiUrl =
      configuredUrl ??
      (process.env.NODE_ENV === "production"
        ? undefined
        : "http://localhost:8080");

    if (!backendApiUrl) {
      throw new Error(
        "BACKEND_API_URL is required when running Next.js in production.",
      );
    }

    const parsedUrl = new URL(backendApiUrl);

    if (
      !["http:", "https:"].includes(parsedUrl.protocol) ||
      parsedUrl.pathname !== "/" ||
      parsedUrl.search ||
      parsedUrl.hash
    ) {
      throw new Error(
        "BACKEND_API_URL must be an HTTP(S) origin without a path, query, or fragment.",
      );
    }

    return [
      {
        source: "/api/:path*",
        destination: `${parsedUrl.origin}/api/:path*`,
        basePath: false,
      },
    ];
  },
};

export default nextConfig;
