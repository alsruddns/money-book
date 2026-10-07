export type GlobalSystemRole = "USER" | "SYSTEM_ADMIN" | "SUPER_ADMIN" | string;

export function getGlobalNavItems(role: GlobalSystemRole | null | undefined) {
  const base = [
    { href: "/books", label: "내 가계부" },
    { href: "/books/invitations", label: "받은 초대" },
    { href: "/board", label: "게시판" },
    { href: "/account", label: "계정 관리" },
  ];
  return role === "SYSTEM_ADMIN" || role === "SUPER_ADMIN"
    ? [...base.slice(0, 3), { href: "/admin", label: "관리자" }, base[3]]
    : base;
}

export function isGlobalHeaderHidden(pathname: string): boolean {
  return pathname === "/" || pathname === "/login" || pathname === "/signup";
}

export function isGlobalNavItemActive(pathname: string, href: string): boolean {
  if (href === "/books") return pathname === href || (pathname.startsWith("/books/") && pathname !== "/books/invitations");
  return pathname === href || pathname.startsWith(`${href}/`);
}
