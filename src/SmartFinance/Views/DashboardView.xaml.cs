using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Data;
using SmartFinance.Services;
using SmartFinance.Views.Dialogs;

namespace SmartFinance.Views;

public partial class DashboardView : UserControl, IViewAtualizavel
{
    private readonly Servicos _servicos = App.Servicos!;

    public DashboardView()
    {
        InitializeComponent();
        Atualizar();
    }

    public void Atualizar()
    {
        var hoje = DateOnly.FromDateTime(DateTime.Today);
        var primeiroDia = new DateOnly(hoje.Year, hoje.Month, 1);

        TxtSaldo.Text = Formatacao.Moeda(_servicos.Carteiras.CalcularSaldoTotalCentavos());
        TxtReceitas.Text = Formatacao.Moeda(_servicos.Transacoes.CalcularReceitasMes(primeiroDia));
        TxtDespesas.Text = Formatacao.Moeda(_servicos.Transacoes.CalcularDespesasMes(primeiroDia));

        long saldoBanco = _servicos.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId);
        long saldoDinheiro = _servicos.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraDinheiroFisicoId);
        TxtSaldoBanco.Text = Formatacao.Moeda(saldoBanco);
        TxtSaldoDinheiro.Text = Formatacao.Moeda(saldoDinheiro);

        var nomesCarteiras = _servicos.Carteiras.ListarCarteiras()
            .ToDictionary(c => c.Id, c => c.Nome);

        ListaTransacoes.ItemsSource = _servicos.Transacoes.ListarRecentes(10)
            .Select(t => new LinhaTransacao(t, nomesCarteiras.GetValueOrDefault(t.CarteiraId ?? Database.CarteiraBancoId, "")))
            .ToList();

        ListaMetas.ItemsSource = _servicos.Metas.ListarTodasMetas()
            .Select(m => new LinhaMeta(m))
            .ToList();
    }

    private void NovaTransacao(object sender, RoutedEventArgs e) => AbrirDialogTransacao(null);

    private void EditarTransacao(object sender, MouseButtonEventArgs e)
    {
        if (InteracaoLista.ItemSobCursor<LinhaTransacao>(e, out var linha))
        {
            AbrirDialogTransacao(linha.Transacao);
        }
    }

    private void AoTeclarLista(object sender, KeyEventArgs e)
    {
        if (InteracaoLista.EnterNoSelecionado<LinhaTransacao>(e, ListaTransacoes.SelectedItem, out var linha))
        {
            AbrirDialogTransacao(linha.Transacao);
        }
    }

    private void AbrirDialogTransacao(Models.Transacao? transacao)
    {
        var dialogo = new TransacaoDialog { Owner = Window.GetWindow(this) };
        dialogo.Configurar(transacao);
        if (dialogo.ShowDialog() == true)
        {
            Atualizar();
        }
    }

    private void NovaMeta(object sender, RoutedEventArgs e) => AbrirDialogMeta(null);

    private void EditarMeta(object sender, MouseButtonEventArgs e)
    {
        if (InteracaoLista.ItemSobCursor<LinhaMeta>(e, out var linha))
        {
            AbrirDialogMeta(linha.Meta);
        }
    }

    private void AoTeclarListaMeta(object sender, KeyEventArgs e)
    {
        if (InteracaoLista.EnterNoSelecionado<LinhaMeta>(e, ListaMetas.SelectedItem, out var linha))
        {
            AbrirDialogMeta(linha.Meta);
        }
    }

    private void AbrirDialogMeta(Models.Meta? meta)
    {
        var dialogo = new MetaDialog { Owner = Window.GetWindow(this) };
        dialogo.Configurar(meta);
        if (dialogo.ShowDialog() == true)
        {
            Atualizar();
        }
    }

    private void AbrirCarteiras(object sender, RoutedEventArgs e)
    {
        var dialogo = new CarteirasDialog { Owner = Window.GetWindow(this) };
        if (dialogo.ShowDialog() == true)
        {
            Atualizar();
        }
    }

    private void AbrirTransferencia(object sender, RoutedEventArgs e)
    {
        var dialogo = new TransferenciaDialog { Owner = Window.GetWindow(this) };
        if (dialogo.ShowDialog() == true)
        {
            Atualizar();
        }
    }

    private void IrParaTransacoes(object sender, RoutedEventArgs e)
    {
        var janela = (MainWindow)Window.GetWindow(this)!;
        janela.NavTransacoes.IsChecked = true; // Checked → navega para Transações
    }
}
