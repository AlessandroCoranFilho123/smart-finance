package app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ValorMonetario {

    private ValorMonetario() {
    }

    public static long parseCentavos(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0L;
        }

        try {
            BigDecimal valor = new BigDecimal(normalizar(texto));
            return valor
                    .movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido", e);
        }
    }

    public static String formatarCentavos(long centavos) {
        return BigDecimal.valueOf(centavos, 2)
                .setScale(2, RoundingMode.UNNECESSARY)
                .toPlainString();
    }

    private static String normalizar(String texto) {
        String normalizado = texto.replace("R$", "").trim();
        if (normalizado.contains(",")) {
            return normalizado.replace(".", "").replace(",", ".");
        }
        return normalizado;
    }
}
