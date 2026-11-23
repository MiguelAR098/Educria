"use client";

import Link from "next/link";
import { ArrowLeft, ShieldCheck } from "lucide-react";
import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const HOJE = [
  ["Dados coletados", "Nome, e-mail, matrícula, turma, respostas às questões, nível por assunto e datas e horários da atividade."],
  [
    "Quem vê o quê",
    "O aluno vê só os próprios dados. O professor vê, dos alunos das turmas em que leciona, o nível, o desempenho nos assuntos da sua matéria, a evolução e os recados enviados; não vê alunos de outras turmas. A pontuação coletiva aparece para a turma.",
  ],
  ["Senhas", "São guardadas com hash (BCrypt), nunca em texto puro."],
  [
    "O que não existe",
    "Ranking entre alunos visível a estudantes, desempenho individual em tela pública e divulgação do nome de um aluno fora da equipe pedagógica.",
  ],
  [
    "Questões",
    "Não são apagadas: o professor desativa a questão. Isso preserva o histórico de respostas dos alunos.",
  ],
];

const PENDENCIAS = [
  "Termo de consentimento do responsável legal, porque os alunos são menores de idade.",
  "Registro de acesso: hoje o sistema não guarda quem consultou os dados de qual aluno e quando.",
  "Canal para o responsável pedir correção ou exclusão dos dados do aluno.",
  "Política de retenção e anonimização. Sugestão para discutir: manter as respostas durante o ano letivo e mais um ano, e depois anonimizar para uso estatístico.",
];

export default function PrivacidadePage() {
  return (
    <AuthGuard allowedRole="PROFESSOR">
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />

        <main className="flex-1 mx-auto w-full max-w-3xl px-4 sm:px-6 py-8 space-y-6">
          <Link href="/professor" className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700">
            <ArrowLeft className="h-4 w-4" /> Minhas turmas
          </Link>

          <div className="space-y-1">
            <Badge variant="secondary" className="gap-1">
              <ShieldCheck className="h-3.5 w-3.5" />
              Dados e privacidade
            </Badge>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
              Tratamento de dados pessoais
            </h1>
            <p className="text-sm text-slate-500">
              Os alunos são menores de idade, então este texto não é enfeite. Ele descreve o que o sistema faz hoje e
              o que ainda precisa ser combinado com a escola antes de qualquer uso real. É um texto de apoio do projeto
              e não substitui orientação jurídica nem a política da escola.
            </p>
          </div>

          <Card className="shadow-md border-slate-200/80">
            <CardHeader>
              <CardTitle className="text-base">O que o sistema faz hoje</CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="space-y-4 text-sm">
                {HOJE.map(([titulo, texto]) => (
                  <div key={titulo}>
                    <dt className="font-semibold text-slate-900">{titulo}</dt>
                    <dd className="text-slate-700">{texto}</dd>
                  </div>
                ))}
              </dl>
            </CardContent>
          </Card>

          <Card className="shadow-md border-amber-200 bg-amber-50/40">
            <CardHeader>
              <CardTitle className="text-base">Pendências antes de usar com alunos reais</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="list-disc space-y-2 pl-5 text-sm text-slate-800">
                {PENDENCIAS.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </CardContent>
          </Card>
        </main>
      </div>
    </AuthGuard>
  );
}
