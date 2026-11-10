using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Services;
using SmartFinance.Views.Dialogs;

namespace SmartFinance.Views;

public partial class CartoesView : UserControl, IViewAtualizavel
{
    private readonly Servicos _servicos = App.Servicos!;
    private static readonly DateOnly Hoje = DateOnly.FromDateTime(DateTime.Today);

    public CartoesView()
    {
        InitializeComponent();
        Atualizar();
    }

    public void Atualizar()
    {
        TxtAberto.Text = Formatacao.Moeda(_servicos.Cartao.CalcularTotalAbertoCentavos());
        TxtParcelasMes.Text = Formatacao.Moeda(_servicos.Cartao.CalcularTotalParcelasMesCentavos());
        long vencendo = _servicos.Cartao.ContarVencendoEmAte(7, Hoje);
        TxtVencendo.Text = $"{vencendo} compra(s)";

        ListaCompras.ItemsSource = _servicos.Cartao.ListarTodas()
            .Select(c => new LinhaCompra(c))
            .ToList();
    }

    private void NovaCompra(object sender, RoutedEventArgs e) => AbrirDialogo(null);

    private void EditarCompra(object sender, MouseButtonEventArgs e)
    {
        if (ListaCompras.SelectedItem is LinhaCompra linha)
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
