import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  isInitialized: boolean;
}

const initialState: AuthState = {
  accessToken: null,
  refreshToken: null,
  isInitialized: false,
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    initializeAuth: (state, action: PayloadAction<AuthTokens | null>) => {
      state.accessToken = action.payload?.accessToken ?? null;
      state.refreshToken = action.payload?.refreshToken ?? null;
      state.isInitialized = true;
    },
    setTokens: (state, action: PayloadAction<AuthTokens>) => {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.isInitialized = true;
    },
    clearAuth: (state) => {
      state.accessToken = null;
      state.refreshToken = null;
      state.isInitialized = true;
    },
  },
});

export const { initializeAuth, setTokens, clearAuth } = authSlice.actions;
export default authSlice.reducer;
