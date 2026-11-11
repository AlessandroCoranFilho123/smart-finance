using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Services;

public sealed class MetaService(MetaRepository metaRepository)
{
    public void CriarMeta(string nome, long alvoCentavos)
    {
        if (string.IsNullOrWhiteSpace(nome))
        {
            throw new ArgumentException("Nome não pode estar vazio");
        }

        if (alvoCentavos <= 0)
        {
            throw new ArgumentException("Valor alvo deve ser positivo");
        }

        metaRepository.Inserir(new Meta(nome.Trim(), alvoCentavos));
    }

    public void AtualizarMeta(Meta meta)
    {
        ArgumentNullException.ThrowIfNull(meta);
        if (meta.SemAlvo)
        {
            throw new ArgumentException("Meta deve ter valor alvo");
        }

        metaRepository.Atualizar(meta);
    }

    public void DeletarMeta(Guid id)
    {
        // D-01: meta com valor acumulado não pode ser excluída — o dinheiro aplicado
        // saiu da carteira; excluí-la o destruiria. O usuário resgata antes de excluir.
        Meta meta = metaRepository.BuscarPorId(id)
            ?? throw new ArgumentException("Meta não encontrada");

        if (meta.AtualCentavos > 0)
        {
            throw new ArgumentException(
                $"A meta possui {Formatacao.Moeda(meta.AtualCentavos)} acumulados. Resgate o valor antes de excluir a meta.");
        }

        metaRepository.Deletar(id);
    }

    public List<Meta> ListarTodasMetas() => metaRepository.ListarTodas();

    public Meta? BuscarPorId(Guid id) => metaRepository.BuscarPorId(id);

    public double CalcularProgressoPercentual(Meta meta) => meta.Progresso * 100.0;

    public bool MetaConcluida(Meta meta) => !meta.SemAlvo && meta.AtualCentavos >= meta.AlvoCentavos!;

    public long CalcularRestante(Meta meta) => meta.RestanteParaAlvo;

    public long ContarMetasConcluidas() => ListarTodasMetas().Count(MetaConcluida);
}
