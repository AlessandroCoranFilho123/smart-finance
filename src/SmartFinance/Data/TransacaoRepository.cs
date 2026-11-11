using System.Globalization;
using System.Text;
using Microsoft.Data.Sqlite;
using SmartFinance.Models;

namespace SmartFinance.Data;

/// <summary>Filtros combináveis da tela de transações (busca textual + tipo/categoria/período).</summary>
public sealed record TransacaoFiltro(
    string Busca,
    TipoTransacao? Tipo,
    Categoria? Categoria,
    DateOnly? DataInicio,
    DateOnly? DataFim)
{
    public static readonly TransacaoFiltro Vazio = new("", null, null, null, null);

    public bool PossuiBusca => !string.IsNullOrWhiteSpace(Busca);
}

public sealed class TransacaoRepository(Database db) : RepositoryBase(db)
{
    public void Inserir(Transacao transacao)
    {
        using var comando = Comando("""
            INSERT INTO transacao (id, descricao, comentario, valor_centavos, tipo, data, meta_id, categoria, carteira_id)
            VALUES ($id, $descricao, $comentario, $valor, $tipo, $data, $metaId, $categoria, $carteiraId)
            """);

        comando.Parameters.AddWithValue("$id", transacao.Id.ToString());
        comando.Parameters.AddWithValue("$descricao", transacao.Descricao);
        comando.Parameters.AddWithValue("$comentario", transacao.Comentario ?? string.Empty);
        comando.Parameters.AddWithValue("$valor", transacao.ValorCentavos);
        comando.Parameters.AddWithValue("$tipo", transacao.Tipo.ToString());
        comando.Parameters.AddWithValue("$data", transacao.Data);
        comando.Parameters.AddWithValue("$metaId", (object?)transacao.MetaId?.ToString() ?? DBNull.Value);
        comando.Parameters.AddWithValue("$categoria", (object?)transacao.Categoria?.ToString() ?? DBNull.Value);
        comando.Parameters.AddWithValue("$carteiraId", (object?)transacao.CarteiraId?.ToString() ?? Database.CarteiraBancoId.ToString());
        comando.ExecuteNonQuery();
    }

    public void Excluir(Guid id)
    {
        using var comando = Comando("DELETE FROM transacao WHERE id = $id");
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.ExecuteNonQuery();
    }

    public Transacao? BuscarPorId(Guid id)
    {
        using var comando = ComandoSelecaoTransacoes(Db, "WHERE id = $id", "LIMIT 1");
        comando.Parameters.AddWithValue("$id", id.ToString());
        using var reader = comando.ExecuteReader();
        return reader.Read() ? LerTransacao(reader) : null;
    }

    public List<Transacao> ListarRecentes(int limite)
    {
        using var comando = ComandoSelecaoTransacoes(Db, string.Empty, "ORDER BY data DESC, rowid DESC LIMIT $limite");
        comando.Parameters.AddWithValue("$limite", limite);
        return ExecutarSelecao(comando);
    }

    public List<Transacao> ListarTodas()
    {
        using var comando = ComandoSelecaoTransacoes(Db, string.Empty, "ORDER BY data DESC, rowid DESC");
        return ExecutarSelecao(comando);
    }

