namespace SmartFinance.Models;

public sealed record CompraCartaoCredito(
    Guid Id,
    string Nome,
    long ValorTotalCentavos,
    int Parcelas,
    DateOnly Vencimento)
{
    public long ValorParcelaCentavos => Parcelas <= 0 ? 0 : (long)Math.Round(ValorTotalCentavos / (double)Parcelas, MidpointRounding.AwayFromZero);

    public long DiasAteVencimento(DateOnly hoje) => Vencimento.DayNumber - hoje.DayNumber;

    public StatusVencimento CalcularStatusVencimento(DateOnly hoje)
    {
        long dias = DiasAteVencimento(hoje);
        if (dias <= 0)
        {
            return StatusVencimento.Vermelho;
        }

        return dias <= 7 ? StatusVencimento.Laranja : StatusVencimento.Verde;
    }

    public string AvisoVencimento(DateOnly hoje)
    {
        long dias = DiasAteVencimento(hoje);
        if (dias < 0)
        {
            return $"Vencido há {Math.Abs(dias)} dia(s)";
        }

        if (dias == 0)
        {
            return "Vence hoje";
        }

        return dias <= 7 ? $"Vence em {dias} dia(s)" : "";
    }
}

public sealed record Emprestimo(
    Guid Id,
    TipoEmprestimo Tipo,
    string Nome,
    long ValorCentavos,
    DateOnly DataPagamento);
