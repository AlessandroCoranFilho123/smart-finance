using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SmartFinance.Views.Dialogs;

namespace SmartFinance.Views;

public partial class MetasView : UserControl, IViewAtualizavel
{
    private readonly Servicos _servicos = App.Servicos!;

    public MetasView()
    {
        InitializeComponent();
        Atualizar();
    }

    public void Atualizar()
    {
        ListaMetas.ItemsSource = _servicos.Metas.ListarTodasMetas()
            .Select(m => new LinhaMeta(m))
            .ToList();
    }

    private void NovaMeta(object sender, RoutedEventArgs e) => AbrirDialogo(null);

    private void EditarMeta(object sender, MouseButtonEventArgs e)
    {
        if (InteracaoLista.ItemSobCursor<LinhaMeta>(e, out var linha))
        {
            AbrirDialogo(linha.Meta);
        }
    }

    private void AoTeclarLista(object sender, KeyEventArgs e)
    {
        if (InteracaoLista.EnterNoSelecionado<LinhaMeta>(e, ListaMetas.SelectedItem, out var linha))
        {
            AbrirDialogo(linha.Meta);
        }
    }

    private void AbrirDialogo(Models.Meta? meta)
    {
        var dialogo = new MetaDialog { Owner = Window.GetWindow(this) };
        dialogo.Configurar(meta);
        if (dialogo.ShowDialog() == true)
        {
            Atualizar();
        }
    }
}
