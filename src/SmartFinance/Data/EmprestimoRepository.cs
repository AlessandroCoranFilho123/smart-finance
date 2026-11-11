using Microsoft.Data.Sqlite;
using SmartFinance.Models;

namespace SmartFinance.Data;

public sealed class EmprestimoRepository(Database db) : RepositoryBase(db)
{
    public void Inserir(Emprestimo emprestimo)
    {
        using var comando = Comando("""
            INSERT INTO emprestimo (id, tipo, nome, valor_centavos, data_pagamento)
            VALUES ($id, $tipo, $nome, $valor, $dataPagamento)
            """);
        comando.Parameters.AddWithValue("$id", emprestimo.Id.ToString());
        comando.Parameters.AddWithValue("$tipo", emprestimo.Tipo.ToString());
        comando.Parameters.AddWithValue("$nome", emprestimo.Nome);
        comando.Parameters.AddWithValue("$valor", emprestimo.ValorCentavos);
        comando.Parameters.AddWithValue("$dataPagamento", emprestimo.DataPagamento);
        comando.ExecuteNonQuery();
    }

    public void Atualizar(Emprestimo emprestimo)
    {
        using var comando = Comando("""
            UPDATE emprestimo
            SET tipo = $tipo, nome = $nome, valor_centavos = $valor, data_pagamento = $dataPagamento
            WHERE id = $id
            """);
        comando.Parameters.AddWithValue("$tipo", emprestimo.Tipo.ToString());
        comando.Parameters.AddWithValue("$nome", emprestimo.Nome);
        comando.Parameters.AddWithValue("$valor", emprestimo.ValorCentavos);
        comando.Parameters.AddWithValue("$dataPagamento", emprestimo.DataPagamento);
        comando.Parameters.AddWithValue("$id", emprestimo.Id.ToString());
        comando.ExecuteNonQuery();
    }

    public void Deletar(Guid id)
    {
        using var comando = Comando("DELETE FROM emprestimo WHERE id = $id");
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.ExecuteNonQuery();
    }

    public List<Emprestimo> ListarTodas()
    {
        var lista = new List<Emprestimo>();
        using var comando = Comando("SELECT id, tipo, nome, valor_centavos, data_pagamento FROM emprestimo ORDER BY data_pagamento");
        using var reader = comando.ExecuteReader();
        while (reader.Read())
        {
            lista.Add(new Emprestimo(
                reader.GetGuid(0),
                Enum.Parse<TipoEmprestimo>(reader.GetString(1)),
                reader.GetString(2),
                reader.GetInt64(3),
                LerData(reader, 4)));
        }

        return lista;
    }

    public List<Emprestimo> ListarPorTipo(TipoEmprestimo tipo)
    {
        var lista = new List<Emprestimo>();
        using var comando = Comando("SELECT id, tipo, nome, valor_centavos, data_pagamento FROM emprestimo WHERE tipo = $tipo ORDER BY data_pagamento");
        comando.Parameters.AddWithValue("$tipo", tipo.ToString());
        using var reader = comando.ExecuteReader();
        while (reader.Read())
        {
            lista.Add(new Emprestimo(
                reader.GetGuid(0),
                Enum.Parse<TipoEmprestimo>(reader.GetString(1)),
                reader.GetString(2),
                reader.GetInt64(3),
                LerData(reader, 4)));
        }

        return lista;
    }
}
