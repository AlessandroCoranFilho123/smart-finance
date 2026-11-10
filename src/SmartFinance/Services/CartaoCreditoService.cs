using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Services;

public sealed class CartaoCreditoService(CartaoCreditoRepository repository)
{
    public void Salvar(Guid? id, string nome, long valorTotalCentavos, int parcelas, DateOnly vencimento)
    {
        var compra = Validar(id, nome, valorTotalCentavos, parcelas, vencimento);
        if (id is null)
        {
            repository.Inserir(compra);
        }
        else
        {
            repository.Atualizar(compra);
        }
    }

    public void Deletar(Guid id) => repository.Deletar(id);

    public List<CompraCartaoCredito> ListarTodas() => repository.ListarTodas();

    public long CalcularTotalAbertoCentavos() => ListarTodas().Sum(c => c.ValorTotalCentavos);

    public long CalcularTotalParcelasMesCentavos() => ListarTodas().Sum(c => c.ValorParcelaCentavos);

    public long ContarVencendoEmAte(int dias, DateOnly hoje) => ListarTodas()
        .Count(compra =>
        {
            long diasAteVencimento = compra.DiasAteVencimento(hoje);
            return diasAteVencimento >= 0 && diasAteVencimento <= dias;
        });

    private static CompraCartaoCredito Validar(Guid? id, string nome, long valorTotalCentavos, int parcelas, DateOnly vencimento)
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

        return new CompraCartaoCredito(id ?? Guid.NewGuid(), nome.Trim(), valorTotalCentavos, parcelas, vencimento);
    }
}
