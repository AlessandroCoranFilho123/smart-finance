using System.IO;
using Microsoft.Data.Sqlite;

namespace SmartFinance.Data;

/// <summary>
/// Acesso ao banco SQLite local. Mantém uma conexão de longa duração para a aplicação
/// e inicializa o schema compatível com a versão Java do aplicativo, incluindo as
/// migrations incrementais e o índice de busca textual (FTS5).
/// </summary>
public sealed class Database : IDisposable
{
    /// <summary>Carteiras padrão criadas no primeiro uso (mesmos IDs da versão Java).</summary>
    public static readonly Guid CarteiraBancoId = new("00000000-0000-0000-0000-000000000001");
    public static readonly Guid CarteiraDinheiroFisicoId = new("00000000-0000-0000-0000-000000000002");

    private const string CaminhoPadraoRelativo = "financas.db";

    private readonly SqliteConnectionStringBuilder _builder;
    private SqliteConnection? _connection;
    private int _profundidadeTransacao;

    public Database(string caminhoBanco)
    {
        CaminhoBanco = Path.GetFullPath(caminhoBanco);
        Directory.CreateDirectory(Path.GetDirectoryName(CaminhoBanco)!);
        _builder = new SqliteConnectionStringBuilder
        {
            DataSource = CaminhoBanco,
            Mode = SqliteOpenMode.ReadWriteCreate,
            Pooling = false
        };
    }

    public string CaminhoBanco { get; }

    /// <summary>Conexão configurada e aberta sob demanda (pragmas aplicados na abertura).</summary>
    public SqliteConnection Connection
    {
        get
        {
            if (_connection is null)
            {
                _connection = new SqliteConnection(_builder.ToString());
                _connection.Open();
                ConfigurarConexao(_connection);
            }

            return _connection;
        }
    }

    /// <summary>Resolve o caminho do banco de produção: %APPDATA%\SmartFinance\financas.db.</summary>
    public static string CaminhoPadrao()
    {
        string? overridePath = Environment.GetEnvironmentVariable("SMARTFINANCE_DB_PATH");
        if (!string.IsNullOrWhiteSpace(overridePath))
        {
            return overridePath;
        }

        string appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
        return Path.Combine(appData, "SmartFinance", CaminhoPadraoRelativo);
    }

    private static void ConfigurarConexao(SqliteConnection connection)
    {
        Executar(connection, "PRAGMA foreign_keys = ON");
        Executar(connection, "PRAGMA busy_timeout = 5000");
        Executar(connection, "PRAGMA journal_mode = WAL");
        Executar(connection, "PRAGMA synchronous = NORMAL");
        Executar(connection, "PRAGMA secure_delete = ON");
    }

    private static void Executar(SqliteConnection connection, string sql)
    {
        using var comando = connection.CreateCommand();
        comando.CommandText = sql;
        comando.ExecuteNonQuery();
    }

    /// <summary>
    /// Executa <paramref name="acao"/> dentro de uma transação SQL. Reentrante: chamadas
    /// aninhadas participam da transação externa em vez de criar uma nova.
    /// </summary>
    public void EmTransacao(Action acao)
    {
        if (_profundidadeTransacao > 0)
        {
            acao();
            return;
        }

        using var transacao = Connection.BeginTransaction();
        _profundidadeTransacao++;
        try
        {
            acao();
            transacao.Commit();
        }
        catch
        {
            transacao.Rollback();
            throw;
        }
        finally
        {
            _profundidadeTransacao--;
        }
    }

    public void Initialize()
    {
        var conn = Connection;

        EmTransacao(() =>
        {
            CriarTabelas(conn);
            MigrarColunasTransacao(conn);
            MigrarColunasPlanejamento(conn);
            CriarIndices(conn);
            CriarBuscaTextual(conn);
        });
    }

