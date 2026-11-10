using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Data;
using SmartFinance.Models;
using SmartFinance.Services;

namespace SmartFinance.Views.Dialogs;

public partial class TransacaoDialog : Window
{
    private static readonly Regex PadraoValor = new("^[0-9.,]*$", RegexOptions.Compiled);

    private static readonly Categoria[] CategoriasEntrada =
        { Categoria.Salario, Categoria.Emprestimo, Categoria.RetirarMeta, Categoria.Outros };

    private static readonly Categoria[] CategoriasSaida =
        { Categoria.Emprestimo, Categoria.Alimentacao, Categoria.Aluguel, Categoria.Transporte,
          Categoria.Internet, Categoria.Agua, Categoria.Luz, Categoria.Compras,
          Categoria.Educacao, Categoria.AdicionarMeta, Categoria.Outros };

    private readonly Servicos _servicos = App.Servicos!;
    private Transacao? _emEdicao;
    private bool _carregando;

    public TransacaoDialog()
    {
        InitializeComponent();
        JanelaNativa.AoFonteInicializada(this);
    }

    /// <summary>transacao nula → modo criação; não nula → modo edição/exclusão.</summary>
    public void Configurar(Transacao? transacao)
    {
        _carregando = true;
        _emEdicao = transacao;

        ComboCarteira.ItemsSource = _servicos.Carteiras.ListarCarteiras();

        if (transacao is null)
        {
            CampoData.SelectedDate = DateTime.Today;
            BotaoSalvar.Content = "Salvar";
            BotaoExcluir.Visibility = Visibility.Collapsed;
            BotaoEntrada.IsChecked = true;
        }
        else
        {
            TxtTitulo.Text = "Detalhes da transação";
            Title = "Detalhes da transação";
            CampoData.SelectedDate = transacao.Data.ToDateTime(TimeOnly.MinValue);
            CampoValor.Text = (transacao.ValorCentavos / 100m).ToString("N2", Formatacao.CulturaBr);
            CampoComentario.Text = transacao.Comentario;
            ComboCarteira.SelectedItem = ((List<Carteira>)ComboCarteira.ItemsSource)
                .FirstOrDefault(c => c.Id == (transacao.CarteiraId ?? Database.CarteiraBancoId));
            BotaoSalvar.Content = "Atualizar";
            BotaoExcluir.Visibility = Visibility.Visible;
            BotaoEntrada.IsChecked = transacao.Tipo == TipoTransacao.Entrada;
            BotaoSaida.IsChecked = transacao.Tipo == TipoTransacao.Saida;
        }

        AtualizarEstadoTipo(transacao);
        _carregando = false;
        CampoValor.Focus();
        CampoValor.CaretIndex = CampoValor.Text.Length;
    }

    private TipoTransacao TipoAtual => BotaoEntrada.IsChecked == true ? TipoTransacao.Entrada : TipoTransacao.Saida;

    private Categoria? CategoriaAtual => ((ItemCombo?)ComboCategoria.SelectedItem)?.Valor;

    private void AoMudarTipo(object sender, RoutedEventArgs e)
    {
        // ToggleButtons precisam ficar mutuamente exclusivos manualmente (não usam ToggleGroup).
        if (sender == BotaoEntrada && BotaoEntrada.IsChecked == true)
        {
            BotaoSaida.IsChecked = false;
        }
        else if (sender == BotaoSaida && BotaoSaida.IsChecked == true)
        {
            BotaoEntrada.IsChecked = false;
        }
        else if (BotaoEntrada.IsChecked != true && BotaoSaida.IsChecked != true)
        {
            (sender == BotaoEntrada ? BotaoEntrada : BotaoSaida).IsChecked = true;
        }

        if (!_carregando)
        {
            AtualizarEstadoTipo(null);
        }
    }

    private void AtualizarEstadoTipo(Transacao? transacao)
    {
        var categorias = TipoAtual == TipoTransacao.Entrada ? CategoriasEntrada : CategoriasSaida;
        ComboCategoria.ItemsSource = categorias
            .Select(c => new ItemCombo(c.NomeExibicao(), c))
            .ToList();

        Categoria? selecionada = transacao?.Categoria;
        if (selecionada is not null && categorias.Contains(selecionada.Value))
        {
            ComboCategoria.SelectedItem = ((List<ItemCombo>)ComboCategoria.ItemsSource)
                .First(i => i.Valor == selecionada.Value);
        }
        else
        {
            ComboCategoria.SelectedItem = null;
            AtualizarVisibilidadeMeta(null);
        }
    }

    private void AoMudarCategoria(object sender, SelectionChangedEventArgs e)
    {
        AtualizarVisibilidadeMeta(CategoriaAtual);
        AtualizarInfoMeta();
    }

