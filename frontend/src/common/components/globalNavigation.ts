export type GlobalSystemRole = "USER" | "SYSTEM_ADMIN" | "SUPER_ADMIN" | string;

export function getGlobalNavItems(role: GlobalSystemRole | null | undefined) {
  const base = [
    { href: "/books", label: "내 가계부" },
    { href: "/books/invitations", label: "받은 초대" },
    { href: "/account", label: "계정 관리" },
  ];
  return role === "SYSTEM_ADMIN" || role === "SUPER_ADMIN"
    ? [...base.slice(0, 2), { href: "/admin", label: "관리자" }, base[2]]
    : base;
}

export function isGlobalHeaderHidden(pathname: string): boolean {
  return pathname === "/" || pathname === "/login" || pathname === "/signup";
}
