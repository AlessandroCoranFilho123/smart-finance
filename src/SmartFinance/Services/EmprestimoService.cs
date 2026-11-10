using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Services;

public sealed class EmprestimoService(EmprestimoRepository repository)
{
    public void Salvar(Guid? id, TipoEmprestimo tipo, string nome, long valorCentavos, DateOnly dataPagamento)
    {
        if (string.IsNullOrWhiteSpace(nome))
        {
            throw new ArgumentException("Nome não pode estar vazio");
        }

        if (valorCentavos <= 0)
        {
            throw new ArgumentException("Valor deve ser positivo");
        }

        var emprestimo = new Emprestimo(id ?? Guid.NewGuid(), tipo, nome.Trim(), valorCentavos, dataPagamento);
        if (id is null)
        {
            repository.Inserir(emprestimo);
        }
        else
        {
            repository.Atualizar(emprestimo);
        }
    }

    public void Deletar(Guid id) => repository.Deletar(id);

    public List<Emprestimo> ListarTodas() => repository.ListarTodas();

    public List<Emprestimo> ListarPorTipo(TipoEmprestimo tipo) => repository.ListarPorTipo(tipo);

    public long TotalPorTipoCentavos(TipoEmprestimo tipo) => ListarPorTipo(tipo).Sum(e => e.ValorCentavos);
}
