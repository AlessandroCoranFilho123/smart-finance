namespace SmartFinance.Models;

public sealed record Transacao(
    Guid Id,
    string Descricao,
    string Comentario,
    long ValorCentavos,
    TipoTransacao Tipo,
    DateOnly Data,
    Guid? MetaId,
    Categoria? Categoria,
    Guid? CarteiraId)
{
    public Transacao(
        Guid id,
        string descricao,
        string comentario,
        long valorCentavos,
        TipoTransacao tipo,
        DateOnly data,
        Guid? metaId,
        Categoria? categoria)
        : this(id, descricao, comentario, valorCentavos, tipo, data, metaId, categoria, null)
    {
    }
}
