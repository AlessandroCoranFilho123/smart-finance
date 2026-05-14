package app.util;

import app.model.Transacao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TransacaoCsvExporter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat VALUE_FORMAT;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.of("pt", "BR"));
        VALUE_FORMAT = new DecimalFormat("0.00", symbols);
        VALUE_FORMAT.setGroupingUsed(false);
    }

    private TransacaoCsvExporter() {
    }

    public static void exportar(Path destino, List<Transacao> transacoes) throws IOException {
        Files.writeString(destino, "\uFEFF" + formatar(transacoes), StandardCharsets.UTF_8);
    }

    public static String formatar(List<Transacao> transacoes) {
        StringBuilder builder = new StringBuilder();
        builder.append("Data;Tipo;Descricao;Categoria;Comentario;Valor")
                .append(System.lineSeparator());

        for (Transacao transacao : ordenarPorDataCrescente(transacoes)) {
            builder.append(escape(DATE_FORMAT.format(transacao.data()))).append(';')
                    .append(escape(transacao.tipo().name())).append(';')
                    .append(escape(transacao.descricao())).append(';')
                    .append(escape(transacao.categoria() != null
                            ? transacao.categoria().name()
                            : "Sem categoria")).append(';')
                    .append(escape(transacao.comentario())).append(';')
                    .append(escape(formatarValor(transacao.valorCentavos())))
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    private static String formatarValor(long valorCentavos) {
        return VALUE_FORMAT.format(valorCentavos / 100.0);
    }

    private static String escape(String valor) {
        String conteudo = valor == null ? "" : valor;
        boolean precisaAspas = conteudo.contains(";")
                || conteudo.contains("\"")
                || conteudo.contains("\n")
                || conteudo.contains("\r");

        if (!precisaAspas) {
            return conteudo;
        }

        return "\"" + conteudo.replace("\"", "\"\"") + "\"";
    }

    private static List<Transacao> ordenarPorDataCrescente(List<Transacao> transacoes) {
        return transacoes.stream()
                .sorted(Comparator.comparing(Transacao::data))
                .toList();
    }
}
