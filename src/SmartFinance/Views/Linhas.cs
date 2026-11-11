using System.Windows;
using System.Windows.Media;
using SmartFinance.Services;

namespace SmartFinance.Views;

/// <summary>Linhas de lista (modelos de exibição já formatados para as visões).</summary>
public sealed class LinhaTransacao
{
    public LinhaTransacao(Models.Transacao transacao, string nomeCarteira)
    {
        Transacao = transacao;
        Icone = Icones.DeCategoria(transacao.Categoria, transacao.Tipo)!;
        Titulo = transacao.Descricao;
        Subtitulo = Formatacao.Data(transacao.Data)
            + (string.IsNullOrWhiteSpace(transacao.Comentario) ? "" : " · " + transacao.Comentario)
            + (nomeCarteira.Length > 0 ? "  ·  " + nomeCarteira : "");
        Valor = Formatacao.MoedaComSinal(transacao.ValorCentavos, transacao.Tipo == Models.TipoTransacao.Entrada);
        CorValor = Icones.BrushDeRecurso(transacao.Tipo == Models.TipoTransacao.Entrada ? "BrushEntrada" : "BrushSaida");
    }

    public Models.Transacao Transacao { get; }
    public ImageSource Icone { get; }
    public string Titulo { get; }
    public string Subtitulo { get; }
    public string Valor { get; }
    public Brush CorValor { get; }
}

public sealed class LinhaMeta
{
    public LinhaMeta(Models.Meta meta)
    {
        Meta = meta;
        Nome = meta.Nome;
        TextoValores = meta.SemAlvo
            ? $"Acumulado: {Formatacao.Moeda(meta.AtualCentavos)}"
            : $"{Formatacao.Moeda(meta.AtualCentavos)} de {Formatacao.Moeda(meta.AlvoCentavos!.Value)}";
        Percentual = meta.SemAlvo ? "" : $"{Math.Round(meta.Progresso * 100)}%";
        Progresso = meta.Progresso;
        CorProgresso = CorDeProgresso(meta);
    }

    public Models.Meta Meta { get; }
    public string Nome { get; }
    public string TextoValores { get; }
    public string Percentual { get; }
    public double Progresso { get; }
    public Brush CorProgresso { get; }

    private static Brush CorDeProgresso(Models.Meta meta)
    {
        if (meta.SemAlvo)
        {
            return Icones.BrushDeRecurso("BrushAccent");
        }

        double p = meta.Progresso;
        string chave = p >= 0.75 ? "BrushEntrada" : p >= 0.50 ? "BrushAviso" : "BrushSaida";
        return Icones.BrushDeRecurso(chave);
    }
}

public sealed class LinhaCompra
{
    public LinhaCompra(Models.CompraCartaoCredito compra, DateOnly hoje)
    {
        Compra = compra;
        Nome = compra.Nome;
        Detalhe = $"{compra.Parcelas}x de {Formatacao.Moeda(compra.ValorParcelaCentavos)} · pagas {compra.ParcelasPagas}/{compra.Parcelas} · total {Formatacao.Moeda(compra.ValorTotalCentavos)} · vence em {Formatacao.Data(compra.Vencimento)}";

        var status = compra.CalcularStatusVencimento(hoje);
        CorAviso = Icones.BrushDeRecurso(status switch
        {
            Models.StatusVencimento.Vermelho => "BrushSaida",
            Models.StatusVencimento.Laranja => "BrushAviso",
            _ => "BrushEntrada"
        });
        Aviso = compra.AvisoVencimento(hoje);
        AvisoVisivel = string.IsNullOrEmpty(Aviso) ? Visibility.Collapsed : Visibility.Visible;
        ValorParcela = Formatacao.Moeda(compra.ValorParcelaCentavos);
    }

    public Models.CompraCartaoCredito Compra { get; }
    public string Nome { get; }
    public string Detalhe { get; }
    public string ValorParcela { get; }
    public string Aviso { get; }
    public Visibility AvisoVisivel { get; }
    public Brush CorAviso { get; }
}

public sealed class LinhaEmprestimo
{
    public LinhaEmprestimo(Models.Emprestimo emprestimo)
    {
        Emprestimo = emprestimo;
        Nome = emprestimo.Nome;
        DataTexto = "Pagamento em " + Formatacao.Data(emprestimo.DataPagamento);
        Valor = Formatacao.Moeda(emprestimo.ValorCentavos);
    }

    public Models.Emprestimo Emprestimo { get; }
    public string Nome { get; }
    public string DataTexto { get; }
    public string Valor { get; }
}
