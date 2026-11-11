using Microsoft.Data.Sqlite;
using SmartFinance.Models;

namespace SmartFinance.Data;

public sealed class CartaoCreditoRepository(Database db) : RepositoryBase(db)
{
    public void Inserir(CompraCartaoCredito compra)
    {
        using var comando = Comando("""
            INSERT INTO compra_cartao_credito (id, nome, valor_total_centavos, parcelas, vencimento)
            VALUES ($id, $nome, $valorTotal, $parcelas, $vencimento)
            """);
        comando.Parameters.AddWithValue("$id", compra.Id.ToString());
        comando.Parameters.AddWithValue("$nome", compra.Nome);
        comando.Parameters.AddWithValue("$valorTotal", compra.ValorTotalCentavos);
        comando.Parameters.AddWithValue("$parcelas", compra.Parcelas);
        comando.Parameters.AddWithValue("$vencimento", compra.Vencimento);
        comando.ExecuteNonQuery();
    }

    public void Atualizar(CompraCartaoCredito compra)
    {
        using var comando = Comando("""
            UPDATE compra_cartao_credito
            SET nome = $nome, valor_total_centavos = $valorTotal, parcelas = $parcelas, vencimento = $vencimento
            WHERE id = $id
            """);
        comando.Parameters.AddWithValue("$nome", compra.Nome);
        comando.Parameters.AddWithValue("$valorTotal", compra.ValorTotalCentavos);
        comando.Parameters.AddWithValue("$parcelas", compra.Parcelas);
        comando.Parameters.AddWithValue("$vencimento", compra.Vencimento);
        comando.Parameters.AddWithValue("$id", compra.Id.ToString());
        comando.ExecuteNonQuery();
    }

    public void Deletar(Guid id)
    {
        using var comando = Comando("DELETE FROM compra_cartao_credito WHERE id = $id");
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.ExecuteNonQuery();
    }

    public List<CompraCartaoCredito> ListarTodas()
    {
        var lista = new List<CompraCartaoCredito>();
        using var comando = Comando("SELECT id, nome, valor_total_centavos, parcelas, vencimento FROM compra_cartao_credito ORDER BY vencimento");
        using var reader = comando.ExecuteReader();
        while (reader.Read())
        {
            lista.Add(new CompraCartaoCredito(
                reader.GetGuid(0),
                reader.GetString(1),
                reader.GetInt64(2),
                reader.GetInt32(3),
                LerData(reader, 4)));
        }

        return lista;
    }
}
