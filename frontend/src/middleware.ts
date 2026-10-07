import { NextRequest, NextResponse } from "next/server";
import { defaultLocale, isLocale, supportedLocales } from "@/i18n/config";

export function middleware(request: NextRequest) {
  const pathname = request.nextUrl.pathname;
  const firstSegment = pathname.split("/")[1];
  if (isLocale(firstSegment)) {
    const requestHeaders = new Headers(request.headers);
    requestHeaders.set("x-locale", firstSegment);
    const response = NextResponse.next({ request: { headers: requestHeaders } });
    response.cookies.set("moneybook-locale", firstSegment, { path: "/", sameSite: "lax", maxAge: 60 * 60 * 24 * 365 });
    return response;
  }
  if (firstSegment && /^[a-z]{2}(?:-[A-Z]{2})?$/.test(firstSegment) && !supportedLocales.includes(firstSegment as never)) {
    return NextResponse.rewrite(new URL("/404", request.url));
  }
  const locale = pathname === "/" ? defaultLocale : (request.cookies.get("moneybook-locale")?.value && isLocale(request.cookies.get("moneybook-locale")?.value) ? request.cookies.get("moneybook-locale")!.value : defaultLocale);
  const destination = request.nextUrl.clone();
  destination.pathname = `/${locale}${pathname === "/" ? "" : pathname}`;
  return NextResponse.redirect(destination, 308);
}

export const config = { matcher: ["/((?!api/|_next/|favicon.ico|robots.txt|sitemap.xml|.*\\..*).*)"] };
