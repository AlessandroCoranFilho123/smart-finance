using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml.Linq;

namespace SmartFinance.Tests;

/// <summary>
/// Salvaguardas de INTEGRIDADE DO PROJETO — classes de bug que o compilador
/// NÃO pega e que já chegaram ao usuário:
///
/// 1. Encoding misto (bug da 3.4.x, mojibake "jÃ¡ estÃ¡"): um único byte
///    inválido em um .cs faz o Roslyn recodificar o arquivo INTEIRO no
///    codepage ANSI e todas as strings acentuadas saem corrompidas no
///    binário. O teste exige UTF-8 estrito em todo .cs/.xaml de src/.
/// 2. Bump de versão (bug da 3.4.5, exe que não iniciava): o replace da
///    versão no app.manifest atingiu a declaração XML e o manifestVersion
///    além do assemblyIdentity, gerando um manifest inválido (erro SxS).
///    O teste valida a estrutura do manifest e a consistência de versão
///    entre app.manifest, csproj e SmartFinance.iss.
/// </summary>
public class ConsistenciaProjetoTests
{
    private static readonly string Raiz = EncontrarRaiz();

    private static string EncontrarRaiz()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir is not null && !File.Exists(Path.Combine(dir.FullName, "SmartFinance.slnx")))
        {
            dir = dir.Parent;
        }

        Assert.NotNull(dir);
        return dir!.FullName;
    }

    public static IEnumerable<object[]> FontesDoProjeto()
    {
        var src = Path.Combine(EncontrarRaiz(), "src");
        foreach (var arquivo in Directory.EnumerateFiles(src, "*", SearchOption.AllDirectories))
        {
            var ext = Path.GetExtension(arquivo).ToLowerInvariant();
            if (ext is ".cs" or ".xaml" && !arquivo.Contains($"{Path.DirectorySeparatorChar}obj{Path.DirectorySeparatorChar}")
                && !arquivo.Contains($"{Path.DirectorySeparatorChar}bin{Path.DirectorySeparatorChar}"))
            {
                yield return new object[] { Path.GetRelativePath(EncontrarRaiz(), arquivo) };
            }
        }
    }

    [Theory]
    [MemberData(nameof(FontesDoProjeto))]
    public void Fontes_SaoUtf8Estrito(string caminhoRelativo)
    {
        // Um único byte inválido derruba o arquivo inteiro para o codepage
        // ANSI no compilador (ver ARQUITETURA.md > Codificação de fontes).
        var estrito = new UTF8Encoding(false, throwOnInvalidBytes: true);
        var bytes = File.ReadAllBytes(Path.Combine(Raiz, caminhoRelativo));
        var excecao = Record.Exception(() => estrito.GetString(bytes));
        Assert.True(excecao is null,
            $"{caminhoRelativo} não é UTF-8 estrito: {excecao?.Message}. " +
            "Corrija os bytes (o texto corrompido aparece no erro); strings acentuadas viram mojibake no binário.");
    }

    [Fact]
    public void Manifest_TemEstruturaValidaEVersaoSincronizada()
    {
        var caminhoManifest = Path.Combine(Raiz, "src", "SmartFinance", "app.manifest");
        var documento = XDocument.Load(caminhoManifest);

        // a declaração XML deve seguir intacta (o bug da 3.4.5 trocou o
        // version="1.0" dela pelo número da release)
        Assert.NotNull(documento.Declaration);
        Assert.Equal("1.0", documento.Declaration!.Version);

        var ns = documento.Root!.Name.Namespace;
        Assert.Equal("1.0", documento.Root!.Attribute("manifestVersion")?.Value);

        var identidade = documento.Root.Element(ns + "assemblyIdentity")
            ?? throw new InvalidOperationException("assemblyIdentity ausente no manifest");
        var versaoManifest = identidade.Attribute("version")?.Value;

        var csproj = Path.Combine(Raiz, "src", "SmartFinance", "SmartFinance.csproj");
        var versaoCsproj = Regex.Match(File.ReadAllText(csproj), "<Version>([^<]+)</Version>").Groups[1].Value;

        Assert.Equal(versaoCsproj + ".0", versaoManifest);
    }

    [Fact]
    public void Versao_DoCsproj_CasaComOInstalador()
    {
        var csproj = File.ReadAllText(Path.Combine(Raiz, "src", "SmartFinance", "SmartFinance.csproj"));
        var versao = Regex.Match(csproj, "<Version>([^<]+)</Version>").Groups[1].Value;

        var iss = File.ReadAllText(Path.Combine(Raiz, "setup", "SmartFinance.iss"));
        var versaoIss = Regex.Match(iss, "#define VersaoApp \"([^\"]+)\"").Groups[1].Value;

        Assert.Equal(versao, versaoIss);
    }
}
