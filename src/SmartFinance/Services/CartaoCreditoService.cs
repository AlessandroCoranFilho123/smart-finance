using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Services;

/// <summary>
/// Compras parceladas do cartão (D-10/D-11): o cadastro gera uma saída do valor
/// total na carteira escolhida (movimentação vinculada via transacao_id), a
/// exclusão estorna e a edição reajusta. "Em aberto" rastreia a dívida restante
/// com o cartão a partir das parcelas pagas.
/// </summary>
public sealed class CartaoCreditoService(
    Database db,
    CartaoCreditoRepository repository,
    TransacaoRepository transacaoRepository)
{
    public void Salvar(Guid? id, string nome, long valorTotalCentavos, int parcelas, int parcelasPagas, DateOnly vencimento, Guid? carteiraId)
    {
        var compra = Validar(id, nome, valorTotalCentavos, parcelas, parcelasPagas, vencimento, carteiraId);

        db.EmTransacao(() =>
        {
            EstornarMovimentacao(id);

            var hoje = DateOnly.FromDateTime(DateTime.Today);
            var movimentacao = new Transacao(
                Guid.NewGuid(),
                $"Compra no cartão: {compra.Nome}",
                string.Empty,
                compra.ValorTotalCentavos,
                TipoTransacao.Saida,
                hoje,
                null,
                Categoria.Compras,
                compra.CarteiraId);

            if (id is null)
            {
                transacaoRepository.Inserir(movimentacao);
                repository.Inserir(compra with { TransacaoId = movimentacao.Id });
            }
            else
            {
                transacaoRepository.Inserir(movimentacao);
                repository.Atualizar(compra with { TransacaoId = movimentacao.Id });
            }
        });
    }

    public void Deletar(Guid id)
    {
        db.EmTransacao(() =>
        {
            EstornarMovimentacao(id);
            repository.Deletar(id);
        });
    }

    public CompraCartaoCredito? BuscarPorId(Guid id) => repository.BuscarPorId(id);

    public List<CompraCartaoCredito> ListarTodas() => repository.ListarTodas();

    /// <summary>Dívida restante com o cartão: parcelas restantes × valor da parcela (D-06/D-11).</summary>
    public long CalcularTotalAbertoCentavos() => ListarTodas().Sum(c => c.ParcelasRestantes * c.ValorParcelaCentavos);

    public long CalcularTotalParcelasMesCentavos() => ListarTodas()
        .Where(c => c.ParcelasRestantes > 0)
        .Sum(c => c.ValorParcelaCentavos);

    public long ContarVencendoEmAte(int dias, DateOnly hoje) => ListarTodas()
        .Count(compra =>
        {
            long diasAteVencimento = compra.DiasAteVencimento(hoje);
            return diasAteVencimento >= 0 && diasAteVencimento <= dias && compra.ParcelasRestantes > 0;
        });

    private void EstornarMovimentacao(Guid? id)
    {
        if (id is null)
        {
            return;
        }

        var atual = repository.BuscarPorId(id.Value);
        if (atual?.TransacaoId is not null)
        {
            transacaoRepository.Excluir(atual.TransacaoId.Value);
        }
    }

    private static CompraCartaoCredito Validar(Guid? id, string nome, long valorTotalCentavos, int parcelas, int parcelasPagas, DateOnly vencimento, Guid? carteiraId)
    {
        if (string.IsNullOrWhiteSpace(nome))
        {
            throw new ArgumentException("Nome não pode estar vazio");
        }

        if (valorTotalCentavos <= 0)
        {
            throw new ArgumentException("Valor deve ser positivo");
        }

        if (parcelas <= 0)
        {
            throw new ArgumentException("Parcelas devem ser maiores que zero");
        }

        if (parcelasPagas < 0 || parcelasPagas > parcelas)
        {
            throw new ArgumentException("Parcelas pagas deve estar entre 0 e o total de parcelas");
        }

        return new CompraCartaoCredito(id ?? Guid.NewGuid(), nome.Trim(), valorTotalCentavos, parcelas, vencimento, parcelasPagas, carteiraId);
    }
}
