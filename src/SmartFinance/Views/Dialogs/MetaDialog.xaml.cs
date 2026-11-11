
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Models;
using SmartFinance.Services;

namespace SmartFinance.Views.Dialogs;

public partial class MetaDialog : Window
{
    private readonly Servicos _servicos = App.Servicos!;
    private Meta? _emEdicao;

    public MetaDialog()
    {
        InitializeComponent();
        JanelaNativa.AoFonteInicializada(this);
    }

    public void Configurar(Meta? meta)
    {
        _emEdicao = meta;

        if (meta is null)
        {
            CampoValor.Text = "";
            BotaoSalvar.Content = "Salvar";
            return;
        }

        TxtTitulo.Text = "Detalhes da meta";
        Title = "Detalhes da meta";
        CampoNome.Text = meta.Nome;
        CampoValor.Text = meta.AlvoCentavos is null
            ? ""
            : (meta.AlvoCentavos.Value / 100m).ToString("N2", Formatacao.CulturaBr);
        BotaoSalvar.Content = "Atualizar";
        BotaoExcluir.Visibility = Visibility.Visible;
        BoxProgresso.Visibility = Visibility.Visible;
        AtualizarProgresso();
    }

    private void AtualizarProgresso()
    {
        if (_emEdicao is null)
        {
            return;
        }

        // Valor mostrado em tela é a fonte da verdade durante a edição.
        long alvo;
        try
        {
            alvo = ValorMonetario.ParseCentavos(CampoValor.Text);
        }
        catch (ArgumentException)
        {
            alvo = _emEdicao.AlvoCentavos ?? 0;
        }

        TxtAtual.Text = Formatacao.Moeda(_emEdicao.AtualCentavos);
        TxtRestante.Text = alvo > _emEdicao.AtualCentavos
            ? Formatacao.Moeda(alvo - _emEdicao.AtualCentavos)
            : "—";
        TxtProgresso.Text = alvo > 0
            ? $"{Math.Min(100, Math.Round(_emEdicao.AtualCentavos * 100.0 / alvo))}%"
            : "0%";
    }

    private void ValidarDigitoValor(object sender, TextCompositionEventArgs e) => MascaraValor.ValidarDigitos(sender, e);

    private void FormatarAoSair(object sender, RoutedEventArgs e) => MascaraValor.FormatarAoSair((System.Windows.Controls.TextBox)sender);

    private void Salvar(object sender, RoutedEventArgs e)
    {
        MostrarErro(null);
        try
        {
            string nome = CampoNome.Text.Trim();
            if (nome.Length == 0)
            {
                MostrarErro("Nome não pode estar vazio");
                return;
            }

            long alvo = ValorMonetario.ParseCentavos(CampoValor.Text);
            if (_emEdicao is null)
            {
                _servicos.Metas.CriarMeta(nome, alvo);
            }
            else
            {
                var meta = new Meta(_emEdicao.Id, nome, alvo, _emEdicao.AtualCentavos);
                _servicos.Metas.AtualizarMeta(meta);
            }

            DialogResult = true;
        }
        catch (ArgumentException ex)
        {
            MostrarErro(ex.Message);
        }
    }

    private void Excluir(object sender, RoutedEventArgs e)
    {
        if (_emEdicao is null)
        {
            return;
        }

        var resposta = MessageBox.Show(this,
            "Tem certeza que deseja excluir esta meta?\nAs transações vinculadas serão mantidas, mas perdem o vínculo.",
            "Excluir meta", MessageBoxButton.YesNo, MessageBoxImage.Question);

        if (resposta != MessageBoxResult.Yes)
        {
            return;
        }

        try
        {
            _servicos.Metas.DeletarMeta(_emEdicao.Id);
            DialogResult = true;
        }
        catch (ArgumentException ex)
        {
            MostrarErro(ex.Message);
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
}
