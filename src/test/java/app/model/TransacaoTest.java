package app.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transacao - record de domínio")
class TransacaoTest {

    @Test
    @DisplayName("UUID gerado quando id é null")
    void idNuloGeraUUID() {
        var t = new Transacao(null, "desc", "", 100_00L, TipoTransacao.Entrada, LocalDate.now(), null, Categoria.Salario);
        assertNotNull(t.id());
    }

    @Test
    @DisplayName("Comentário null normalizado para vazio")
    void comentarioNuloViraVazio() {
        var t = new Transacao(UUID.randomUUID(), "desc", null, 100_00L, TipoTransacao.Entrada, LocalDate.now(), null, Categoria.Salario);
        assertEquals("", t.comentario());
    }

    @Test
    @DisplayName("Campos acessíveis corretamente")
    void accessors() {
        UUID id = UUID.randomUUID();
        LocalDate data = LocalDate.of(2024, 6, 15);
        var t = new Transacao(id, "Aluguel", "nota", 1500_00L, TipoTransacao.Saida, data, null, Categoria.Aluguel);
        assertAll(
                () -> assertEquals(id, t.id()),
                () -> assertEquals("Aluguel", t.descricao()),
                () -> assertEquals("nota", t.comentario()),
                () -> assertEquals(1500_00L, t.valorCentavos()),
                () -> assertEquals(TipoTransacao.Saida, t.tipo()),
                () -> assertEquals(data, t.data()),
                () -> assertNull(t.metaId()),
                () -> assertEquals(Categoria.Aluguel, t.categoria())
        );
    }

    @Test
    @DisplayName("Record equality — mesmos campos = iguais")
    void recordEquality() {
        UUID id = UUID.randomUUID();
        LocalDate data = LocalDate.now();
        var t1 = new Transacao(id, "S", "", 5000_00L, TipoTransacao.Entrada, data, null, Categoria.Salario);
        var t2 = new Transacao(id, "S", "", 5000_00L, TipoTransacao.Entrada, data, null, Categoria.Salario);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    @DisplayName("Transações com IDs distintos são diferentes")
    void recordDesigualdade() {
        LocalDate data = LocalDate.now();
        var t1 = new Transacao(UUID.randomUUID(), "S", "", 100_00L, TipoTransacao.Entrada, data, null, Categoria.Salario);
        var t2 = new Transacao(UUID.randomUUID(), "S", "", 100_00L, TipoTransacao.Entrada, data, null, Categoria.Salario);
        assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("metaId associado corretamente")
    void comMetaId() {
        UUID metaId = UUID.randomUUID();
        var t = new Transacao(UUID.randomUUID(), "Aplic", "", 500_00L, TipoTransacao.Saida, LocalDate.now(), metaId, Categoria.AdicionarMeta);
        assertEquals(metaId, t.metaId());
    }
}
