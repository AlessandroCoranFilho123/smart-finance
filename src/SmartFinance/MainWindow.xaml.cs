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

    /// <summary>F5/F6: checagem diária de nova versão; se houver instalador
    /// publicado, oferece baixar e atualizar automaticamente.</summary>
    private async void VerificarAtualizacaoSeNecessario()
    {
        if (!Services.AtualizacaoService.DeveVerificarHoje())
        {
            return;
        }

        var informacao = await Services.AtualizacaoService.VerificarNovaVersaoAsync();
        Services.AtualizacaoService.MarcarVerificadoHoje();

        if (informacao is null)
        {
            return;
        }

        await Dispatcher.InvokeAsync(() => OferecerAtualizacao(informacao));
    }

    private async void OferecerAtualizacao(Services.InformacaoAtualizacao informacao)
    {
        bool comInstalador = !string.IsNullOrEmpty(informacao.UrlInstalador);
        string pergunta = comInstalador
            ? $"Nova versão {informacao.VersaoNova} disponível (atual: {Services.AtualizacaoService.VersaoAtual.ToString(3)}).\n\nBaixar e instalar agora? O aplicativo será fechado e reaberto automaticamente."
            : $"Nova versão {informacao.VersaoNova} disponível (atual: {Services.AtualizacaoService.VersaoAtual.ToString(3)}).\n\nAbrir a página de download?";

        var resposta = MessageBox.Show(pergunta, "Atualização disponível",
            MessageBoxButton.YesNo, MessageBoxImage.Information);

        if (resposta != MessageBoxResult.Yes)
        {
            return;
        }

        if (!comInstalador)
        {
            Services.AtualizacaoService.AbrirPaginaDownload(informacao.UrlDownload);
            return;
        }

        // baixa com janela de progresso
        var janelaBaixando = CriarJanelaBaixando(informacao.VersaoNova);
        janelaBaixando.Show();
        string caminhoSetup;
        try
        {
            caminhoSetup = await Services.AtualizacaoService
                .BaixarInstaladorAsync(informacao.UrlInstalador!, informacao.VersaoNova);
        }
        catch (Exception ex)
        {
            janelaBaixando.Close();
            MessageBox.Show($"Falha ao baixar a atualização:\n{ex.Message}", "Atualização",
                MessageBoxButton.OK, MessageBoxImage.Error);
            return;
        }

        janelaBaixando.Close();

        // integridade: se o hash do instalador foi publicado, confere antes de executar
        if (informacao.HashInstalador is not null
            && !Services.AtualizacaoService.VerificarIntegridade(caminhoSetup, informacao.HashInstalador))
        {
            MessageBox.Show("O arquivo baixado não confere com o hash publicado. A atualização foi cancelada.",
                "Atualização", MessageBoxButton.OK, MessageBoxImage.Error);
            return;
        }

        var instalar = MessageBox.Show(
            "Download concluído e verificado. Instalar agora? O aplicativo será fechado e reaberto automaticamente.",
            "Atualização pronta", MessageBoxButton.OKCancel, MessageBoxImage.Question);

        if (instalar != MessageBoxResult.OK)
        {
            return;
        }

        Services.AtualizacaoService.InstalarAtualizacao(caminhoSetup);
        Application.Current.Shutdown();
    }

    private static Window CriarJanelaBaixando(string versaoNova)
    {
        return new Window
        {
            Title = "Atualização",
            Width = 380,
            Height = 140,
            WindowStartupLocation = WindowStartupLocation.CenterScreen,
            ResizeMode = ResizeMode.NoResize,
            Content = new System.Windows.Controls.TextBlock
            {
                Text = $"Baixando a versão {versaoNova}…\nO instalador tem cerca de 45 MB.",
                Margin = new Thickness(20),
                TextWrapping = TextWrapping.Wrap,
                FontSize = 13
            }
        };
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
