using Microsoft.Data.Sqlite;
using SmartFinance.Data;
using SmartFinance.Models;
using SmartFinance.Services;

namespace SmartFinance.Tests;

/// <summary>
/// Testes da auditoria externa estática (SF-001…SF-037), conforme
/// 14_TEST_PLAN_FOR_GLM.md e decisões em DECISOES_PRODUTO.md.
/// </summary>
public class AuditoriaExternaTests : IDisposable
{
    private readonly TestApp _app = new();
    private static readonly DateOnly Hoje = new(2026, 11, 10);

    // ── SF-029: coerência Tipo × Categoria (T-003) ────────────────────────

    [Theory]
    [InlineData(TipoTransacao.Entrada, Categoria.Aluguel)]
    [InlineData(TipoTransacao.Saida, Categoria.Salario)]
    [InlineData(TipoTransacao.Entrada, Categoria.AdicionarMeta)]
    [InlineData(TipoTransacao.Saida, Categoria.RetirarMeta)]
    public void Registrar_TipoECategoriaIncoerentes_SaoRecusados(TipoTransacao tipo, Categoria categoria)
    {
        Assert.Throws<ArgumentException>(() =>
            _app.Transacoes.Registrar(tipo, categoria, 100_00, null, "", Hoje));
        Assert.Empty(_app.TransacaoRepo.ListarTodas());
    }

    [Theory]
    [InlineData(TipoTransacao.Entrada, Categoria.Salario)]
    [InlineData(TipoTransacao.Saida, Categoria.Emprestimo)]
    [InlineData(TipoTransacao.Entrada, Categoria.RetirarMeta)]
    [InlineData(TipoTransacao.Saida, Categoria.AdicionarMeta)]
    public void Registrar_TipoECategoriaCoerentes_SaoAceitos(TipoTransacao tipo, Categoria categoria)
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        _app.Metas.CriarMeta("Destino", 1000_00);
        var meta = _app.Metas.ListarTodasMetas().Single();
        meta.Adicionar(500_00); // acumulado para o caso Resgatar meta
        _app.MetaRepo.Atualizar(meta);

