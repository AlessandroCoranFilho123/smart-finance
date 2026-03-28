package app.repository;

import app.database.Database;
import app.model.Meta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MetaDAO {

    /**
     * Ponto de extensão para testes: subclasses podem injetar
     * uma Connection de banco em memória sem alterar o código de produção.
     */
    protected Connection getConn() throws SQLException {
        return Database.getConnection();
    }

    public void inserir(Meta meta) {
        String sql = """
                    INSERT INTO meta (id, nome, alvo_centavos, atual_centavos)
                    VALUES (?, ?, ?, ?)
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, meta.getIdAsString());
            ps.setString(2, meta.getNome());
            ps.setObject(3, meta.getAlvoCentavos());
            ps.setLong(4, meta.getAtualCentavos());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Meta> listarTodas() {
        List<Meta> metas = new ArrayList<>();
        String sql = "SELECT * FROM meta";

        try (Connection c = getConn();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Object alvoObj = rs.getObject("alvo_centavos");
                Long alvoCentavos = alvoObj == null ? null : ((Number) alvoObj).longValue();

                metas.add(new Meta(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("nome"),
                        alvoCentavos,
                        rs.getLong("atual_centavos")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return metas;
    }

    public void atualizar(Meta meta) {
        String sql = """
                    UPDATE meta
                    SET nome = ?, alvo_centavos = ?, atual_centavos = ?
                    WHERE id = ?
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, meta.getNome());
            ps.setObject(2, meta.getAlvoCentavos());
            ps.setLong(3, meta.getAtualCentavos());
            ps.setString(4, meta.getIdAsString());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deletar(UUID id) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM meta WHERE id = ?")) {

            ps.setString(1, id.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Meta buscarPorId(UUID id) {
        String sql = "SELECT * FROM meta WHERE id = ?";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;
            Object alvoObj = rs.getObject("alvo_centavos");
            Long alvoCentavos = alvoObj == null ? null : ((Number) alvoObj).longValue();
            return new Meta(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("nome"),
                    alvoCentavos,
                    rs.getLong("atual_centavos")
            );

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar meta", e);
        }
    }
}
