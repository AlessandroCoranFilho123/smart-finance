package app.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Meta - modelo de domínio")
class MetaTest {

    @Nested
    @DisplayName("Construção")
    class Construcao {
        @Test
        void idNullGeraUUID() {
            assertNotNull(new Meta(null, "Viagem", 100_00L, 0).getId());
        }

        @Test
        void construtorConvenienciaZeraAtual() {
            Meta m = new Meta("Viagem", 500_00L);
            assertEquals(0, m.getAtualCentavos());
            assertNotNull(m.getId());
        }

        @Test
        void nomeNuloViraVazio() {
            assertEquals("", new Meta(UUID.randomUUID(), null, 100_00L, 0).getNome());
        }

        @Test
        void nomeTrimado() {
            assertEquals("Poupança", new Meta(UUID.randomUUID(), "  Poupança  ", 100_00L, 0).getNome());
        }

        @Test
        void toStringRetornaNome() {
            assertEquals("Carro", new Meta("Carro", 1000_00L).toString());
        }

        @Test
        void getIdAsStringRetornaUUIDValido() {
            Meta m = new Meta("X", 100_00L);
            assertEquals(m.getId(), UUID.fromString(m.getIdAsString()));
        }
    }

    @Nested
    @DisplayName("semAlvo()")
    class SemAlvo {
        @Test
        void retornaTrueParaNull() {
            assertTrue(new Meta(UUID.randomUUID(), "T", null, 0).semAlvo());
        }

        @Test
        void retornaTrueParaZero() {
            assertTrue(new Meta(UUID.randomUUID(), "T", 0L, 0).semAlvo());
        }

        @Test
        void retornaTrueParaNegativo() {
            assertTrue(new Meta(UUID.randomUUID(), "T", -1L, 0).semAlvo());
        }

        @Test
        void retornaFalseComAlvo() {
            assertFalse(new Meta("T", 1000_00L).semAlvo());
        }
    }

    @Nested
    @DisplayName("progresso()")
    class Progresso {
        @Test
        void zeroSemAlvo() {
            assertEquals(0.0, new Meta(UUID.randomUUID(), "T", null, 50_00L).progresso());
        }

        @Test
        void cinquentaPorcento() {
            assertEquals(0.5, new Meta(UUID.randomUUID(), "T", 200_00L, 100_00L).progresso(), 0.001);
        }

        @Test
        void limitadoAUm() {
            assertEquals(1.0, new Meta(UUID.randomUUID(), "T", 100_00L, 200_00L).progresso());
        }

        @Test
        void exatamenteUm() {
            assertEquals(1.0, new Meta(UUID.randomUUID(), "T", 300_00L, 300_00L).progresso());
        }

        @Test
        void progressoZeroSemAporte() {
            assertEquals(0.0, new Meta("T", 100_00L).progresso());
        }
    }

    @Nested
    @DisplayName("adicionar()")
    class Adicionar {
        @Test
        void somaCorretamente() {
            Meta m = new Meta(UUID.randomUUID(), "T", 1000_00L, 100_00L);
            m.adicionar(200_00L);
            assertEquals(300_00L, m.getAtualCentavos());
        }

        @Test
        void zeroLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> new Meta("T", 500_00L).adicionar(0));
        }

        @Test
        void negativoLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> new Meta("T", 500_00L).adicionar(-1));
        }

        @Test
        void adicionarMultiplasVezes() {
            Meta m = new Meta("T", 1000_00L);
            m.adicionar(100_00L);
            m.adicionar(200_00L);
            m.adicionar(300_00L);
            assertEquals(600_00L, m.getAtualCentavos());
        }
    }

    @Nested
    @DisplayName("retirar()")
    class Retirar {
        @Test
        void retirarParcial() {
            Meta m = new Meta(UUID.randomUUID(), "T", 1000_00L, 500_00L);
            assertEquals(200_00L, m.retirar(200_00L));
            assertEquals(300_00L, m.getAtualCentavos());
        }

        @Test
        void limitadoAoDisponivel() {
            Meta m = new Meta(UUID.randomUUID(), "T", 1000_00L, 100_00L);
            assertEquals(100_00L, m.retirar(500_00L));
            assertEquals(0, m.getAtualCentavos());
        }

        @Test
        void zeroRetornaZero() {
            Meta m = new Meta(UUID.randomUUID(), "T", 1000_00L, 200_00L);
            assertEquals(0, m.retirar(0));
            assertEquals(200_00L, m.getAtualCentavos());
        }

        @Test
        void negativoRetornaZero() {
            Meta m = new Meta(UUID.randomUUID(), "T", 1000_00L, 200_00L);
            assertEquals(0, m.retirar(-100));
            assertEquals(200_00L, m.getAtualCentavos());
        }

        @Test
        void retirarTudoZeraAtual() {
            Meta m = new Meta(UUID.randomUUID(), "T", 1000_00L, 1000_00L);
            m.retirar(1000_00L);
            assertEquals(0, m.getAtualCentavos());
        }
    }

    @Nested
    @DisplayName("restanteParaAlvo()")
    class RestanteParaAlvo {
        @Test
        void semAlvoRetornaMaxValue() {
            assertEquals(Long.MAX_VALUE, new Meta(UUID.randomUUID(), "T", null, 0).restanteParaAlvo());
        }

        @Test
        void restanteCorreto() {
            assertEquals(350_00L, new Meta(UUID.randomUUID(), "T", 500_00L, 150_00L).restanteParaAlvo());
        }

        @Test
        void metaAtingidaRetornaZero() {
            assertEquals(0, new Meta(UUID.randomUUID(), "T", 300_00L, 400_00L).restanteParaAlvo());
        }

        @Test
        void exatamenteNaMetaRetornaZero() {
            assertEquals(0, new Meta(UUID.randomUUID(), "T", 300_00L, 300_00L).restanteParaAlvo());
        }
    }
}