        var excecao = Record.Exception(() =>
            _app.Transacoes.Registrar(tipo, categoria, 100_00, meta, "", Hoje));
        Assert.Null(excecao);
    }

    // ── SF-004: editar transação de meta não infla a meta (T-004, 004b, 004c) ──

    [Fact]
    public void T004_EditarAplicacaoComMesmoValor_NaoInflaMeta()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        var meta = CriarMeta(alvo: 1000_00);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 100_00, meta, "", Hoje);
        var id = _app.TransacaoRepo.ListarTodas().Single().Id;
        var metaDaUi = _app.MetaRepo.BuscarPorId(meta.Id); // estado carregado antes da edição

        _app.Transacoes.Atualizar(id, TipoTransacao.Saida, Categoria.AdicionarMeta,
            100_00, metaDaUi, "", Hoje);

        Assert.Equal(100_00, _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos);
        Assert.Equal(900_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
    }

    [Fact]
    public void T004b_EditarAplicacaoParaValorMaior_AtualizaCorretamente()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        var meta = CriarMeta(alvo: 1000_00);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 100_00, meta, "", Hoje);
        var id = _app.TransacaoRepo.ListarTodas().Single().Id;
        var metaDaUi = _app.MetaRepo.BuscarPorId(meta.Id);

        _app.Transacoes.Atualizar(id, TipoTransacao.Saida, Categoria.AdicionarMeta,
            150_00, metaDaUi, "", Hoje);

        Assert.Equal(150_00, _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos);
        Assert.Equal(850_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
    }

    [Fact]
    public void T004c_EditarResgate_AtualizaCorretamente()
    {
        var meta = CriarMeta(alvo: 1000_00, atual: 300_00);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 100_00, meta, "", Hoje);
        var id = _app.TransacaoRepo.ListarTodas().Single().Id;
        var metaDaUi = _app.MetaRepo.BuscarPorId(meta.Id);

        _app.Transacoes.Atualizar(id, TipoTransacao.Entrada, Categoria.RetirarMeta,
            50_00, metaDaUi, "", Hoje);

        Assert.Equal(250_00, _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos);
    }

    // ── SF-005: exclusão/edição de entrada não deixa carteira negativa (T-005) ──

    [Fact]
    public void T005a_ExcluirEntradaUsadaPorSaida_EhBloqueado()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 0);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 1000_00, null, "", Hoje);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Aluguel, 900_00, null, "", Hoje);
        var salario = _app.TransacaoRepo.ListarTodas().First(t => t.Categoria == Categoria.Salario).Id;

        var excecao = Assert.Throws<ArgumentException>(() => _app.Transacoes.ExcluirTransacao(salario));

        Assert.Contains("saldo negativo", excecao.Message);
        Assert.Equal(2, _app.TransacaoRepo.ListarTodas().Count); // rollback: nada excluído
        Assert.Equal(100_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
    }

    [Fact]
    public void T005b_ReduzirEntradaAbaixoDoGasto_EhBloqueado()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 0);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 1000_00, null, "", Hoje);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Aluguel, 900_00, null, "", Hoje);
        var salario = _app.TransacaoRepo.ListarTodas().First(t => t.Categoria == Categoria.Salario).Id;

        Assert.Throws<ArgumentException>(() =>
            _app.Transacoes.Atualizar(salario, TipoTransacao.Entrada, Categoria.Salario, 500_00, null, "", Hoje));

        Assert.Equal(1000_00, _app.TransacaoRepo.BuscarPorId(salario)!.ValorCentavos);
        Assert.Equal(100_00, _app.Carteiras.CalcularSaldoCarteiraCentavos(Database.CarteiraBancoId));
    }

    [Fact]
    public void T005c_MoverEntradaParaCarteiraSemFundos_EhBloqueado()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 0);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 1000_00, null, "", Hoje);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Aluguel, 900_00, null, "", Hoje);
        var salario = _app.TransacaoRepo.ListarTodas().First(t => t.Categoria == Categoria.Salario).Id;

        Assert.Throws<ArgumentException>(() =>
            _app.Transacoes.Atualizar(salario, TipoTransacao.Entrada, Categoria.Salario, 1000_00, null, "", Hoje,
                Database.CarteiraDinheiroFisicoId));

        Assert.Equal(Database.CarteiraBancoId, _app.TransacaoRepo.BuscarPorId(salario)!.CarteiraId);
    }

    // ── SF-006: excluir aplicação após resgate cria dinheiro (T-006, D-05) ──

    [Fact]
    public void T006_ExcluirAplicacaoAposResgate_EhBloqueadoETotalSePreserva()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);
        var meta = CriarMeta(alvo: 1000_00);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 100_00, meta, "", Hoje);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 100_00, meta, "", Hoje);
        var aplicacao = _app.TransacaoRepo.ListarTodas().First(t => t.Categoria == Categoria.AdicionarMeta).Id;

        long totalAntes = _app.Carteiras.CalcularSaldoTotalCentavos() + _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos;

        var excecao = Assert.Throws<ArgumentException>(() => _app.Transacoes.ExcluirTransacao(aplicacao));

        Assert.Contains("já foi resgatado", excecao.Message);
        long totalDepois = _app.Carteiras.CalcularSaldoTotalCentavos() + _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos;
        Assert.Equal(totalAntes, totalDepois);
    }

    [Fact]
    public void T006b_ExcluirAplicacaoSemResgates_Permite()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 100_00);
        var meta = CriarMeta(alvo: 1000_00);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 100_00, meta, "", Hoje);
        var aplicacao = _app.TransacaoRepo.ListarTodas().Single().Id;

        _app.Transacoes.ExcluirTransacao(aplicacao);

        Assert.Equal(100_00, _app.Carteiras.CalcularSaldoTotalCentavos());
    }

    // ── SF-007: excluir meta com saldo (T-007, D-01) ──────────────────────

    [Fact]
    public void T007_ExcluirMetaComValorAcumulado_EhBloqueadoESaldoTotalSePreserva()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        _app.Metas.CriarMeta("Viagem", 500_00);
        var meta = _app.Metas.ListarTodasMetas().Single();
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 200_00, meta, "", Hoje);

        long totalAntes = _app.Carteiras.CalcularSaldoTotalCentavos() + _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos;

        var excecao = Assert.Throws<ArgumentException>(() => _app.Metas.DeletarMeta(meta.Id));

        Assert.Contains("acumulados", excecao.Message);
        long totalDepois = _app.Carteiras.CalcularSaldoTotalCentavos() + _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos;
        Assert.Equal(totalAntes, totalDepois);
    }

    // ── SF-008: totais do mês ignoram movimentos de meta (T-008, D-02) ────

    [Fact]
    public void T008_DespesasDoMes_IgnoramAplicacaoEmMeta()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Alimentacao, 50_00, null, "", Hoje);
        _app.Metas.CriarMeta("Viagem", 1000_00);
        var meta = _app.Metas.ListarTodasMetas().Single();
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 200_00, meta, "", Hoje);

        Assert.Equal(50_00, _app.Transacoes.CalcularDespesasMes(new DateOnly(2026, 11, 1)));
    }

    [Fact]
    public void T008b_ReceitasDoMes_IgnoramResgateDeMeta()
    {
        _app.Metas.CriarMeta("Viagem", 1000_00);
        var meta = _app.Metas.ListarTodasMetas().Single();
        meta.Adicionar(300_00);
        _app.MetaRepo.Atualizar(meta);
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 0);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 100_00, meta, "", Hoje);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.Salario, 500_00, null, "", Hoje);

        Assert.Equal(500_00, _app.Transacoes.CalcularReceitasMes(new DateOnly(2026, 11, 1)));
    }

    // ── SF-011: banco legado com GUIDs minúsculos com letras ──────────────

    [Fact]
    public void T011_BancoLegadoComGuidMinusculo_OperacoesPorIdFuncionam()
    {
        string dir = NovoDir();
        try
        {
            string caminho = Path.Combine(dir, "legado.db");
            CriarBancoLegado(caminho, idTransacao: "aaaaaaaa-1111-2222-3333-444444444444",
                idMeta: "bbbbbbbb-1111-2222-3333-444444444444", comMeta: true);

            using var db = new Database(caminho);
            db.Initialize();
            var repo = new TransacaoRepository(db);
            var metaRepo = new MetaRepository(db);
            var carteiraRepo = new CarteiraRepository(db);
            var carteiras = new CarteiraService(carteiraRepo, repo);
            var servico = new TransacaoService(db, repo, metaRepo, carteiras);

            var idTransacao = new Guid("aaaaaaaa-1111-2222-3333-444444444444");
            var idMeta = new Guid("bbbbbbbb-1111-2222-3333-444444444444");

            // editar a transação legada (por Guid) deve afetar exatamente 1 linha
            servico.Atualizar(idTransacao, TipoTransacao.Saida, Categoria.Alimentacao, 55_00, null, "legado", Hoje);
            Assert.Equal(55_00, repo.BuscarPorId(idTransacao)!.ValorCentavos);

            // aplicar em meta legada: FK com meta_id minúsculo não pode falhar
            carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
            var metaLegada = metaRepo.BuscarPorId(idMeta)!;
            servico.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 10_00, metaLegada, "", Hoje);
            Assert.Equal(110_00, metaRepo.BuscarPorId(idMeta)!.AtualCentavos);

            // excluir a transação legada
            servico.ExcluirTransacao(idTransacao);
            Assert.Null(repo.BuscarPorId(idTransacao));
        }
        finally
        {
            TryDelete(dir);
        }
    }

    // ── SF-035: strings de enum do banco Java legado ──────────────────────

    [Fact]
    public void Sf035_BancoLegado_TodosOsEnumsCarregam()
    {
        string dir = NovoDir();
        try
        {
            string caminho = Path.Combine(dir, "enums.db");
            using (var conn = new SqliteConnection($"Data Source={caminho};Pooling=false"))
            {
                conn.Open();
                Executar(conn, "CREATE TABLE carteira (id TEXT PRIMARY KEY, nome TEXT NOT NULL UNIQUE, saldo_inicial_centavos INTEGER NOT NULL DEFAULT 0)");
                Executar(conn, "INSERT INTO carteira (id, nome) VALUES ('00000000-0000-0000-0000-000000000001', 'Banco')");
                Executar(conn, "CREATE TABLE transacao (id TEXT PRIMARY KEY, descricao TEXT NOT NULL, valor_centavos INTEGER NOT NULL, tipo TEXT NOT NULL, data TEXT NOT NULL, meta_id TEXT, categoria TEXT)");
                Executar(conn, "INSERT INTO transacao (id, descricao, valor_centavos, tipo, data, categoria) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'A', 100, 'Saida', '2026-01-15', 'Alimentacao')");
                Executar(conn, "INSERT INTO transacao (id, descricao, valor_centavos, tipo, data, categoria) VALUES ('aaaaaaaa-0000-0000-0000-000000000002', 'B', 100, 'Entrada', '2026-01-15', 'RetirarMeta')");
                Executar(conn, "INSERT INTO transacao (id, descricao, valor_centavos, tipo, data, categoria) VALUES ('aaaaaaaa-0000-0000-0000-000000000003', 'C', 100, 'Saida', '2026-01-15', 'AdicionarMeta')");
            }

            using var db = new Database(caminho);
            db.Initialize();
            var repo = new TransacaoRepository(db);

            var todas = repo.ListarTodas();
            Assert.Equal(3, todas.Count);
            Assert.Contains(todas, t => t.Categoria == Categoria.RetirarMeta && t.Tipo == TipoTransacao.Entrada);
            Assert.Contains(todas, t => t.Categoria == Categoria.AdicionarMeta && t.Tipo == TipoTransacao.Saida);
            Assert.Contains(todas, t => t.Categoria == Categoria.Alimentacao);
        }
        finally
        {
            TryDelete(dir);
        }
    }

    // ── SF-028: reconciliação (T-R, invariante I1 simplificado) ───────────

    [Fact]
    public void TR_CenarioMisto_ConservaODinheiroTotal()
    {
        _app.Carteiras.AtualizarSaldoInicial(Database.CarteiraBancoId, 1000_00);
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.Alimentacao, 50_00, null, "", Hoje);
        _app.Metas.CriarMeta("Viagem", 1000_00);
        var meta = _app.Metas.ListarTodasMetas().Single();
        _app.Transacoes.Registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 200_00, meta, "", Hoje);
        _app.Transacoes.Registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 50_00, meta, "", Hoje);
        _app.Carteiras.Transferir(Database.CarteiraBancoId, Database.CarteiraDinheiroFisicoId, 100_00, Hoje, "saque");

        // I1: Σ carteiras + Σ metas = saldos iniciais + entradas externas − saídas externas
        long ladoEsquerdo = _app.Carteiras.CalcularSaldoTotalCentavos()
                            + _app.MetaRepo.BuscarPorId(meta.Id)!.AtualCentavos;
        long ladoDireito = 1000_00 - 50_00;

        Assert.Equal(ladoDireito, ladoEsquerdo);
    }

    // ── apoio ─────────────────────────────────────────────────────────────

    private static void CriarBancoLegado(string caminho, string idTransacao,
        string? idMeta, bool comMeta, string tipo = "Saida", string? categoria = "Alimentacao")
    {
            using var conn = new SqliteConnection($"Data Source={caminho};Pooling=false");
            conn.Open();
            Executar(conn, "CREATE TABLE carteira (id TEXT PRIMARY KEY, nome TEXT NOT NULL UNIQUE, saldo_inicial_centavos INTEGER NOT NULL DEFAULT 0)");
            Executar(conn, "INSERT INTO carteira (id, nome, saldo_inicial_centavos) VALUES ('00000000-0000-0000-0000-000000000001', 'Banco', 50000)");
            if (comMeta)
            {
            Executar(conn, "CREATE TABLE meta (id TEXT PRIMARY KEY, nome TEXT NOT NULL, alvo_centavos INTEGER, atual_centavos INTEGER NOT NULL DEFAULT 0)");
            Executar(conn, $"INSERT INTO meta (id, nome, alvo_centavos, atual_centavos) VALUES ('{idMeta}', 'Legada', 100000, 10000)");
            Executar(conn, $"CREATE TABLE transacao (id TEXT PRIMARY KEY, descricao TEXT NOT NULL, valor_centavos INTEGER NOT NULL, tipo TEXT NOT NULL, data TEXT NOT NULL, meta_id TEXT REFERENCES meta(id) ON DELETE SET NULL, categoria TEXT)");
            Executar(conn, $"INSERT INTO transacao (id, descricao, valor_centavos, tipo, data, meta_id, categoria) VALUES ('{idTransacao}', 'Legada', 10000, '{tipo}', '2026-01-15', '{idMeta}', '{categoria}')");
        }
        else
        {
            Executar(conn, "CREATE TABLE transacao (id TEXT PRIMARY KEY, descricao TEXT NOT NULL, valor_centavos INTEGER NOT NULL, tipo TEXT NOT NULL, data TEXT NOT NULL, meta_id TEXT)");
            Executar(conn, $"INSERT INTO transacao (id, descricao, valor_centavos, tipo, data, categoria) VALUES ('{idTransacao}', 'Legada', 10000, '{tipo}', '2026-01-15', '{categoria}')");
        }
    }

    private Meta CriarMeta(long alvo, long atual = 0)
    {
        var meta = new Meta(Guid.NewGuid(), "Legada", alvo, atual);
        _app.MetaRepo.Inserir(meta);
        return meta;
    }

    private static string NovoDir()
    {
        string dir = Path.Combine(Path.GetTempPath(), "sf-audext-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dir);
        return dir;
    }

    private static void Executar(SqliteConnection conn, string sql)
    {
        using var comando = conn.CreateCommand();
        comando.CommandText = sql;
        comando.ExecuteNonQuery();
    }

    private static void TryDelete(string dir)
    {
        try
        {
            Directory.Delete(dir, recursive: true);
        }
        catch (IOException)
        {
        }
    }

    public void Dispose() => _app.Dispose();
}
