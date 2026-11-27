import axios, { CanceledError, type InternalAxiosRequestConfig } from "axios";
import { useAuthStore } from "./auth-store";

export const api = axios.create({
  baseURL: "/",
  headers: {
    "Content-Type": "application/json",
    "X-Educria-Client": "web",
  },
  timeout: 10000,
});

const sessoesPorRequisicao = new WeakMap<InternalAxiosRequestConfig, number>();

api.interceptors.request.use(
  (config) => {
    const { sessao, token } = useAuthStore.getState();
    sessoesPorRequisicao.set(config, sessao);
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: unknown) => { throw error; },
  { synchronous: true },
);

api.interceptors.response.use(
  (response) => {
    if (sessoesPorRequisicao.get(response.config) !== useAuthStore.getState().sessao) {
      throw new CanceledError();
    }
    return response;
  },
  (error: unknown) => {
    if (!axios.isAxiosError(error)) throw error;

    const { sessao, token, logout } = useAuthStore.getState();
    const config = error.config;
    if (config && sessoesPorRequisicao.get(config) !== sessao) throw new CanceledError();

    if (error.response?.status === 401 && token && config?.headers.get("Authorization") === `Bearer ${token}`) {
      logout();
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }
    throw error;
  }
);
