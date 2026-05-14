package app.util;

import app.model.TipoTransacao;
import app.model.Transacao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TransacaoTxtExporter {

    private static final NumberFormat CURRENCY_FORMAT =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private TransacaoTxtExporter() {
    }

    public static void exportar(Path destino, List<Transacao> transacoes) throws IOException {
        Files.writeString(destino, formatar(transacoes), StandardCharsets.UTF_8);
    }

    public static String formatar(List<Transacao> transacoes) {
        long totalEntradas = somarPorTipo(transacoes, TipoTransacao.Entrada);
        long totalSaidas = somarPorTipo(transacoes, TipoTransacao.Saida);

        StringBuilder builder = new StringBuilder();
        builder.append("Smart Finance - Exportacao de Transacoes").append(System.lineSeparator());
        builder.append("Gerado em: ")
                .append(TIMESTAMP_FORMAT.format(LocalDateTime.now()))
                .append(System.lineSeparator());
        builder.append("Quantidade: ").append(transacoes.size()).append(System.lineSeparator());
        builder.append("Total de Entradas: ")
                .append(CURRENCY_FORMAT.format(totalEntradas / 100.0))
                .append(System.lineSeparator());
        builder.append("Total de Saidas: ")
                .append(CURRENCY_FORMAT.format(totalSaidas / 100.0))
                .append(System.lineSeparator());
        builder.append(System.lineSeparator());

        if (transacoes.isEmpty()) {
            builder.append("Nenhuma transacao encontrada.").append(System.lineSeparator());
            return builder.toString();
        }

        List<Transacao> transacoesOrdenadas = ordenarPorDataDecrescente(transacoes);
        for (int i = 0; i < transacoesOrdenadas.size(); i++) {
            Transacao transacao = transacoesOrdenadas.get(i);

            builder.append(i + 1).append(". ")
                    .append(DATE_FORMAT.format(transacao.data()))
                    .append(" | ")
                    .append(transacao.tipo())
                    .append(" | ")
                    .append(transacao.descricao())
                    .append(" | ")
                    .append(CURRENCY_FORMAT.format(transacao.valorCentavos() / 100.0))
                    .append(System.lineSeparator());

            builder.append("Categoria: ")
                    .append(transacao.categoria() != null ? transacao.categoria() : "Sem categoria")
                    .append(System.lineSeparator());

            if (!transacao.comentario().isBlank()) {
                builder.append("Comentario: ")
                        .append(transacao.comentario())
                        .append(System.lineSeparator());
            }

            if (i < transacoesOrdenadas.size() - 1) {
                builder.append(System.lineSeparator());
            }
        }

        return builder.toString();
    }

    private static long somarPorTipo(List<Transacao> transacoes, TipoTransacao tipo) {
        return transacoes.stream()
                .filter(transacao -> transacao.tipo() == tipo)
                .mapToLong(Transacao::valorCentavos)
                .sum();
    }

    private static List<Transacao> ordenarPorDataDecrescente(List<Transacao> transacoes) {
        return transacoes.stream()
                .sorted(Comparator.comparing(Transacao::data).reversed())
                .toList();
    }
}
