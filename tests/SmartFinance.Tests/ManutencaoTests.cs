using SmartFinance.Data;
using SmartFinance.Services;

namespace SmartFinance.Tests;

public class ManutencaoTests
{
    [Fact]
    public void BackupDiario_CriaArquivoComDados()
    {
        using var app = new TestApp();
        app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 123_00);
        string pasta = NovoDir();

        var backup = new BackupService(app.Db.CaminhoBanco, pasta);
        string? criado = backup.ExecutarBackupDiario();

        Assert.NotNull(criado);
        Assert.True(File.Exists(criado));
        Assert.Contains(DateTime.Today.ToString("yyyy-MM-dd"), criado);

        // o snapshot deve conter os dados
        using var leitura = new Microsoft.Data.Sqlite.SqliteConnection($"Data Source={criado};Mode=ReadOnly;Pooling=false");
        leitura.Open();
        using var comando = leitura.CreateCommand();
        comando.CommandText = "SELECT COUNT(*) FROM carteira";
        Assert.Equal(2L, (long)comando.ExecuteScalar()!);
    }

    [Fact]
    public void BackupDiario_SegundaVezNoMesmoDia_EhDispensado()
    {
        using var app = new TestApp();
        string pasta = NovoDir();
        var backup = new BackupService(app.Db.CaminhoBanco, pasta);

        Assert.NotNull(backup.ExecutarBackupDiario());
        Assert.Null(backup.ExecutarBackupDiario());
        Assert.Single(Directory.GetFiles(pasta, "financas-*.db"));
    }

    [Fact]
    public void BackupDiario_PodaAlemDoLimite()
    {
        using var app = new TestApp();
        string pasta = NovoDir();
        // simula 9 backups antigos
        for (int i = 1; i <= 9; i++)
        {
            string antigo = Path.Combine(pasta, $"financas-2026-10-{i:00}.db");
            File.WriteAllText(antigo, "lixo");
            File.SetLastWriteTime(antigo, DateTime.Now.AddDays(-i));
        }

        var backup = new BackupService(app.Db.CaminhoBanco, pasta, manter: 7);
        backup.ExecutarBackupDiario();

        // retenção: os 7 mais recentes no total (incluindo o de hoje)
        Assert.Equal(7, Directory.GetFiles(pasta, "financas-*.db").Length);
    }

    [Fact]
    public void RotacaoDeLog_ArquivaAposUmMegabyte()
    {
        string pasta = NovoDir();
        string log = Path.Combine(pasta, "erro.log");
        File.WriteAllText(log, new string('x', 1_100_000));

        App.RotacionarSeNecessario(log);

        Assert.False(File.Exists(log));
        Assert.Single(Directory.GetFiles(pasta, "erro-*.log"));
    }

    [Fact]
    public void RotacaoDeLog_LogPequeno_NaoFazNada()
    {
        string pasta = NovoDir();
        string log = Path.Combine(pasta, "erro.log");
        File.WriteAllText(log, "linha");

        App.RotacionarSeNecessario(log);

        Assert.True(File.Exists(log));
        Assert.Empty(Directory.GetFiles(pasta, "erro-*.log"));
    }

    private static string NovoDir()
    {
        string dir = Path.Combine(Path.GetTempPath(), "sf-manutencao-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dir);
        return dir;
    }
}
