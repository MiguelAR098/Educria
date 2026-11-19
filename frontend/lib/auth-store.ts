import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import { User, UserRole } from "./types";

interface AuthState {
  token: string | null;
  user: User | null;
  _hasHydrated: boolean;
  setAuth: (token: string, user: User) => void;
  logout: () => void;
  setHasHydrated: (state: boolean) => void;
}

function decodeJwtPayload(token: string): Partial<User> | null {
  try {
    const base64Url = token.split(".")[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    const parsed = JSON.parse(jsonPayload);
    return {
      id: parsed.id,
      email: parsed.sub || parsed.email,
      nome: parsed.nome || parsed.name,
      role: (parsed.perfil || parsed.role || parsed.authorities?.[0]?.replace("ROLE_", "")) as UserRole,
    };
  } catch {
    return null;
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      _hasHydrated: false,
      setAuth: (token, user) => {
        const decoded = decodeJwtPayload(token);
        const mergedUser: User = {
          nome: user.nome || decoded?.nome || "Estudante",
          email: user.email || decoded?.email || "",
          role: user.role || decoded?.role || "ALUNO",
          matricula: user.matricula,
          id: user.id || decoded?.id,
        };
        set({ token, user: mergedUser });
      },
      logout: () => {
        set({ token: null, user: null });
      },
      setHasHydrated: (state) => {
        set({ _hasHydrated: state });
      },
    }),
    {
      name: "educria-auth-storage",
      storage: createJSONStorage(() => localStorage),
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated(true);
      },
    }
  )
);
