package app.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("FormatadorData")
class FormatadorDataTest {

    @Test
    @DisplayName("formata datas no padrao dd/MM/yyyy")
    void formataDatasNoPadraoBrasileiro() {
        assertEquals("30/03/2026", FormatadorData.formatar(LocalDate.of(2026, 3, 30)));
    }

    @Test
    @DisplayName("converte texto dd/MM/yyyy para LocalDate")
    void converteTextoParaLocalDate() {
        LocalDate data = FormatadorData.criarConversor().fromString("30/03/2026");

        assertEquals(LocalDate.of(2026, 3, 30), data);
    }

    @Test
    @DisplayName("retorna nulo ao converter texto vazio")
    void retornaNuloAoConverterTextoVazio() {
        assertNull(FormatadorData.criarConversor().fromString("   "));
    }
}