    private void AtualizarVisibilidadeMeta(Categoria? categoria)
    {
        bool precisaMeta = categoria is Categoria.AdicionarMeta or Categoria.RetirarMeta;
        BoxMeta.Visibility = precisaMeta ? Visibility.Visible : Visibility.Collapsed;

        if (precisaMeta)
        {
            ComboMeta.ItemsSource = _servicos.Metas.ListarTodasMetas();
            ComboMeta.SelectedItem = null;
        }
    }

    private void AoMudarMeta(object sender, SelectionChangedEventArgs e) => AtualizarInfoMeta();

    private void AtualizarInfoMeta()
    {
        if (ComboMeta.SelectedItem is not Meta meta)
        {
            TxtInfoMeta.Text = "";
            return;
        }

        TxtInfoMeta.Text = CategoriaAtual switch
        {
            Categoria.AdicionarMeta => $"Faltam {Formatacao.Moeda(meta.RestanteParaAlvo)} para atingir a meta",
            Categoria.RetirarMeta => $"Disponível: {Formatacao.Moeda(meta.AtualCentavos)}",
            _ => ""
        };
    }

    private void ValidarDigitoValor(object sender, TextCompositionEventArgs e)
    {
        e.Handled = !PadraoValor.IsMatch(e.Text);
    }

    private void FormatarValorAoSair(object sender, RoutedEventArgs e)
    {
        if (CampoValor.Text.Length == 0)
        {
            return;
        }

        try
        {
            long centavos = ValorMonetario.ParseCentavos(CampoValor.Text);
            CampoValor.Text = (centavos / 100m).ToString("N2", Formatacao.CulturaBr);
        }
        catch (ArgumentException)
        {
            // deixa o usuário corrigir o texto inválido
        }
    }

    private void Salvar(object sender, RoutedEventArgs e)
    {
        MostrarErro(null);

        try
        {
            long valorCentavos = ValorMonetario.ParseCentavos(CampoValor.Text);
            if (valorCentavos <= 0)
            {
                MostrarErro("Valor deve ser maior que zero");
                return;
            }

            if (CampoData.SelectedDate is null)
            {
                MostrarErro("Selecione uma data");
                return;
            }

            var categoria = CategoriaAtual;
            if (categoria is null)
            {
                MostrarErro("Selecione uma categoria");
                return;
            }

            var carteira = (Carteira?)ComboCarteira.SelectedItem;
            if (carteira is null)
            {
                MostrarErro("Selecione uma carteira");
                return;
            }

            bool exigeMeta = categoria is Categoria.AdicionarMeta or Categoria.RetirarMeta;
            var meta = exigeMeta ? (Meta?)ComboMeta.SelectedItem : null;
            if (exigeMeta && meta is null)
            {
                MostrarErro("Selecione uma meta");
                return;
            }

            DateOnly data = DateOnly.FromDateTime(CampoData.SelectedDate.Value);

            if (_emEdicao is null)
            {
                _servicos.Transacoes.Registrar(TipoAtual, categoria.Value, valorCentavos, meta,
                    CampoComentario.Text, data, carteira.Id);
            }
            else
            {
                _servicos.Transacoes.Atualizar(_emEdicao.Id, TipoAtual, categoria.Value, valorCentavos, meta,
                    CampoComentario.Text, data, carteira.Id);
            }

            DialogResult = true;
        }
        catch (ArgumentException ex)
        {
            MostrarErro(ex.Message);
        }
        catch (Exception ex)
        {
            MostrarErro("Erro ao salvar transação: " + ex.Message);
        }
    }

    private void Excluir(object sender, RoutedEventArgs e)
    {
        if (_emEdicao is null)
        {
            return;
        }

        var resposta = MessageBox.Show(this, "Tem certeza que deseja excluir esta transação?",
            "Excluir transação", MessageBoxButton.YesNo, MessageBoxImage.Question);

        if (resposta != MessageBoxResult.Yes)
        {
            return;
        }

        try
        {
            _servicos.Transacoes.ExcluirTransacao(_emEdicao.Id);
            DialogResult = true;
        }
        catch (Exception ex)
        {
            MostrarErro("Erro ao excluir: " + ex.Message);
        }
    }

    private void Cancelar(object sender, RoutedEventArgs e) => DialogResult = false;

    private void MostrarErro(string? mensagem)
    {
        if (mensagem is null)
        {
            TxtErro.Visibility = Visibility.Collapsed;
            TxtErro.Text = "";
        }
        else
        {
            TxtErro.Text = mensagem;
            TxtErro.Visibility = Visibility.Visible;
        }
    }

    private sealed record ItemCombo(string Rotulo, Categoria Valor)
    {
        public override string ToString() => Rotulo;
    }
}
