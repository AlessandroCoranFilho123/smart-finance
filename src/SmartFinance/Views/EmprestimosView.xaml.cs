using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Models;
using SmartFinance.Services;
using SmartFinance.Views.Dialogs;

namespace SmartFinance.Views;

public partial class EmprestimosView : UserControl, IViewAtualizavel
{
    private readonly Servicos _servicos = App.Servicos!;

    public EmprestimosView()
    {
        InitializeComponent();
        Atualizar();
    }

    public void Atualizar()
    {
        TxtEmprestei.Text = Formatacao.Moeda(_servicos.Emprestimos.TotalPorTipoCentavos(TipoEmprestimo.Emprestei));
        TxtPedi.Text = Formatacao.Moeda(_servicos.Emprestimos.TotalPorTipoCentavos(TipoEmprestimo.PediEmprestado));

        ListaEmprestei.ItemsSource = _servicos.Emprestimos.ListarPorTipo(TipoEmprestimo.Emprestei)
            .Select(e => new LinhaEmprestimo(e))
            .ToList();
        ListaPedi.ItemsSource = _servicos.Emprestimos.ListarPorTipo(TipoEmprestimo.PediEmprestado)
            .Select(e => new LinhaEmprestimo(e))
            .ToList();
    }

    private void NovoEmprestimo(object sender, RoutedEventArgs e) => AbrirDialogo(null);

    private void EditarSelecionado(object sender, MouseButtonEventArgs e)
    {
        var origem = (ListBox)sender;
        if (origem.SelectedItem is LinhaEmprestimo linha)
        {
            AbrirDialogo(linha.Emprestimo);
        }
    }

    private void AbrirDialogo(Models.Emprestimo? emprestimo)
    {
        var dialogo = new EmprestimoDialog { Owner = Window.GetWindow(this) };
        dialogo.Configurar(emprestimo);
        if (dialogo.ShowDialog() == true)
        {
            Atualizar();
        }
    }
}