    public List<Transacao> ListarPorFiltro(TransacaoFiltro filtro)
    {
        filtro ??= TransacaoFiltro.Vazio;
        // Se a busca não contiver nenhum termo utilizável (ex.: só símbolos),
        // ignora o filtro textual em vez de enviar um MATCH vazio ao FTS5.
        string consultaFts = filtro.PossuiBusca ? CriarConsultaFts(filtro.Busca) : string.Empty;
        bool filtrarTexto = consultaFts.Length > 0;

        var parametros = new List<(string Nome, object Valor)>();
        var condicoes = new List<string>();

        string sql = filtrarTexto
            ? "SELECT t.id, t.descricao, t.comentario, t.valor_centavos, t.tipo, t.data, t.meta_id, t.categoria, t.carteira_id FROM transacao t JOIN transacao_fts ON transacao_fts.rowid = t.rowid"
            : "SELECT t.id, t.descricao, t.comentario, t.valor_centavos, t.tipo, t.data, t.meta_id, t.categoria, t.carteira_id FROM transacao t";

        if (filtrarTexto)
        {
            condicoes.Add("transacao_fts MATCH $busca");
            parametros.Add(("$busca", consultaFts));
        }

        if (filtro.Tipo is not null)
        {
            condicoes.Add("t.tipo = $tipo");
            parametros.Add(("$tipo", filtro.Tipo.Value.ToString()));
        }

        if (filtro.Categoria is not null)
        {
            condicoes.Add("t.categoria = $categoria");
            parametros.Add(("$categoria", filtro.Categoria.Value.ToString()));
        }

        if (filtro.DataInicio is not null)
        {
            condicoes.Add("t.data >= $dataInicio");
            parametros.Add(("$dataInicio", filtro.DataInicio.Value));
        }

        if (filtro.DataFim is not null)
        {
            condicoes.Add("t.data <= $dataFim");
            parametros.Add(("$dataFim", filtro.DataFim.Value));
        }

        if (condicoes.Count > 0)
        {
            sql += " WHERE " + string.Join(" AND ", condicoes);
        }

        sql += " ORDER BY t.data DESC, t.rowid DESC";

        using var comando = Db.Connection.CreateCommand();
        comando.CommandText = sql;
        foreach (var (nome, valor) in parametros)
        {
            comando.Parameters.AddWithValue(nome, valor);
        }

        return ExecutarSelecao(comando);
    }

    public long CalcularSaldoPorCarteira(Guid carteiraId)
    {
        var carteiraEfetiva = carteiraId == Guid.Empty ? Database.CarteiraBancoId : carteiraId;
        using var comando = Comando("""
            SELECT COALESCE(SUM(CASE tipo WHEN 'Entrada' THEN valor_centavos ELSE -valor_centavos END), 0)
            FROM transacao
            WHERE COALESCE(carteira_id, '00000000-0000-0000-0000-000000000001') = $carteiraId
            """);
        comando.Parameters.AddWithValue("$carteiraId", carteiraEfetiva.ToString());
        return (long)(comando.ExecuteScalar() ?? 0L);
    }

    public long CalcularTotalPorTipoEMes(TipoTransacao tipo, DateOnly primeiroDiaDoMes)
    {
        // Movimentos de meta (aplicar/resgatar) não entram nos totais do mês:
        // não são renda nem consumo, apenas mudam dinheiro de lugar (decisão D-02).
        DateOnly fim = primeiroDiaDoMes.AddMonths(1);
        using var comando = Comando("""
            SELECT SUM(valor_centavos) FROM transacao
            WHERE tipo = $tipo AND data >= $inicio AND data < $fim
              AND (categoria IS NULL OR categoria NOT IN ('AdicionarMeta', 'RetirarMeta'))
            """);
        comando.Parameters.AddWithValue("$tipo", tipo.ToString());
        comando.Parameters.AddWithValue("$inicio", primeiroDiaDoMes);
        comando.Parameters.AddWithValue("$fim", fim);
        return comando.ExecuteScalar() is long total ? total : 0L;
    }

    private List<Transacao> ExecutarSelecao(SqliteCommand comando)
    {
        var lista = new List<Transacao>();
        using var reader = comando.ExecuteReader();
        while (reader.Read())
        {
            lista.Add(LerTransacao(reader));
        }

        return lista;
    }

    /// <summary>
    /// Monta a consulta FTS5: termos sem acento, prefixo e conectados por AND
    /// (mesma semântica da versão Java, para não quebrar a busca existente).
    /// </summary>
    private static string CriarConsultaFts(string busca)
    {
        string semAcentos = RemoverAcentos(busca);
        var partes = new List<string>();
        var termo = new StringBuilder();

        foreach (char c in semAcentos)
        {
            if (char.IsLetterOrDigit(c))
            {
                termo.Append(c);
            }
            else if (termo.Length > 0)
            {
                partes.Add(termo.ToString() + "*");
                termo.Clear();
            }
        }

        if (termo.Length > 0)
        {
            partes.Add(termo.ToString() + "*");
        }

        return string.Join(" AND ", partes);
    }

    private static string RemoverAcentos(string texto)
    {
        string normalizado = texto.Normalize(NormalizationForm.FormD);
        var builder = new StringBuilder(normalizado.Length);
        foreach (char c in normalizado)
        {
            if (CharUnicodeInfo.GetUnicodeCategory(c) != UnicodeCategory.NonSpacingMark)
            {
                builder.Append(char.ToLowerInvariant(c));
            }
        }

        return builder.ToString();
    }
}
