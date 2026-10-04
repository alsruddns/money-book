"use client";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useCurrentUser } from "@/auth/hooks/useCurrentUser";
import { useGetAdminMeQuery } from "../controller/adminApi";

export function useAdminSession() {
  const router = useRouter();
  const auth = useCurrentUser();
  const admin = useGetAdminMeQuery(undefined, { skip: auth.isLoading || !auth.isAuthenticated || auth.currentUser?.passwordChangeRequired === true, refetchOnMountOrArgChange: true });
  const role = admin.data?.systemRole;
  const isAdmin = role === "SYSTEM_ADMIN" || role === "SUPER_ADMIN";
  useEffect(() => {
    if (!auth.isLoading && auth.isAuthenticated && auth.currentUser?.passwordChangeRequired) router.replace("/change-required-password");
    else if (!auth.isLoading && !auth.isAuthenticated) router.replace("/login");
    else if (auth.isAuthenticated && !admin.isLoading && !isAdmin) router.replace("/books");
  }, [auth.isLoading, auth.isAuthenticated, auth.currentUser?.passwordChangeRequired, admin.isLoading, isAdmin, router]);
  return { ...admin, user: admin.data, isSuperAdmin: role === "SUPER_ADMIN", isAdmin, isLoading: auth.isLoading || (auth.isAuthenticated && admin.isLoading), isAuthenticated: auth.isAuthenticated };
}