    private static void CriarTabelas(SqliteConnection conn)
    {
        Executar(conn, """
            CREATE TABLE IF NOT EXISTS meta (
                id             TEXT PRIMARY KEY,
                nome           TEXT    NOT NULL,
                alvo_centavos  INTEGER,
                atual_centavos INTEGER NOT NULL DEFAULT 0
            )
            """);

        Executar(conn, """
            CREATE TABLE IF NOT EXISTS carteira (
                id                     TEXT PRIMARY KEY,
                nome                   TEXT    NOT NULL UNIQUE,
                saldo_inicial_centavos INTEGER NOT NULL DEFAULT 0
            )
            """);

        Executar(conn, """
            INSERT OR IGNORE INTO carteira (id, nome, saldo_inicial_centavos)
            VALUES ('00000000-0000-0000-0000-000000000001', 'Banco', 0),
                   ('00000000-0000-0000-0000-000000000002', 'Dinheiro físico', 0)
            """);

        Executar(conn, """
            CREATE TABLE IF NOT EXISTS transacao (
                id             TEXT PRIMARY KEY,
                descricao      TEXT    NOT NULL,
                comentario     TEXT    NOT NULL DEFAULT '',
                valor_centavos INTEGER NOT NULL,
                tipo           TEXT    NOT NULL,
                data           TEXT    NOT NULL,
                meta_id        TEXT,
                categoria      TEXT,
                carteira_id    TEXT    DEFAULT '00000000-0000-0000-0000-000000000001',
                FOREIGN KEY (meta_id) REFERENCES meta (id) ON DELETE SET NULL,
                FOREIGN KEY (carteira_id) REFERENCES carteira (id) ON DELETE SET NULL
            )
            """);

        Executar(conn, """
            CREATE TABLE IF NOT EXISTS compra_cartao_credito (
                id                   TEXT PRIMARY KEY,
                nome                 TEXT    NOT NULL,
                valor_total_centavos INTEGER NOT NULL,
                parcelas             INTEGER NOT NULL,
                vencimento           TEXT    NOT NULL,
                parcelas_pagas       INTEGER NOT NULL DEFAULT 0,
                carteira_id          TEXT    DEFAULT '00000000-0000-0000-0000-000000000001',
                transacao_id         TEXT
            )
            """);

        Executar(conn, """
            CREATE TABLE IF NOT EXISTS emprestimo (
                id             TEXT PRIMARY KEY,
                tipo           TEXT    NOT NULL,
                nome           TEXT    NOT NULL,
                valor_centavos INTEGER NOT NULL,
                data_pagamento TEXT    NOT NULL,
                carteira_id    TEXT    DEFAULT '00000000-0000-0000-0000-000000000001',
                transacao_id   TEXT
            )
            """);

        Executar(conn, """
            CREATE TABLE IF NOT EXISTS transferencia_carteira (
                id                  TEXT PRIMARY KEY,
                origem_carteira_id  TEXT    NOT NULL,
                destino_carteira_id TEXT    NOT NULL,
                valor_centavos      INTEGER NOT NULL,
                data                TEXT    NOT NULL,
                comentario          TEXT    NOT NULL DEFAULT '',
                FOREIGN KEY (origem_carteira_id) REFERENCES carteira (id) ON DELETE CASCADE,
                FOREIGN KEY (destino_carteira_id) REFERENCES carteira (id) ON DELETE CASCADE
            )
            """);
    }

    /// <summary>Migrations das tabelas de cartão e empréstimo (D-10/D-11): colunas de
    /// carteira, vínculo com a transação gerada e parcelas pagas. Default preserva
    /// registros legados (sem movimentação, nada pago).</summary>
    private static void MigrarColunasPlanejamento(SqliteConnection conn)
    {
        AdicionarColunaSeFaltar(conn, "compra_cartao_credito", "parcelas_pagas", "INTEGER NOT NULL DEFAULT 0");
        AdicionarColunaSeFaltar(conn, "compra_cartao_credito", "carteira_id", "TEXT DEFAULT '00000000-0000-0000-0000-000000000001'");
        AdicionarColunaSeFaltar(conn, "compra_cartao_credito", "transacao_id", "TEXT");
        AdicionarColunaSeFaltar(conn, "emprestimo", "carteira_id", "TEXT DEFAULT '00000000-0000-0000-0000-000000000001'");
        AdicionarColunaSeFaltar(conn, "emprestimo", "transacao_id", "TEXT");
    }

    private static void AdicionarColunaSeFaltar(SqliteConnection conn, string tabela, string coluna, string definicao)
    {
        bool existe = false;
        using (var comando = conn.CreateCommand())
        {
            comando.CommandText = $"PRAGMA table_info({tabela})";
            using var reader = comando.ExecuteReader();
            while (reader.Read())
            {
                if (string.Equals(reader.GetString(1), coluna, StringComparison.OrdinalIgnoreCase))
                {
                    existe = true;
                    break;
                }
            }
        }

        if (!existe)
        {
            Executar(conn, $"ALTER TABLE {tabela} ADD COLUMN {coluna} {definicao}");
        }
    }

