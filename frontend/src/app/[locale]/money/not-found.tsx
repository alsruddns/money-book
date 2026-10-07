import NotFoundPageContent from "@/common/components/NotFoundPageContent";
import { privatePageMetadata } from "@/common/seo/siteMetadata";
import type { Metadata } from "next";

export const metadata: Metadata = privatePageMetadata();

export default function NotFound() {
  return <NotFoundPageContent />;
}
