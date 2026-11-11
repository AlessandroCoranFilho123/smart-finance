using System.Diagnostics;
using System.Text.Json.Serialization;
using System.Net.Http;
using System.IO;
using SmartFinance.Views;
using System.Reflection;
using System.Text.Json;

namespace SmartFinance.Services;

/// <summary>Informação sobre uma versão mais recente disponível.</summary>
public sealed record InformacaoAtualizacao(string VersaoNova, string UrlDownload);

/// <summary>
/// Verificação de atualização via GitHub Releases (F5): 1× por dia, silenciosa
/// em qualquer falha, sem envio de dados — apenas leitura da versão mais recente.
/// </summary>
public static class AtualizacaoService
{
    private const string UrlLatest =
        "https://api.github.com/repos/AlessandroCoranFilho123/smart-finance/releases/latest";

    internal static readonly string CaminhoControle = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "SmartFinance", "atualizacao.json");

    public static Version VersaoAtual => typeof(AtualizacaoService).Assembly.GetName().Version ?? new Version(0, 0, 0);

    public static bool EhMaisNova(string tag, Version atual)
    {
        return Version.TryParse(tag.TrimStart('v', 'V', ' '), out var versao) && versao > atual;
    }

    public static async Task<InformacaoAtualizacao?> VerificarNovaVersaoAsync()
    {
        try
        {
            using var http = new HttpClient();
            http.Timeout = TimeSpan.FromSeconds(5);
            http.DefaultRequestHeaders.UserAgent.ParseAdd("SmartFinance-Desktop");
            using var resposta = await http.GetAsync(UrlLatest);
            resposta.EnsureSuccessStatusCode();

            await using var fluxo = await resposta.Content.ReadAsStreamAsync();
            using var json = await JsonDocument.ParseAsync(fluxo);
            string tag = json.RootElement.GetProperty("tag_name").GetString() ?? string.Empty;
            string url = json.RootElement.TryGetProperty("html_url", out var u) ? u.GetString() ?? UrlLatest : UrlLatest;

            return EhMaisNova(tag, VersaoAtual)
                ? new InformacaoAtualizacao(tag.TrimStart('v', 'V', ' '), url)
                : null;
        }
        catch
        {
            // qualquer falha de rede/parse = checagem silenciosamente ignorada
            return null;
        }
    }

    /// <summary>Deve verificar agora? (1× por dia)</summary>
    public static bool DeveVerificarHoje()
    {
        try
        {
            if (!File.Exists(CaminhoControle))
            {
                return true;
            }

            using var json = JsonDocument.Parse(File.ReadAllText(CaminhoControle));
            string ultima = json.RootElement.GetProperty("ultimaVerificacao").GetString() ?? "";
            return ultima != DateTime.Today.ToString("yyyy-MM-dd");
        }
        catch
        {
            return true;
        }
    }

    public static void MarcarVerificadoHoje()
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(CaminhoControle)!);
            File.WriteAllText(CaminhoControle,
                JsonSerializer.Serialize(new Controle(DateTime.Today.ToString("yyyy-MM-dd"))));
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
        {
        }
    }

    /// <summary>Abre a página da release no navegador padrão.</summary>
    public static void AbrirPaginaDownload(string url)
    {
        Process.Start(new ProcessStartInfo(url) { UseShellExecute = true });
    }

    private sealed record Controle([property: JsonPropertyName("ultimaVerificacao")] string UltimaVerificacao);
}
