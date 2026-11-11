using Microsoft.Data.Sqlite;
using SmartFinance.Models;

namespace SmartFinance.Data;

public sealed class EmprestimoRepository(Database db) : RepositoryBase(db)
{
    private const string Colunas =
        "id, tipo, nome, valor_centavos, data_pagamento, carteira_id, transacao_id";

    public void Inserir(Emprestimo emprestimo)
    {
        using var comando = Comando("""
            INSERT INTO emprestimo (id, tipo, nome, valor_centavos, data_pagamento, carteira_id, transacao_id)
            VALUES ($id, $tipo, $nome, $valor, $dataPagamento, $carteiraId, $transacaoId)
            """);
        PreencherParametros(comando, emprestimo);
        comando.ExecuteNonQuery();
    }

    public void Atualizar(Emprestimo emprestimo)
    {
        using var comando = Comando("""
            UPDATE emprestimo
            SET tipo = $tipo, nome = $nome, valor_centavos = $valor, data_pagamento = $dataPagamento,
                carteira_id = $carteiraId, transacao_id = $transacaoId
            WHERE id = $id
            """);
        PreencherParametros(comando, emprestimo);
        comando.ExecuteNonQuery();
    }

    public void DefinirTransacaoVinculada(Guid id, Guid? transacaoId)
    {
        using var comando = Comando("UPDATE emprestimo SET transacao_id = $transacaoId WHERE id = $id");
        comando.Parameters.AddWithValue("$transacaoId", (object?)transacaoId?.ToString() ?? DBNull.Value);
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.ExecuteNonQuery();
    }

    public void Deletar(Guid id)
    {
        using var comando = Comando("DELETE FROM emprestimo WHERE id = $id");
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.ExecuteNonQuery();
    }

    public Emprestimo? BuscarPorId(Guid id)
    {
        using var comando = Comando($"SELECT {Colunas} FROM emprestimo WHERE id = $id");
        comando.Parameters.AddWithValue("$id", id.ToString());
        using var reader = comando.ExecuteReader();
        return reader.Read() ? Ler(reader) : null;
    }

    public List<Emprestimo> ListarTodas()
    {
        var lista = new List<Emprestimo>();
        using var comando = Comando($"SELECT {Colunas} FROM emprestimo ORDER BY data_pagamento");
        using var reader = comando.ExecuteReader();
        while (reader.Read())
        {
            lista.Add(Ler(reader));
        }

        return lista;
    }

    public List<Emprestimo> ListarPorTipo(TipoEmprestimo tipo)
    {
        var lista = new List<Emprestimo>();
        using var comando = Comando($"SELECT {Colunas} FROM emprestimo WHERE tipo = $tipo ORDER BY data_pagamento");
        comando.Parameters.AddWithValue("$tipo", tipo.ToString());
        using var reader = comando.ExecuteReader();
        while (reader.Read())
        {
            lista.Add(Ler(reader));
        }

        return lista;
    }

    private static void PreencherParametros(SqliteCommand comando, Emprestimo emprestimo)
    {
        comando.Parameters.AddWithValue("$id", emprestimo.Id.ToString());
        comando.Parameters.AddWithValue("$tipo", emprestimo.Tipo.ToString());
        comando.Parameters.AddWithValue("$nome", emprestimo.Nome);
        comando.Parameters.AddWithValue("$valor", emprestimo.ValorCentavos);
        comando.Parameters.AddWithValue("$dataPagamento", emprestimo.DataPagamento);
        comando.Parameters.AddWithValue("$carteiraId", emprestimo.CarteiraId?.ToString() ?? Database.CarteiraBancoId.ToString());
        comando.Parameters.AddWithValue("$transacaoId", (object?)emprestimo.TransacaoId?.ToString() ?? DBNull.Value);
    }

    private static Emprestimo Ler(SqliteDataReader reader)
    {
        return new Emprestimo(
            reader.GetGuid(0),
            Enum.Parse<TipoEmprestimo>(reader.GetString(1)),
            reader.GetString(2),
            reader.GetInt64(3),
            reader.GetFieldValue<DateOnly>(4),
            reader.IsDBNull(5) ? Database.CarteiraBancoId : reader.GetGuid(5),
            reader.IsDBNull(6) ? null : reader.GetGuid(6));
    }
}
