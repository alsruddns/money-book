import AdminShell from "@/admin/components/AdminShell";
import { privatePageMetadata } from "@/common/seo/siteMetadata";
import type { Metadata } from "next";

export const metadata: Metadata = privatePageMetadata();

export default function AdminLayout({ children }: { children: React.ReactNode }) { return <AdminShell>{children}</AdminShell>; }
