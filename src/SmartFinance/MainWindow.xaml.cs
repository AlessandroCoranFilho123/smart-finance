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
        AreaConteudo.Content = new DashboardView();
    }

    private void AoNavegar(object sender, RoutedEventArgs e)
    {
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

    /// <summary>Abre o diálogo de transação usado pelo dashboard e pela lista de transações.</summary>
    internal void AbrirDialogTransacao(Models.Transacao? transacao = null)
    {
        var dialogo = new TransacaoDialog { Owner = this };
        dialogo.Configurar(transacao);
        dialogo.ShowDialog();
        if (dialogo.DialogResult == true)
        {
            AtualizarVisaoAtual();
        }
    }
}
