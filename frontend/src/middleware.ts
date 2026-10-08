import { NextRequest, NextResponse } from "next/server";
import { isLocale } from "@/i18n/config";

export function middleware(request: NextRequest) {
  const [locale, section] = request.nextUrl.pathname.split("/").slice(1, 3);
  if (locale && /^[a-z]{2}(?:-[A-Z]{2})?$/.test(locale) && !isLocale(locale)) {
    return new NextResponse(null, { status: 404 });
  }
  if (!isLocale(locale) || section !== "money") return NextResponse.next();
  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-locale", locale);
  return NextResponse.next({ request: { headers: requestHeaders } });
}

export const config = { matcher: ["/((?!api/|_next/|favicon.ico|robots.txt|sitemap.xml|.*\\..*).*)"] };
