import { Button } from "@/components/ui/button";

interface Props {
  consulta: {
    isPending: boolean;
    isError: boolean;
    isRefetchError: boolean;
    isFetching: boolean;
    refetch: () => unknown;
  };
  recurso: string;
}

export function EstadoConsulta({ consulta, recurso }: Props) {
  if (consulta.isPending) return <p role="status">Carregando {recurso}…</p>;
  if (consulta.isError) return (
    <div role="alert" className="flex flex-wrap items-center gap-3">
      Não foi possível {consulta.isRefetchError ? "atualizar" : "carregar"} {recurso}.
      <Button type="button" size="sm" variant="outline" disabled={consulta.isFetching} onClick={() => consulta.refetch()}>
        Tentar novamente
      </Button>
    </div>
  );
  return null;
}
