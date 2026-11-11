using System.Windows;
using System.Windows.Controls;
using SmartFinance.Themes;
using SmartFinance.Views;
using SmartFinance.Views.Dialogs;

namespace SmartFinance;

public partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        AtualizarIndicadorTema();
        TemaManager.TemaAlterado += AtualizarIndicadorTema;

        // SF-018: a janela principal também recebe a barra de título escura,
        // reaplicada quando o tema muda (liga e desliga).
        JanelaNativa.AoFonteInicializada(this);
        TemaManager.TemaAlterado += () => JanelaNativa.Aplicar(this);

        Loaded += (_, _) => VerificarAtualizacaoSeNecessario();

        AreaConteudo.Content = new DashboardView();
    }

    /// <summary>F5: checagem diária de nova versão, fora da thread de UI.</summary>
    private async void VerificarAtualizacaoSeNecessario()
    {
        if (!Services.AtualizacaoService.DeveVerificarHoje())
        {
            return;
        }

        var informacao = await Services.AtualizacaoService.VerificarNovaVersaoAsync();
        Services.AtualizacaoService.MarcarVerificadoHoje();

        if (informacao is null || !Dispatcher.CheckAccess())
        {
            if (informacao is not null)
            {
                await Dispatcher.InvokeAsync(() => NotificarAtualizacao(informacao));
            }

            return;
        }

        NotificarAtualizacao(informacao);
    }

    private void NotificarAtualizacao(Services.InformacaoAtualizacao informacao)
    {
        var resposta = MessageBox.Show(
            $"Nova versão {informacao.VersaoNova} disponível (atual: {Services.AtualizacaoService.VersaoAtual.ToString(3)}).\nAbrir a página de download?",
            "Atualização disponível",
            MessageBoxButton.YesNo,
            MessageBoxImage.Information);

        if (resposta == MessageBoxResult.Yes)
        {
            Services.AtualizacaoService.AbrirPaginaDownload(informacao.UrlDownload);
        }
    }

    private void AoNavegar(object sender, RoutedEventArgs e)
    {
        if (AreaConteudo is null)
        {
            // IsChecked="True" do NavInicio dispara este evento durante o parsing
            // do XAML, antes de AreaConteudo existir; o construtor já define o dashboard.
            return;
        }

        AreaConteudo.Content = sender switch
        {
            _ when sender == NavTransacoes => new TransacoesView(),
            _ when sender == NavMetas => new MetasView(),
            _ when sender == NavCartoes => new CartoesView(),
            _ when sender == NavEmprestimos => new EmprestimosView(),
            _ => new DashboardView()
        };
    }

    /// <summary>Recarrega os dados da visão ativa (após diálogos ou troca de tema).</summary>
    public void AtualizarVisaoAtual()
    {
        if (AreaConteudo.Content is IViewAtualizavel visao)
        {
            visao.Atualizar();
        }
    }

    private void AlternarTema(object sender, RoutedEventArgs e)
    {
        TemaManager.Aplicar(TemaManager.TemaEscuroAtivo ? TemaApp.Claro : TemaApp.Escuro);
        AtualizarVisaoAtual();
    }

    private void AtualizarIndicadorTema()
    {
        if (IconeTema is not null)
        {
            IconeTema.Source = Icones.Carregar(TemaManager.TemaEscuroAtivo ? "tema/claro.png" : "tema/escuro.png");
            TextoTema.Text = TemaManager.RotuloBotaoTema();
        }
    }
}
