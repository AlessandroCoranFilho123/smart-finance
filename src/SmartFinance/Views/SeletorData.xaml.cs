using System.ComponentModel;
using System.Windows;
using System.Windows.Automation;
using System.Windows.Controls;
using System.Windows.Input;

namespace SmartFinance.Views;

/// <summary>
/// Seletor de data por calendário (sem digitação de texto): botão que exibe a
/// data atual e abre um calendário em popup ao clicar (pedido do usuário —
/// "data deve ter opção calendário, não texto").
/// </summary>
public partial class SeletorData : UserControl
{
    public static readonly DependencyProperty DataSelecionadaProperty =
        DependencyProperty.Register(nameof(DataSelecionada), typeof(DateTime?),
            typeof(SeletorData), new FrameworkPropertyMetadata(null,
                FrameworkPropertyMetadataOptions.BindsTwoWayByDefault, OnDataSelecionadaChanged));

    public DateTime? DataSelecionada
    {
        get => (DateTime?)GetValue(DataSelecionadaProperty);
        set => SetValue(DataSelecionadaProperty, value);
    }

    /// <summary>Evento para a visão reagir quando a data muda (ex.: aplicar filtros).</summary>
    public event EventHandler? DataAlterada;

    public SeletorData()
    {
        InitializeComponent();
        Calendario.SelectedDatesChanged += (_, _) =>
        {
            DataSelecionada = Calendario.SelectedDate;
            PopupCalendario.IsOpen = false;
        };
        AtualizarTexto();
    }

    // UserControl sem peer de automação próprio: sem isto, o controle não aparece
    // na árvore UIA por AutomationId (SF-057).
    protected override System.Windows.Automation.Peers.AutomationPeer OnCreateAutomationPeer()
        => new System.Windows.Automation.Peers.FrameworkElementAutomationPeer(this);

    private void AbrirCalendario(object sender, RoutedEventArgs e)
    {
        Calendario.DisplayDate = DataSelecionada ?? DateTime.Today;
        if (DataSelecionada is not null)
        {
            Calendario.SelectedDate = DataSelecionada;
        }

        PopupCalendario.IsOpen = true;
    }

    private static void OnDataSelecionadaChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
    {
        if (d is SeletorData seletor)
        {
            seletor.AtualizarTexto();
            seletor.DataAlterada?.Invoke(seletor, EventArgs.Empty);
        }
    }

    private void AtualizarTexto()
    {
        var valor = DataSelecionada;
        TextoData.Text = valor is null
            ? "Selecione uma data"
            : valor.Value.ToString("dd/MM/yyyy");
        AutomationProperties.SetName(BotaoData, "Data: " + TextoData.Text);
    }
}
