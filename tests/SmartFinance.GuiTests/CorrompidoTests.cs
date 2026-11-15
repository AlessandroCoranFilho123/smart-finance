using System.Diagnostics;
using System.Drawing.Imaging;
using System.IO;
using FlaUI.Core;
using FlaUI.Core.AutomationElements;
using FlaUI.Core.Tools;
using FlaUI.UIA3;
using Xunit;

namespace SmartFinance.GuiTests;

/// <summary>
/// SF-009: banco corrompido deve mostrar mensagem clara e encerrar. Classe
/// PRÓPRIA (sem app prévio na fixture): se outro processo do app estiver vivo,
/// o mutex de instância única mostra "já está em execução" ANTES do bootstrap
/// e o cenário de corrupção nunca é alcançado (bug detectado na auditoria de
/// 15/11/2026 — o teste antigo validava o diálogo errado).
/// </summary>
[Trait("Category", "Captura")]
public class CorrompidoTests
{
    [Fact]
    public void SF009_BancoCorrompido_MostraErroEEncerra()
    {
        var dirTemp = Path.Combine(Path.GetTempPath(), "sf-corrompido-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dirTemp);
        var caminhoBanco = Path.Combine(dirTemp, "corrompido.db");
        File.WriteAllBytes(caminhoBanco, new byte[] { 0xDE, 0xAD, 0xBE, 0xEF, 0x00, 0x01 });

        var pastaCapturas = Path.GetFullPath(Path.Combine(
            AppContext.BaseDirectory, "..", "..", "..", "..", "..", "docs", "auditoria", "imagens"));

        var psi = new ProcessStartInfo
        {
            FileName = Path.Combine(AppContext.BaseDirectory, "SmartFinance.exe"),
            UseShellExecute = false
        };
        psi.EnvironmentVariables["SMARTFINANCE_DB_PATH"] = caminhoBanco;
        using var processo = Process.Start(psi)!;
        using var automacao = new UIA3Automation();

        var caixa = Retry.WhileNull(() => Application.Attach(processo)
                .GetAllTopLevelWindows(automacao)
                .FirstOrDefault(w => w.Properties.Name.ValueOrDefault == "Smart Finance"),
            TimeSpan.FromSeconds(20), ignoreException: true).Result
            ?? throw new InvalidOperationException("mensagem de erro não apareceu");

        // o diálogo deve ser o de FALHA DE INICIALIZAÇÃO, não o de instância única
        var textos = string.Join(" ", caixa
            .FindAllDescendants(cf => cf.ByControlType(FlaUI.Core.Definitions.ControlType.Text))
            .Select(t => t.Name));
        Assert.Contains("Falha ao iniciar", textos);

        using (var bitmap = caixa.Capture())
        {
            bitmap.Save(Path.Combine(pastaCapturas, "20-erro-banco-corrompido.png"), ImageFormat.Png);
        }

        caixa.FindFirstDescendant(cf => cf.ByName("OK"))!.AsButton().Invoke();
        Assert.True(processo.WaitForExit(10_000), "processo deve encerrar após o usuário fechar a mensagem (SF-009)");

        try
        {
            Directory.Delete(dirTemp, recursive: true);
        }
        catch (IOException)
        {
        }
    }
}
