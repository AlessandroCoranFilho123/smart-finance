using Microsoft.Data.Sqlite;
using SmartFinance.Models;

namespace SmartFinance.Data;

public sealed class CarteiraRepository(Database db) : RepositoryBase(db)
{
    public List<Carteira> ListarTodas()
    {
        var lista = new List<Carteira>();
        using var comando = Comando("SELECT id, nome, saldo_inicial_centavos FROM carteira ORDER BY nome COLLATE NOCASE");
        using var reader = comando.ExecuteReader();
        while (reader.Read())
        {
            lista.Add(new Carteira(reader.GetGuid(0), reader.GetString(1), reader.GetInt64(2)));
        }

        return lista;
    }

    public Carteira? BuscarPorId(Guid id)
    {
        using var comando = Comando("SELECT id, nome, saldo_inicial_centavos FROM carteira WHERE id = $id");
        comando.Parameters.AddWithValue("$id", id.ToString());
        using var reader = comando.ExecuteReader();
        return reader.Read() ? new Carteira(reader.GetGuid(0), reader.GetString(1), reader.GetInt64(2)) : null;
    }

    public void AtualizarSaldoInicial(Guid id, long saldoInicialCentavos)
    {
        using var comando = Comando("UPDATE carteira SET saldo_inicial_centavos = $saldo WHERE id = $id");
        comando.Parameters.AddWithValue("$saldo", saldoInicialCentavos);
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.ExecuteNonQuery();
    }

    public void InserirTransferencia(TransferenciaCarteira transferencia)
    {
        using var comando = Comando("""
            INSERT INTO transferencia_carteira (id, origem_carteira_id, destino_carteira_id, valor_centavos, data, comentario)
            VALUES ($id, $origem, $destino, $valor, $data, $comentario)
            """);
        comando.Parameters.AddWithValue("$id", transferencia.Id.ToString());
        comando.Parameters.AddWithValue("$origem", transferencia.OrigemCarteiraId.ToString());
        comando.Parameters.AddWithValue("$destino", transferencia.DestinoCarteiraId.ToString());
        comando.Parameters.AddWithValue("$valor", transferencia.ValorCentavos);
        comando.Parameters.AddWithValue("$data", transferencia.Data);
        comando.Parameters.AddWithValue("$comentario", transferencia.Comentario ?? string.Empty);
        comando.ExecuteNonQuery();
    }

    public long CalcularTransferenciasEntrada(Guid carteiraId)
    {
        using var comando = Comando("SELECT COALESCE(SUM(valor_centavos), 0) FROM transferencia_carteira WHERE destino_carteira_id = $id");
        comando.Parameters.AddWithValue("$id", carteiraId);
        return (long)(comando.ExecuteScalar() ?? 0L);
    }

    public long CalcularTransferenciasSaida(Guid carteiraId)
    {
        using var comando = Comando("SELECT COALESCE(SUM(valor_centavos), 0) FROM transferencia_carteira WHERE origem_carteira_id = $id");
        comando.Parameters.AddWithValue("$id", carteiraId);
        return (long)(comando.ExecuteScalar() ?? 0L);
    }
}
