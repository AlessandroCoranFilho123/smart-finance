using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Services;

/// <summary>
/// Empréstimos (D-10): o cadastro gera a movimentação real na carteira —
/// "Emprestei" é saída, "Pedi emprestado" é entrada — vinculada via transacao_id;
/// exclusão estorna e edição reajusta.
/// </summary>
public sealed class EmprestimoService(
    Database db,
    EmprestimoRepository repository,
    TransacaoRepository transacaoRepository)
{
    public void Salvar(Guid? id, TipoEmprestimo tipo, string nome, long valorCentavos, DateOnly dataPagamento, Guid? carteiraId)
    {
        if (string.IsNullOrWhiteSpace(nome))
        {
            throw new ArgumentException("Nome não pode estar vazio");
        }

        if (valorCentavos <= 0)
        {
            throw new ArgumentException("Valor deve ser positivo");
        }

        var emprestimo = new Emprestimo(id ?? Guid.NewGuid(), tipo, nome.Trim(), valorCentavos, dataPagamento, carteiraId);

        db.EmTransacao(() =>
        {
            EstornarMovimentacao(id);

            var hoje = DateOnly.FromDateTime(DateTime.Today);
            bool saida = tipo == TipoEmprestimo.Emprestei;
            var movimentacao = new Transacao(
                Guid.NewGuid(),
                saida ? $"Empréstimo concedido: {emprestimo.Nome}" : $"Empréstimo recebido: {emprestimo.Nome}",
                string.Empty,
                emprestimo.ValorCentavos,
                saida ? TipoTransacao.Saida : TipoTransacao.Entrada,
                hoje,
                null,
                Categoria.Emprestimo,
                emprestimo.CarteiraId);

            transacaoRepository.Inserir(movimentacao);

            if (id is null)
            {
                repository.Inserir(emprestimo with { TransacaoId = movimentacao.Id });
            }
            else
            {
                repository.Atualizar(emprestimo with { TransacaoId = movimentacao.Id });
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

    public List<Emprestimo> ListarTodas() => repository.ListarTodas();

    public List<Emprestimo> ListarPorTipo(TipoEmprestimo tipo) => repository.ListarPorTipo(tipo);

    public long TotalPorTipoCentavos(TipoEmprestimo tipo) => ListarPorTipo(tipo).Sum(e => e.ValorCentavos);

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
}
