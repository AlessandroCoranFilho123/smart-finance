package app.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transacao - edge cases")
class TransacaoEdgeCaseTest {

    @Nested
    @DisplayName("Valores limites")
    class ValoresLimites {
        @Test
        @DisplayName("valor maximo de centavos")
        void valorMaximoCentavos() {
            long maxValue = Long.MAX_VALUE;
            var t = new Transacao(UUID.randomUUID(), "Max", "", maxValue,
                    TipoTransacao.Entrada, LocalDate.now(), null, Categoria.Salario);
            assertEquals(maxValue, t.valorCentavos());
        }

        @Test
        @DisplayName("valor minimo (1 centavo)")
        void valorMinimoCentavo() {
            var t = new Transacao(UUID.randomUUID(), "Min", "", 1L,
                    TipoTransacao.Saida, LocalDate.now(), null, Categoria.Outros);
            assertEquals(1L, t.valorCentavos());
        }
    }

    @Nested
    @DisplayName("Valores especiais")
    class ValoresEspeciais {
        @Test
        @DisplayName("descricao vazia")
        void descricaoVazia() {
            var t = new Transacao(UUID.randomUUID(), "", "", 100L,
                    TipoTransacao.Entrada, LocalDate.now(), null, Categoria.Salario);
            assertEquals("", t.descricao());
        }

        @Test
        @DisplayName("descricao com caracteres especiais")
        void descricaoCaracteresEspeciais() {
            var t = new Transacao(UUID.randomUUID(), "R$ 1.000,00 + 10% = €", "", 1000_00L,
                    TipoTransacao.Entrada, LocalDate.now(), null, Categoria.Salario);
            assertEquals("R$ 1.000,00 + 10% = €", t.descricao());
        }

        @Test
        @DisplayName("comentario multilinha")
        void comentarioMultilinha() {
            String multiLine = "Linha 1\nLinha 2\nLinha 3";
            var t = new Transacao(UUID.randomUUID(), "Desc", multiLine, 100L,
                    TipoTransacao.Saida, LocalDate.now(), null, Categoria.Outros);
            assertEquals(multiLine, t.comentario());
        }
    }

    @Nested
    @DisplayName("Datas")
    class Datas {
        @Test
        @DisplayName("data futura")
        void dataFutura() {
            LocalDate future = LocalDate.now().plusYears(10);
            var t = new Transacao(UUID.randomUUID(), "Futuro", "", 100L,
                    TipoTransacao.Entrada, future, null, Categoria.Salario);
            assertEquals(future, t.data());
        }

        @Test
        @DisplayName("data passada")
        void dataPassada() {
            LocalDate past = LocalDate.now().minusYears(5);
            var t = new Transacao(UUID.randomUUID(), "Passado", "", 100L,
                    TipoTransacao.Saida, past, null, Categoria.Alimentacao);
            assertEquals(past, t.data());
        }

        @Test
        @DisplayName("data de ano bissexto")
        void dataAnoBissexto() {
            LocalDate leapDay = LocalDate.of(2024, 2, 29);
            var t = new Transacao(UUID.randomUUID(), "Bissexto", "", 100L,
                    TipoTransacao.Entrada, leapDay, null, Categoria.Salario);
            assertEquals(leapDay, t.data());
        }
    }

    @Nested
    @DisplayName("Categorias")
    class Categorias {
        @Test
        @DisplayName("todas as categorias podem ser usadas")
        void todasCategorias() {
            for (Categoria cat : Categoria.values()) {
                var t = new Transacao(UUID.randomUUID(), cat.toString(), "", 100L,
                        TipoTransacao.Entrada, LocalDate.now(), null, cat);
                assertEquals(cat, t.categoria());
            }
        }

        @Test
        @DisplayName("categoria null com metaId")
        void categoriaNullComMetaId() {
            UUID metaId = UUID.randomUUID();
            var t = new Transacao(UUID.randomUUID(), "Test", "", 100L,
                    TipoTransacao.Saida, LocalDate.now(), metaId, null);
            assertNull(t.categoria());
            assertEquals(metaId, t.metaId());
        }
    }
}
