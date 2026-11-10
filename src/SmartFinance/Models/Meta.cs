namespace SmartFinance.Models;

public sealed class Meta(Guid id, string nome, long? alvoCentavos, long atualCentavos)
{
    public Meta(string nome, long? alvoCentavos)
        : this(Guid.NewGuid(), nome, alvoCentavos, 0)
    {
    }

    public Guid Id { get; } = id == Guid.Empty ? Guid.NewGuid() : id;

    public string Nome { get; } = nome?.Trim() ?? string.Empty;

    public long? AlvoCentavos { get; private set; } = alvoCentavos;

    public long AtualCentavos { get; private set; } = atualCentavos;

    public bool SemAlvo => AlvoCentavos is null or <= 0;

    public long RestanteParaAlvo => SemAlvo ? long.MaxValue : Math.Max(0, AlvoCentavos!.Value - AtualCentavos);

    public void Adicionar(long centavos)
    {
        if (centavos <= 0)
        {
            throw new ArgumentException("Valor deve ser positivo");
        }

        AtualCentavos += centavos;
    }

    /// <summary>Retira da meta no máximo o valor disponível; retorna o valor efetivamente retirado.</summary>
    public long Retirar(long centavos)
    {
        if (centavos <= 0)
        {
            return 0;
        }

        long permitido = Math.Min(centavos, AtualCentavos);
        AtualCentavos -= permitido;
        return permitido;
    }

    public double Progresso => SemAlvo ? 0.0 : Math.Min((double)AtualCentavos / AlvoCentavos!.Value, 1.0);
}
