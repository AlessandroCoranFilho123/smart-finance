using Microsoft.Data.Sqlite;
using SmartFinance.Models;

namespace SmartFinance.Data;

public abstract class RepositoryBase(Database db)
{
    protected Database Db { get; } = db;

    protected SqliteCommand Comando(string sql)
    {
        var comando = Db.Connection.CreateCommand();
        comando.CommandText = sql;
        return comando;
    }

    protected static DateOnly LerData(SqliteDataReader reader, int ordinal) => reader.GetFieldValue<DateOnly>(ordinal);

    protected static Transacao LerTransacao(SqliteDataReader reader)
    {
        return new Transacao(
            reader.GetGuid(0),
            reader.GetString(1),
            reader.IsDBNull(2) ? string.Empty : reader.GetString(2),
            reader.GetInt64(3),
            Enum.Parse<TipoTransacao>(reader.GetString(4)),
            LerData(reader, 5),
            reader.IsDBNull(6) ? null : reader.GetGuid(6),
            reader.IsDBNull(7) ? null : Enum.Parse<Categoria>(reader.GetString(7)),
            reader.IsDBNull(8) ? Database.CarteiraBancoId : reader.GetGuid(8));
    }

    private const string ColunasTransacao =
        "id, descricao, comentario, valor_centavos, tipo, data, meta_id, categoria, carteira_id";

    // SF-033: where/orderBy são montados apenas com constantes desta classe (nunca
    // entrada do usuário); valores dinâmicos vão sempre via SqliteParameter.
    protected static SqliteCommand ComandoSelecaoTransacoes(Database db, string where, string orderBy)
    {
        var comando = db.Connection.CreateCommand();
        comando.CommandText = $"SELECT {ColunasTransacao} FROM transacao {where} {orderBy}";
        return comando;
    }
}
