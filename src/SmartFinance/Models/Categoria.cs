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

    /// <summary>Lista de categorias oferecida pela interface para cada tipo (mesma regra dos diálogos).</summary>
    public static Categoria[] CategoriasValidas(TipoTransacao tipo) => tipo switch
    {
        TipoTransacao.Entrada =>
            [Categoria.Salario, Categoria.Emprestimo, Categoria.RetirarMeta, Categoria.Outros],
        TipoTransacao.Saida =>
        [
            Categoria.Emprestimo, Categoria.Alimentacao, Categoria.Aluguel, Categoria.Transporte,
            Categoria.Internet, Categoria.Agua, Categoria.Luz, Categoria.Compras,
            Categoria.Educacao, Categoria.AdicionarMeta, Categoria.Outros
        ],
        _ => []
    };

    /// <summary>Defesa no serviço contra tipo × categoria incoerentes (SF-029/SF-003).</summary>
    public static bool EhValidaPara(this Categoria categoria, TipoTransacao tipo) =>
        CategoriasValidas(tipo).Contains(categoria);
}
