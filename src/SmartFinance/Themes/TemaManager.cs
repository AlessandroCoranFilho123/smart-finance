using System.IO;
using System.Windows;
using Microsoft.Win32;
using System.Text.Json;

namespace SmartFinance.Themes;

public enum TemaApp
{
    Sistema,
    Claro,
    Escuro
}

/// <summary>Gerencia o tema visual (claro/escuro) com persistência em settings.json.</summary>
public static class TemaManager
{
    private static readonly string CaminhoConfig =
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "SmartFinance", "settings.json");

    private const string DicionarioClaro = "pack://application:,,,/Themes/Palette.Light.xaml";
    private const string DicionarioEscuro = "pack://application:,,,/Themes/Palette.Dark.xaml";

    public static TemaApp TemaSelecionado { get; private set; } = TemaApp.Sistema;

    public static bool TemaEscuroAtivo { get; private set; }

    public static event Action? TemaAlterado;

    public static void Inicializar()
    {
        Aplicar(Carregar());
    }

    public static void Aplicar(TemaApp tema)
    {
        TemaSelecionado = tema;
        bool escuro = tema == TemaApp.Escuro || (tema == TemaApp.Sistema && SistemaEmModoEscuro());
        TrocarPaleta(escuro);
        TemaEscuroAtivo = escuro;
        Salvar(tema);
        TemaAlterado?.Invoke();
    }

    public static string RotuloBotaoTema() => TemaEscuroAtivo ? "Tema claro" : "Tema escuro";

    private static bool SistemaEmModoEscuro()
    {
        try
        {
            using var chave = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Themes\Personalize");
            return chave?.GetValue("AppsUseLightTheme") is 0;
        }
        catch
        {
            return false;
        }
    }

    private static void TrocarPaleta(bool escuro)
    {
        var dicionarios = Application.Current.Resources.MergedDictionaries;
        int indice = dicionarios.IndexOf(d => d.Source?.OriginalString.Contains("Palette.") == true);
        if (indice < 0)
        {
            indice = 0;
            dicionarios.Insert(0, new ResourceDictionary());
        }

        dicionarios[indice] = new ResourceDictionary
        {
            Source = new Uri(escuro ? DicionarioEscuro : DicionarioClaro)
        };
    }

    private static TemaApp Carregar()
    {
        try
        {
            if (!File.Exists(CaminhoConfig))
            {
                return TemaApp.Sistema;
            }

            var config = JsonSerializer.Deserialize<Configuracao>(File.ReadAllText(CaminhoConfig));
            return config?.Tema?.ToLowerInvariant() switch
            {
                "claro" => TemaApp.Claro,
                "escuro" => TemaApp.Escuro,
                _ => TemaApp.Sistema
            };
        }
        catch
        {
            return TemaApp.Sistema;
        }
    }

    private static void Salvar(TemaApp tema)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(CaminhoConfig)!);
            string texto = JsonSerializer.Serialize(new Configuracao(
                tema switch
                {
                    TemaApp.Claro => "claro",
                    TemaApp.Escuro => "escuro",
                    _ => "sistema"
                }));
            File.WriteAllText(CaminhoConfig, texto);
        }
        catch (IOException)
        {
            // Falha ao salvar preferência não deve interromper o aplicativo.
        }
    }

    private sealed record Configuracao(string Tema);
}

file static class DicionarioExt
{
    public static int IndexOf(this System.Collections.ObjectModel.Collection<ResourceDictionary> colecao,
        Func<ResourceDictionary, bool> predicado)
    {
        for (int i = 0; i < colecao.Count; i++)
        {
            if (predicado(colecao[i]))
            {
                return i;
            }
        }

        return -1;
    }
}
