import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { tokenStorage } from "../storage/tokenStorage";

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
}

const initialState = (): AuthState => {
  const tokens = tokenStorage.getTokens();
  return {
    accessToken: tokens?.accessToken ?? null,
    refreshToken: tokens?.refreshToken ?? null,
  };
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setTokens: (state, action: PayloadAction<AuthTokens>) => {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
    },
    clearAuth: (state) => {
      state.accessToken = null;
      state.refreshToken = null;
    },
  },
});

export const { setTokens, clearAuth } = authSlice.actions;
export default authSlice.reducer;
