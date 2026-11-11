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

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern IntPtr FindWindowW(string? classe, string titulo);

    [DllImport("user32.dll")]
    private static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    private static extern bool ShowWindow(IntPtr hWnd, int comando);

    /// <summary>Traz a janela de uma instância já em execução para frente (F3).</summary>
    public static void TrazerExistenteParaFrente()
    {
        try
        {
            var handle = FindWindowW(null, "Smart Finance");
            if (handle != IntPtr.Zero)
            {
                ShowWindow(handle, 9); // SW_RESTORE
                SetForegroundWindow(handle);
            }
        }
        catch (DllNotFoundException)
        {
        }
    }

    public static void AoFonteInicializada(Window janela)
    {
        janela.SourceInitialized += (_, _) => Aplicar(janela);
    }

    /// <summary>Aplica (ou desliga) a barra de título escura conforme o tema ativo.
    /// Chame de novo quando o tema mudar (SF-018).</summary>
    public static void Aplicar(Window janela)
    {
        try
        {
            var handle = new WindowInteropHelper(janela).Handle;
            if (handle == IntPtr.Zero)
            {
                return;
            }

            int modo = TemaManager.TemaEscuroAtivo ? 1 : 0;
            if (DwmSetWindowAttribute(handle, DwmwaUseImmersiveDarkMode, ref modo, sizeof(int)) != 0)
            {
                DwmSetWindowAttribute(handle, DwmwaUseImmersiveDarkModeAntigo, ref modo, sizeof(int));
            }
        }
        catch (DllNotFoundException)
        {
            // Ambiente sem DWM: mantém barra de título padrão.
        }
    }
}
