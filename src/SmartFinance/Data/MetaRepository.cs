using Microsoft.Data.Sqlite;
using SmartFinance.Models;

namespace SmartFinance.Data;

public sealed class MetaRepository(Database db) : RepositoryBase(db)
{
    public void Inserir(Meta meta)
    {
        using var comando = Comando("INSERT INTO meta (id, nome, alvo_centavos, atual_centavos) VALUES ($id, $nome, $alvo, $atual)");
        comando.Parameters.AddWithValue("$id", meta.Id);
        comando.Parameters.AddWithValue("$nome", meta.Nome);
        comando.Parameters.AddWithValue("$alvo", (object?)meta.AlvoCentavos ?? DBNull.Value);
        comando.Parameters.AddWithValue("$atual", meta.AtualCentavos);
        comando.ExecuteNonQuery();
    }

    public void Atualizar(Meta meta)
    {
        using var comando = Comando("UPDATE meta SET nome = $nome, alvo_centavos = $alvo, atual_centavos = $atual WHERE id = $id");
        comando.Parameters.AddWithValue("$nome", meta.Nome);
        comando.Parameters.AddWithValue("$alvo", (object?)meta.AlvoCentavos ?? DBNull.Value);
        comando.Parameters.AddWithValue("$atual", meta.AtualCentavos);
        comando.Parameters.AddWithValue("$id", meta.Id);
        comando.ExecuteNonQuery();
    }

    public void Deletar(Guid id)
    {
        using var comando = Comando("DELETE FROM meta WHERE id = $id");
        comando.Parameters.AddWithValue("$id", id);
        comando.ExecuteNonQuery();
    }

    public Meta? BuscarPorId(Guid id)
    {
        using var comando = Comando("SELECT id, nome, alvo_centavos, atual_centavos FROM meta WHERE id = $id");
        comando.Parameters.AddWithValue("$id", id);
        using var reader = comando.ExecuteReader();
        return reader.Read() ? LerMeta(reader) : null;
    }

    public List<Meta> ListarTodas()
    {
        var lista = new List<Meta>();
        using var comando = Comando("SELECT id, nome, alvo_centavos, atual_centavos FROM meta ORDER BY nome COLLATE NOCASE");
        using var reader = comando.ExecuteReader();
        while (reader.Read())
        {
            lista.Add(LerMeta(reader));
        }

        return lista;
    }

    private static Meta LerMeta(SqliteDataReader reader)
    {
        long? alvo = reader.IsDBNull(2) ? null : reader.GetInt64(2);
        return new Meta(reader.GetGuid(0), reader.GetString(1), alvo, reader.GetInt64(3));
    }
}
