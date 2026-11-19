import { Button } from "@/components/ui/button";

interface Props {
  consulta: { isPending: boolean; isError: boolean; refetch: () => unknown };
  recurso: string;
}

export function EstadoConsulta({ consulta, recurso }: Props) {
  if (consulta.isPending) return <p role="status">Carregando {recurso}…</p>;
  if (consulta.isError) return (
    <div role="alert" className="flex flex-wrap items-center gap-3">
      Não foi possível atualizar {recurso}.
      <Button size="sm" variant="outline" onClick={() => consulta.refetch()}>Tentar novamente</Button>
    </div>
  );
  return null;
}
