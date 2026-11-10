using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using SmartFinance.Models;

namespace SmartFinance.Views;

/// <summary>Resolve os ícones de categoria (assets reutilizados da versão Java).</summary>
public static class Icones
{
    private const string Base = "pack://application:,,,/Assets/icons/";
    private static readonly Dictionary<string, ImageSource> Cache = new();

    public static ImageSource? DeCategoria(Categoria? categoria, TipoTransacao tipo)
    {
        string arquivo = categoria switch
        {
            Categoria.Salario => "salario",
            Categoria.Emprestimo => "emprestimo",
            Categoria.Aluguel => "aluguel",
            Categoria.Alimentacao => "alimentacao",
            Categoria.Internet => "internet",
            Categoria.Agua => "agua",
            Categoria.Luz => "luz",
            Categoria.Compras => "compras",
            Categoria.Educacao => "educacao",
            Categoria.Transporte => "transporte",
            Categoria.AdicionarMeta or Categoria.RetirarMeta => "meta",
            _ => tipo == TipoTransacao.Entrada ? "entrada" : "saida"
        };

        return Carregar($"categoria/{arquivo}.png");
    }

    public static ImageSource Carregar(string caminhoRelativo)
    {
        if (Cache.TryGetValue(caminhoRelativo, out var emCache))
        {
            return emCache;
        }

        var imagem = new BitmapImage(new Uri(Base + caminhoRelativo))
        {
            CacheOption = BitmapCacheOption.OnLoad
        };
        imagem.Freeze();
        Cache[caminhoRelativo] = imagem;
        return imagem;
    }

    public static Brush BrushDeRecurso(string chave) =>
        (Brush)Application.Current.TryFindResource(chave)!;
}
