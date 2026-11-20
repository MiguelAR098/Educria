"use client";

import { Toaster as Sonner } from "sonner";

export function Toaster() {
  return (
    <Sonner
      position="top-right"
      richColors
      closeButton
      toastOptions={{
        style: {
          borderRadius: "14px",
          fontSize: "0.8125rem",
        },
      }}
    />
  );
}