    /// <summary>Migrations de bancos criados pela versão Java 2.0 (colunas incrementais).</summary>
    private static void MigrarColunasTransacao(SqliteConnection conn)
    {
        var colunas = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        using (var comando = conn.CreateCommand())
        {
            comando.CommandText = "PRAGMA table_info(transacao)";
            using var reader = comando.ExecuteReader();
            while (reader.Read())
            {
                colunas.Add(reader.GetString(1));
            }
        }

        if (colunas.Count == 0)
        {
            return;
        }

        if (!colunas.Contains("comentario"))
        {
            Executar(conn, "ALTER TABLE transacao ADD COLUMN comentario TEXT NOT NULL DEFAULT ''");
        }

        if (!colunas.Contains("categoria"))
        {
            Executar(conn, "ALTER TABLE transacao ADD COLUMN categoria TEXT");
        }

        if (!colunas.Contains("carteira_id"))
        {
            Executar(conn, "ALTER TABLE transacao ADD COLUMN carteira_id TEXT");
        }

        Executar(conn, """
            UPDATE transacao
            SET carteira_id = '00000000-0000-0000-0000-000000000001'
            WHERE carteira_id IS NULL
            """);
    }

    private static void CriarIndices(SqliteConnection conn)
    {
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_transacao_data_desc ON transacao (data DESC)");
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_transacao_tipo_data ON transacao (tipo, data)");
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_transacao_categoria_data ON transacao (categoria, data)");
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_transacao_meta_id ON transacao (meta_id)");
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_transacao_carteira_id ON transacao (carteira_id)");
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_meta_nome ON meta (nome COLLATE NOCASE)");
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_carteira_nome ON carteira (nome COLLATE NOCASE)");
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_compra_cartao_vencimento ON compra_cartao_credito (vencimento)");
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_emprestimo_tipo_data ON emprestimo (tipo, data_pagamento)");
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_transferencia_origem ON transferencia_carteira (origem_carteira_id, data)");
        Executar(conn, "CREATE INDEX IF NOT EXISTS idx_transferencia_destino ON transferencia_carteira (destino_carteira_id, data)");
    }

    private static void CriarBuscaTextual(SqliteConnection conn)
    {
        bool indiceCriadoAgora = !ObjetoExiste(conn, "transacao_fts");

        Executar(conn, """
            CREATE VIRTUAL TABLE IF NOT EXISTS transacao_fts USING fts5(
                descricao,
                comentario,
                categoria,
                tipo,
                data,
                content='transacao',
                content_rowid='rowid',
                tokenize='unicode61 remove_diacritics 2'
            )
            """);

        Executar(conn, """
            CREATE TRIGGER IF NOT EXISTS transacao_ai AFTER INSERT ON transacao BEGIN
                INSERT INTO transacao_fts(rowid, descricao, comentario, categoria, tipo, data)
                VALUES (new.rowid, new.descricao, new.comentario, COALESCE(new.categoria, ''), new.tipo, new.data);
            END
            """);

        Executar(conn, """
            CREATE TRIGGER IF NOT EXISTS transacao_ad AFTER DELETE ON transacao BEGIN
                INSERT INTO transacao_fts(transacao_fts, rowid, descricao, comentario, categoria, tipo, data)
                VALUES ('delete', old.rowid, old.descricao, old.comentario, COALESCE(old.categoria, ''), old.tipo, old.data);
            END
            """);

        Executar(conn, """
            CREATE TRIGGER IF NOT EXISTS transacao_au AFTER UPDATE ON transacao BEGIN
                INSERT INTO transacao_fts(transacao_fts, rowid, descricao, comentario, categoria, tipo, data)
                VALUES ('delete', old.rowid, old.descricao, old.comentario, COALESCE(old.categoria, ''), old.tipo, old.data);
                INSERT INTO transacao_fts(rowid, descricao, comentario, categoria, tipo, data)
                VALUES (new.rowid, new.descricao, new.comentario, COALESCE(new.categoria, ''), new.tipo, new.data);
            END
            """);

        if (indiceCriadoAgora)
        {
            Executar(conn, "INSERT INTO transacao_fts(transacao_fts) VALUES ('rebuild')");
        }
    }

    private static bool ObjetoExiste(SqliteConnection conn, string nome)
    {
        using var comando = conn.CreateCommand();
        comando.CommandText = "SELECT 1 FROM sqlite_master WHERE name = $nome LIMIT 1";
        comando.Parameters.AddWithValue("$nome", nome);
        return comando.ExecuteScalar() is not null;
    }

    public void Dispose()
    {
        _connection?.Dispose();
        _connection = null;
    }
}
