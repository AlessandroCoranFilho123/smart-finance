package app.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Meta - edge cases e limites")
class MetaEdgeCaseTest {

    @Nested
    @DisplayName("Valores limites")
    class ValoresLimites {
        @Test
        @DisplayName("alvo maximo Long.MAX_VALUE")
        void alvoMaximoLong() {
            Meta m = new Meta("Max", Long.MAX_VALUE);
            assertFalse(m.semAlvo());
            assertEquals(Long.MAX_VALUE, m.getAlvoCentavos());
        }

        @Test
        @DisplayName("atual pode exceder alvo")
        void atualExcedeAlvo() {
            Meta m = new Meta(UUID.randomUUID(), "Excedente", 100_00L, 200_00L);
            assertEquals(1.0, m.progresso(), 0.001);
        }

        @Test
        @DisplayName("adicionar valor muito grande")
        void adicionarValorMuitoGrande() {
            Meta m = new Meta(UUID.randomUUID(), "Grande", Long.MAX_VALUE, 1L);
            m.adicionar(Long.MAX_VALUE / 2);
            assertTrue(m.getAtualCentavos() > 0);
        }

        @Test
        @DisplayName("retirar exatamente todo valor")
        void retirarTudo() {
            Meta m = new Meta(UUID.randomUUID(), "Zerar", 100_00L, 100_00L);
            long retirado = m.retirar(100_00L);
            assertEquals(100_00L, retirado);
            assertEquals(0, m.getAtualCentavos());
        }
    }

    @Nested
    @DisplayName("Estados especiais")
    class EstadosEspeciais {
        @Test
        @DisplayName("meta sem nome (vazio)")
        void metaSemNome() {
            Meta m = new Meta("", 100_00L);
            assertEquals("", m.getNome());
            assertFalse(m.semAlvo());
        }

        @Test
        @DisplayName("meta com nome de caracteres unicode")
        void metaNomeUnicode() {
            Meta m = new Meta("Poupança € ¥ £", 1000_00L);
            assertEquals("Poupança € ¥ £", m.getNome());
        }

        @Test
        @DisplayName("progresso em meta vazia retorna 0")
        void progressoMetaVazia() {
            Meta m = new Meta(UUID.randomUUID(), "Vazia", 100_00L, 0L);
            assertEquals(0.0, m.progresso());
        }

        @Test
        @DisplayName("restante em meta vazia")
        void restanteMetaVazia() {
            Meta m = new Meta(UUID.randomUUID(), "Vazia", 100_00L, 0L);
            assertEquals(100_00L, m.restanteParaAlvo());
        }
    }

    @Nested
    @DisplayName("Operacoes encadeadas")
    class OperacoesEncadeadas {
        @Test
        @DisplayName("adicionar e retirar multiplas vezes")
        void adicionarRetirarMultiplasVezes() {
            Meta m = new Meta(UUID.randomUUID(), "Teste", 1000_00L, 100_00L);
            m.adicionar(200_00L);
            m.retirar(50_00L);
            m.adicionar(150_00L);
            assertEquals(400_00L, m.getAtualCentavos());
            assertEquals(600_00L, m.restanteParaAlvo());
        }

        @Test
        @DisplayName("retirar mais que disponivel retorna valor permitido")
        void retirarExcessoRetornaValorPermitido() {
            Meta m = new Meta(UUID.randomUUID(), "Pouco", 1000_00L, 100_00L);
            long permitido = m.retirar(500_00L);
            assertEquals(100_00L, permitido);
            assertEquals(0, m.getAtualCentavos());
        }
    }

    @Nested
    @DisplayName("Meta concluida")
    class MetaConcluida {
        @Test
        @DisplayName("exatamente no alvo considera concluida")
        void exatamenteNoAlvo() {
            Meta m = new Meta(UUID.randomUUID(), "Completa", 500_00L, 500_00L);
            assertEquals(1.0, m.progresso());
            assertEquals(0, m.restanteParaAlvo());
        }

        @Test
        @DisplayName("bem alem do alvo")
        void bemAcimaDoAlvo() {
            Meta m = new Meta(UUID.randomUUID(), "Ultrapassada", 100_00L, 1000_00L);
            assertEquals(1.0, m.progresso());
            assertEquals(0, m.restanteParaAlvo());
        }
    }

    @Nested
    @DisplayName("Comparacao e identificacao")
    class Comparacao {
        @Test
        @DisplayName("UUIDs diferentes para metas diferentes")
        void uuidsDiferentes() {
            Meta m1 = new Meta("A", 100_00L);
            Meta m2 = new Meta("A", 100_00L);
            assertNotEquals(m1.getId(), m2.getId());
        }

        @Test
        @DisplayName("mesmo nome nao significa mesma meta")
        void mesmoNomeDiferentes() {
            Meta m1 = new Meta("Viagem", 1000_00L);
            Meta m2 = new Meta("Viagem", 1000_00L);
            assertNotEquals(m1.getId(), m2.getId());
            assertNotEquals(m1, m2);
        }
    }
}
