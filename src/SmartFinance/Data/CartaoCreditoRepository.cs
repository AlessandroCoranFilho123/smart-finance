using Microsoft.Data.Sqlite;
using SmartFinance.Models;

namespace SmartFinance.Data;

public sealed class CartaoCreditoRepository(Database db) : RepositoryBase(db)
{
    private const string Colunas =
        "id, nome, valor_total_centavos, parcelas, vencimento, parcelas_pagas, carteira_id, transacao_id";

    public void Inserir(CompraCartaoCredito compra)
    {
        using var comando = Comando("""
            INSERT INTO compra_cartao_credito (id, nome, valor_total_centavos, parcelas, vencimento, parcelas_pagas, carteira_id, transacao_id)
            VALUES ($id, $nome, $valorTotal, $parcelas, $vencimento, $pagas, $carteiraId, $transacaoId)
            """);
        PreencherParametros(comando, compra);
        comando.ExecuteNonQuery();
    }

    public void Atualizar(CompraCartaoCredito compra)
    {
        using var comando = Comando("""
            UPDATE compra_cartao_credito
            SET nome = $nome, valor_total_centavos = $valorTotal, parcelas = $parcelas,
                vencimento = $vencimento, parcelas_pagas = $pagas, carteira_id = $carteiraId, transacao_id = $transacaoId
            WHERE id = $id
            """);
        PreencherParametros(comando, compra);
        comando.ExecuteNonQuery();
    }

    public void DefinirTransacaoVinculada(Guid id, Guid? transacaoId)
    {
        using var comando = Comando("UPDATE compra_cartao_credito SET transacao_id = $transacaoId WHERE id = $id");
        comando.Parameters.AddWithValue("$transacaoId", (object?)transacaoId?.ToString() ?? DBNull.Value);
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.ExecuteNonQuery();
    }

    public void Deletar(Guid id)
    {
        using var comando = Comando("DELETE FROM compra_cartao_credito WHERE id = $id");
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.ExecuteNonQuery();
    }

    public CompraCartaoCredito? BuscarPorId(Guid id)
    {
        using var comando = Comando($"SELECT {Colunas} FROM compra_cartao_credito WHERE id = $id");
        comando.Parameters.AddWithValue("$id", id.ToString());
        using var reader = comando.ExecuteReader();
        return reader.Read() ? Ler(reader) : null;
    }

    public List<CompraCartaoCredito> ListarTodas()
    {
        var lista = new List<CompraCartaoCredito>();
        using var comando = Comando($"SELECT {Colunas} FROM compra_cartao_credito ORDER BY vencimento");
        using var reader = comando.ExecuteReader();
        while (reader.Read())
        {
            lista.Add(Ler(reader));
        }

        return lista;
    }

    private static void PreencherParametros(SqliteCommand comando, CompraCartaoCredito compra)
    {
        comando.Parameters.AddWithValue("$id", compra.Id.ToString());
        comando.Parameters.AddWithValue("$nome", compra.Nome);
        comando.Parameters.AddWithValue("$valorTotal", compra.ValorTotalCentavos);
        comando.Parameters.AddWithValue("$parcelas", compra.Parcelas);
        comando.Parameters.AddWithValue("$vencimento", compra.Vencimento);
        comando.Parameters.AddWithValue("$pagas", compra.ParcelasPagas);
        comando.Parameters.AddWithValue("$carteiraId", compra.CarteiraId?.ToString() ?? Database.CarteiraBancoId.ToString());
        comando.Parameters.AddWithValue("$transacaoId", (object?)compra.TransacaoId?.ToString() ?? DBNull.Value);
    }

    private static CompraCartaoCredito Ler(SqliteDataReader reader)
    {
        return new CompraCartaoCredito(
            reader.GetGuid(0),
            reader.GetString(1),
            reader.GetInt64(2),
            reader.GetInt32(3),
            reader.GetFieldValue<DateOnly>(4),
            reader.GetInt32(5),
            reader.IsDBNull(6) ? Database.CarteiraBancoId : reader.GetGuid(6),
            reader.IsDBNull(7) ? null : reader.GetGuid(7));
    }
}
