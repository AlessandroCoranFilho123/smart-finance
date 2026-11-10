namespace SmartFinance.Models;

public enum Categoria
{
    Salario,
    Emprestimo,
    Aluguel,
    Alimentacao,
    Internet,
    Agua,
    Luz,
    Compras,
    Educacao,
    Transporte,
    AdicionarMeta,
    RetirarMeta,
    Outros
}

public static class CategoriaExtensoes
{
    /// <summary>Rótulo amigável em pt-BR usado pela interface (o banco guarda o nome do enum).</summary>
    public static string NomeExibicao(this Categoria categoria) => categoria switch
    {
        Categoria.Salario => "Salário",
        Categoria.Alimentacao => "Alimentação",
        Categoria.Agua => "Água",
        Categoria.AdicionarMeta => "Aplicar em meta",
        Categoria.RetirarMeta => "Resgatar meta",
        _ => categoria.ToString()
    };
}
