package app.repository;

import app.database.Database;
import app.model.TipoTransacao;
import app.model.Transacao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransacaoDAO {

    public void inserir(Transacao transacao) {
        String sql = """
                    INSERT INTO transacao (id, descricao, valor_centavos, tipo, data, meta_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, transacao.getId().toString());
            ps.setString(2, transacao.getDescricao());
            ps.setLong(3, transacao.getValorCentavos());
            ps.setString(4, transacao.getTipo().name());
            ps.setString(5, transacao.getData().toString());

            if (transacao.getMetaId() != null) {
                ps.setString(6, transacao.getMetaId().toString());
            } else {
                ps.setNull(6, Types.VARCHAR);
            }

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir transação", e);
        }
    }

    public List<Transacao> listarRecentes(int limite) {
        List<Transacao> list = new ArrayList<>();

        String sql = """
                    SELECT * FROM transacao
                    ORDER BY data DESC
                    LIMIT ?
                """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, limite);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                UUID metaId = rs.getString("meta_id") == null
                        ? null
                        : UUID.fromString(rs.getString("meta_id"));

                list.add(new Transacao(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("descricao"),
                        rs.getLong("valor_centavos"),
                        TipoTransacao.valueOf(rs.getString("tipo")),
                        LocalDate.parse(rs.getString("data")),
                        metaId
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar transações", e);
        }

        return list;
    }

    public long calcularSaldo() {
        String sql = """
                    SELECT
                        SUM(
                            CASE tipo
                                WHEN 'Entrada' THEN valor_centavos
                                ELSE -valor_centavos
                            END
                        ) AS saldo
                    FROM transacao
                """;

        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            return rs.next() ? rs.getLong("saldo") : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular saldo", e);
        }
    }

    public Transacao buscarPorId(UUID id) {
        String sql = "SELECT * FROM transacao WHERE id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            UUID metaId = rs.getString("meta_id") == null
                    ? null
                    : UUID.fromString(rs.getString("meta_id"));

            return new Transacao(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("descricao"),
                    rs.getLong("valor_centavos"),
                    TipoTransacao.valueOf(rs.getString("tipo")),
                    LocalDate.parse(rs.getString("data")),
                    metaId
            );

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar transação", e);
        }
    }

    public void excluir(UUID id) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps =
                     c.prepareStatement("DELETE FROM transacao WHERE id = ?")) {

            ps.setString(1, id.toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir transação", e);
        }
    }

}
