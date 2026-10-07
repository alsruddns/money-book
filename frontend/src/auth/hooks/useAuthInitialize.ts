"use client";

import { useEffect } from "react";
import { useDispatch } from "react-redux";
import type { AppDispatch } from "@/store/store";
import { initializeAuth } from "../store/authSlice";
import { tokenStorage } from "../storage/tokenStorage";

export function useAuthInitialize() {
  const dispatch = useDispatch<AppDispatch>();

  useEffect(() => {
    dispatch(initializeAuth(tokenStorage.getTokens()));
  }, [dispatch]);
}
