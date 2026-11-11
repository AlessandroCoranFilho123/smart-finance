using System.Windows;
using System.Windows.Input;

namespace SmartFinance.Views;

/// <summary>Interação com listas: duplo clique somente sobre itens (SF-028)
/// e ativação por Enter no item selecionado.</summary>
public static class InteracaoLista
{
    public static bool ItemSobCursor<T>(MouseButtonEventArgs e, out T item) where T : class
    {
        item = (e.OriginalSource as FrameworkElement)?.DataContext as T ??
               (e.OriginalSource is FrameworkElement fe
                   ? fe.TemplatedParent as T
                   : null) ??
               default!;
        return item is not null;
    }

    public static bool EnterNoSelecionado<T>(KeyEventArgs e, object? selecionado, out T item) where T : class
    {
        if (e.Key == Key.Enter && selecionado is T t)
        {
            item = t;
            return true;
        }

        item = default!;
        return false;
    }
}
