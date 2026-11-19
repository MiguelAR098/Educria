import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";
import { Providers } from "@/components/providers";

const jakarta = localFont({
  src: "./fonts/plus-jakarta-sans-latin-wght-normal.woff2",
  weight: "200 800",
  display: "swap",
});

export const metadata: Metadata = {
  title: "EDUCRIA | Aprendizagem Adaptativa e Gamificada",
  description:
    "Plataforma educacional gamificada e adaptativa para estudantes do Ensino Fundamental II.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-BR">
      <body className={`${jakarta.className} antialiased min-h-screen bg-slate-50`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
