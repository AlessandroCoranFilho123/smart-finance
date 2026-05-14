package app.util;

import app.model.Categoria;
import app.model.TipoTransacao;
import app.model.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransacaoCsvExporter")
class TransacaoCsvExporterTest {

    @Test
    @DisplayName("formata CSV com cabeçalho e escape de campos especiais")
    void formataCsvComCabecalhoEEscape() {
        List<Transacao> transacoes = List.of(
                new Transacao(
                        UUID.randomUUID(),
                        "Mercado; mensal",
                        "Compra \"grande\"",
                        350_90L,
                        TipoTransacao.Saida,
                        LocalDate.of(2026, 4, 12),
                        null,
                        Categoria.Alimentacao
                )
        );

        String conteudo = TransacaoCsvExporter.formatar(transacoes);

        assertAll(
                () -> assertTrue(conteudo.startsWith("Data;Tipo;Descricao;Categoria;Comentario;Valor")),
                () -> assertTrue(conteudo.contains("12/04/2026;Saida;\"Mercado; mensal\";Alimentacao;\"Compra \"\"grande\"\"\";350,90")),
                () -> assertFalse(conteudo.contains("R$"))
        );
    }

    @Test
    @DisplayName("exporta arquivo UTF-8 com BOM para abrir bem no Excel")
    void exportaArquivoUtf8ComBom(@TempDir Path tempDir) throws Exception {
        Path destino = tempDir.resolve("transacoes.csv");

        TransacaoCsvExporter.exportar(destino, List.of(
                new Transacao(
                        UUID.randomUUID(),
                        "Salario",
                        "",
                        5000_00L,
                        TipoTransacao.Entrada,
                        LocalDate.of(2026, 4, 10),
                        null,
                        Categoria.Salario
                )
        ));

        byte[] bytes = Files.readAllBytes(destino);
        String conteudo = Files.readString(destino);

        assertAll(
                () -> assertTrue(Files.exists(destino)),
                () -> assertEquals((byte) 0xEF, bytes[0]),
                () -> assertEquals((byte) 0xBB, bytes[1]),
                () -> assertEquals((byte) 0xBF, bytes[2]),
                () -> assertTrue(conteudo.contains("10/04/2026;Entrada;Salario;Salario;;5000,00"))
        );
    }

    @Test
    @DisplayName("mantém apenas o cabeçalho quando não há transações")
    void mantemApenasCabecalhoQuandoNaoHaTransacoes() {
        String conteudo = TransacaoCsvExporter.formatar(List.of());

        assertEquals("Data;Tipo;Descricao;Categoria;Comentario;Valor" + System.lineSeparator(), conteudo);
    }

    @Test
    @DisplayName("ordena o CSV da transação mais antiga para a mais recente")
    void ordenaCsvDaMaisAntigaParaMaisRecente() {
        Transacao recente = new Transacao(
                UUID.randomUUID(),
                "Conta de luz",
                "",
                180_00L,
                TipoTransacao.Saida,
                LocalDate.of(2026, 4, 20),
                null,
                Categoria.Luz
        );
        Transacao antiga = new Transacao(
                UUID.randomUUID(),
                "Salario",
                "",
                5000_00L,
                TipoTransacao.Entrada,
                LocalDate.of(2026, 4, 5),
                null,
                Categoria.Salario
        );

        String conteudo = TransacaoCsvExporter.formatar(List.of(recente, antiga));

        assertTrue(conteudo.indexOf("05/04/2026;Entrada;Salario") <
                conteudo.indexOf("20/04/2026;Saida;Conta de luz"));
    }
}
