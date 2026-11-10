using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;
using SmartFinance.Themes;

namespace SmartFinance.Views;

/// <summary>Integração nativa leve: barra de título escura no Windows 10/11 em modo escuro.</summary>
public static class JanelaNativa
{
    private const int DwmwaUseImmersiveDarkModeAntigo = 19;
    private const int DwmwaUseImmersiveDarkMode = 20;

    [DllImport("dwmapi.dll")]
    private static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int valor, int tamanho);

    public static void AoFonteInicializada(Window janela)
    {
        janela.SourceInitialized += (_, _) => AplicarBarraEscura(janela);
    }

    public static void AplicarBarraEscura(Window janela)
    {
        try
        {
            if (!TemaManager.TemaEscuroAtivo)
            {
                return;
            }

            var handle = new WindowInteropHelper(janela).Handle;
            int ligado = 1;
            if (DwmSetWindowAttribute(handle, DwmwaUseImmersiveDarkMode, ref ligado, sizeof(int)) != 0)
            {
                DwmSetWindowAttribute(handle, DwmwaUseImmersiveDarkModeAntigo, ref ligado, sizeof(int));
            }
        }
        catch (DllNotFoundException)
        {
            // Ambiente sem DWM: mantém barra de título padrão.
        }
    }
}
