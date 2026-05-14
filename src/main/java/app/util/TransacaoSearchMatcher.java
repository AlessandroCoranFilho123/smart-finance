package app.util;

import app.model.Transacao;

import java.text.Normalizer;
import java.util.Locale;
import java.util.stream.Stream;

public final class TransacaoSearchMatcher {

    private TransacaoSearchMatcher() {
    }

    public static boolean corresponde(Transacao transacao, String busca) {
        String termoNormalizado = normalizar(busca);
        if (termoNormalizado.isBlank()) {
            return true;
        }

        return Stream.of(
                        transacao.descricao(),
                        transacao.comentario(),
                        transacao.categoria() != null ? transacao.categoria().name() : "",
                        transacao.tipo() != null ? transacao.tipo().name() : "",
                        FormatadorData.formatar(transacao.data())
                )
                .map(TransacaoSearchMatcher::normalizar)
                .anyMatch(campo -> campo.contains(termoNormalizado));
    }

    private static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }

        String semAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return semAcentos.toLowerCase(Locale.ROOT).trim();
    }
}
