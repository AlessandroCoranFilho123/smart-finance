using System.Diagnostics;
using System.IO;
using Microsoft.Data.Sqlite;

namespace SmartFinance.Services;

/// <summary>
/// Backup diário do banco via API de backup online do SQLite (snapshot consistente
/// mesmo com a conexão aberta). Mantém no máximo os arquivos mais recentes.
/// </summary>
public sealed class BackupService(string caminhoBanco, string pastaBackups, int manter = 7)
{
    public string PastaBackups { get; } = pastaBackups;
    public int Manter { get; } = manter;

    /// <summary>Cria o backup do dia se ainda não existir; retorna o caminho ou null se dispensado.</summary>
    public string? ExecutarBackupDiario()
    {
        Directory.CreateDirectory(PastaBackups);
        string destino = Path.Combine(PastaBackups, $"financas-{DateTime.Today:yyyy-MM-dd}.db");

        if (File.Exists(destino))
        {
            Podar();
            return null;
        }

        var origemStr = new SqliteConnectionStringBuilder { DataSource = caminhoBanco, Mode = SqliteOpenMode.ReadOnly, Pooling = false };
        var destinoStr = new SqliteConnectionStringBuilder { DataSource = destino, Pooling = false };
        using var origem = new SqliteConnection(origemStr.ConnectionString);
        using var backupConn = new SqliteConnection(destinoStr.ConnectionString);
        origem.Open();
        backupConn.Open();
        origem.BackupDatabase(backupConn);

        Podar();
        return destino;
    }

    private void Podar()
    {
        var antigos = Directory.GetFiles(PastaBackups, "financas-*.db")
            .OrderByDescending(File.GetLastWriteTime)
            .Skip(Manter);
        foreach (string arquivo in antigos)
        {
            try
            {
                File.Delete(arquivo);
            }
            catch (IOException)
            {
                // arquivo em uso: tentativa de limpeza best-effort
            }
        }
    }
}
