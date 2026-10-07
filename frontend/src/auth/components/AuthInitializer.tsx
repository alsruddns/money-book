"use client";

import type { ReactNode } from "react";
import { useAuthInitialize } from "../hooks/useAuthInitialize";

export default function AuthInitializer({ children }: { children: ReactNode }) {
  useAuthInitialize();
  return children;
}
