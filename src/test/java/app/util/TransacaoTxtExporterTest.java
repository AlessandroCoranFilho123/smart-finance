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

@DisplayName("TransacaoTxtExporter")
class TransacaoTxtExporterTest {

    @Test
    @DisplayName("formata exportação em TXT sem incluir seção de metas")
    void formataExportacaoSemSecaoDeMetas() {
        List<Transacao> transacoes = List.of(
                new Transacao(
                        UUID.randomUUID(),
                        "Salario",
                        "Pagamento principal",
                        5000_00L,
                        TipoTransacao.Entrada,
                        LocalDate.of(2026, 3, 30),
                        UUID.randomUUID(),
                        Categoria.Salario
                ),
                new Transacao(
                        UUID.randomUUID(),
                        "Aluguel",
                        "",
                        1200_00L,
                        TipoTransacao.Saida,
                        LocalDate.of(2026, 3, 29),
                        null,
                        Categoria.Aluguel
                )
        );

        String conteudo = TransacaoTxtExporter.formatar(transacoes);

        assertAll(
                () -> assertTrue(conteudo.contains("Smart Finance - Exportacao de Transacoes")),
                () -> assertTrue(conteudo.contains("Quantidade: 2")),
                () -> assertTrue(conteudo.contains("30/03/2026 | Entrada | Salario")),
                () -> assertTrue(conteudo.contains("Comentario: Pagamento principal")),
                () -> assertTrue(conteudo.contains("Categoria: Aluguel")),
                () -> assertFalse(conteudo.contains("Minhas Metas")),
                () -> assertFalse(conteudo.contains("metaId"))
        );
    }

    @Test
    @DisplayName("exporta arquivo UTF-8 com conteúdo formatado")
    void exportaArquivoComConteudoFormatado(@TempDir Path tempDir) throws Exception {
        Path destino = tempDir.resolve("transacoes.txt");
        List<Transacao> transacoes = List.of(
                new Transacao(
                        UUID.randomUUID(),
                        "Mercado",
                        "compras do mês",
                        350_00L,
                        TipoTransacao.Saida,
                        LocalDate.of(2026, 3, 28),
                        null,
                        Categoria.Alimentacao
                )
        );

        TransacaoTxtExporter.exportar(destino, transacoes);

        String conteudo = Files.readString(destino);
        assertAll(
                () -> assertTrue(Files.exists(destino)),
                () -> assertTrue(conteudo.contains("Mercado")),
                () -> assertTrue(conteudo.contains("compras do mês")),
                () -> assertTrue(conteudo.contains("Categoria: Alimentacao"))
        );
    }
}
