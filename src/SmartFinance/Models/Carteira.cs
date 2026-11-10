namespace SmartFinance.Models;

public sealed record Carteira(Guid Id, string Nome, long SaldoInicialCentavos);

public sealed record CarteiraResumo(Carteira Carteira, long SaldoCentavos);

public sealed record TransferenciaCarteira(
    Guid Id,
    Guid OrigemCarteiraId,
    Guid DestinoCarteiraId,
    long ValorCentavos,
    DateOnly Data,
    string Comentario);
