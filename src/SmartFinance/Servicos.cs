using SmartFinance.Data;
using SmartFinance.Services;

namespace SmartFinance;

/// <summary>Agrega os serviços construídos na inicialização do aplicativo.</summary>
public sealed class Servicos(
    Database db,
    TransacaoService transacoes,
    CarteiraService carteiras,
    MetaService metas,
    CartaoCreditoService cartao,
    EmprestimoService emprestimos)
{
    public Database Db { get; } = db;
    public TransacaoService Transacoes { get; } = transacoes;
    public CarteiraService Carteiras { get; } = carteiras;
    public MetaService Metas { get; } = metas;
    public CartaoCreditoService Cartao { get; } = cartao;
    public EmprestimoService Emprestimos { get; } = emprestimos;
}
