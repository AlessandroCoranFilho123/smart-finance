using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Services;
using SmartFinance.Views.Dialogs;

namespace SmartFinance.Views;

public partial class CartoesView : UserControl, IViewAtualizavel
{
    private readonly Servicos _servicos = App.Servicos!;
    

    public CartoesView()
    {
        InitializeComponent();
        Atualizar();
    }

    public void Atualizar()
    {
        TxtAberto.Text = Formatacao.Moeda(_servicos.Cartao.CalcularTotalAbertoCentavos());
        TxtParcelasMes.Text = Formatacao.Moeda(_servicos.Cartao.CalcularTotalParcelasMesCentavos());
        var hoje = DateOnly.FromDateTime(DateTime.Today); // SF-016: data calculada a cada atualização
        long vencendo = _servicos.Cartao.ContarVencendoEmAte(7, hoje);
        TxtVencendo.Text = $"{vencendo} compra(s)";

        ListaCompras.ItemsSource = _servicos.Cartao.ListarTodas()
            .Select(c => new LinhaCompra(c, hoje))
            .ToList();
    }

    private void NovaCompra(object sender, RoutedEventArgs e) => AbrirDialogo(null);

    private void EditarCompra(object sender, MouseButtonEventArgs e)
    {
        if (InteracaoLista.ItemSobCursor<LinhaCompra>(e, out var linha))
        {
            AbrirDialogo(linha.Compra);
        }
    }

    private void AoTeclarLista(object sender, KeyEventArgs e)
    {
        if (InteracaoLista.EnterNoSelecionado<LinhaCompra>(e, ListaCompras.SelectedItem, out var linha))
        {
            AbrirDialogo(linha.Compra);
        }
    }

    private void AbrirDialogo(Models.CompraCartaoCredito? compra)
    {
        var dialogo = new CartaoDialog { Owner = Window.GetWindow(this) };
        dialogo.Configurar(compra);
        if (dialogo.ShowDialog() == true)
        {
            Atualizar();
        }
    }
}
