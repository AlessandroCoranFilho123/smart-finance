using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Reflection;

using System.Security.Cryptography;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;

namespace SmartFinance.Services;

/// <summary>Informação sobre uma versão mais recente disponível.</summary>
public sealed record InformacaoAtualizacao(
    string VersaoNova,
    string UrlDownload,
    string? UrlInstalador = null,
    string? HashInstalador = null);

/// <summary>
/// Verificação e instalação de atualizações via GitHub Releases (F5/F6):
/// checagem 1× por dia, silenciosa em qualquer falha, sem envio de dados.
/// Quando há versão nova com instalador publicado, baixa, confere o SHA-256
/// contra o hash das notas e reinstala silenciosamente (o instalador reabre
/// o aplicativo ao terminar).
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

            if (!EhMaisNova(tag, VersaoAtual))
            {
                return null;
            }

            // instalador (setup) para a atualização automática + hash publicado nas notas
            string? urlInstalador = null;
            string? corpoNotas = json.RootElement.TryGetProperty("body", out var b) ? b.GetString() : null;
            if (json.RootElement.TryGetProperty("assets", out var assets) && assets.GetArrayLength() > 0)
            {
                foreach (var asset in assets.EnumerateArray())
                {
                    string nome = asset.GetProperty("name").GetString() ?? "";
                    if (nome.StartsWith("SmartFinance-Setup-", StringComparison.OrdinalIgnoreCase))
                    {
                        urlInstalador = asset.GetProperty("browser_download_url").GetString();
                        break;
                    }
                }
            }

            return new InformacaoAtualizacao(
                tag.TrimStart('v', 'V', ' '),
                url,
                urlInstalador,
                ExtrairHashSetup(corpoNotas));
        }
        catch
        {
            // qualquer falha de rede/parse = checagem silenciosamente ignorada
            return null;
        }
    }

    /// <summary>Hash SHA-256 do instalador publicado nas notas (linha "SmartFinance-Setup").</summary>
    public static string? ExtrairHashSetup(string? corpoNotas)
    {
        if (string.IsNullOrEmpty(corpoNotas))
        {
            return null;
        }

        var m = Regex.Match(corpoNotas, @"SmartFinance-Setup[^`]*`: `([0-9a-f]{64})`");
        return m.Success ? m.Groups[1].Value : null;
    }

    /// <summary>Baixa o instalador para a pasta de atualização; retorna o caminho baixado.</summary>
    public static async Task<string> BaixarInstaladorAsync(string urlInstalador, string versaoNova)
    {
        string pasta = Path.Combine(Path.GetTempPath(), "SmartFinance-Update");
        Directory.CreateDirectory(pasta);
        string destino = Path.Combine(pasta, $"SmartFinance-Setup-{versaoNova}.exe");

        using var http = new HttpClient();
        http.Timeout = TimeSpan.FromMinutes(10);
        http.DefaultRequestHeaders.UserAgent.ParseAdd("SmartFinance-Desktop");

        using var resposta = await http.GetAsync(urlInstalador, HttpCompletionOption.ResponseHeadersRead);
        resposta.EnsureSuccessStatusCode();
        await using var origem = await resposta.Content.ReadAsStreamAsync();
        await using var destinoFs = new FileStream(destino, FileMode.Create, FileAccess.Write, FileShare.None);
        await origem.CopyToAsync(destinoFs);

        return destino;
    }

    /// <summary>Confere o SHA-256 do instalador baixado contra o hash publicado.</summary>
    public static bool VerificarIntegridade(string caminhoArquivo, string hashEsperado)
    {
        using var sha = SHA256.Create();
        using var fluxo = File.OpenRead(caminhoArquivo);
        string calculado = Convert.ToHexString(sha.ComputeHash(fluxo)).ToLowerInvariant();
        return string.Equals(calculado, hashEsperado.ToLowerInvariant(), StringComparison.Ordinal);
    }

    /// <summary>Inicia o instalador silencioso e encerra o aplicativo; o instalador
    /// substitui os arquivos e reabre o aplicativo ao terminar.</summary>
    public static void InstalarAtualizacao(string caminhoSetup)
    {
        Process.Start(new ProcessStartInfo(caminhoSetup)
        {
            Arguments = "/VERYSILENT /SUPPRESSMSGBOXES /NORESTART /CLOSEAPPLICATIONS",
            UseShellExecute = true
        });
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
