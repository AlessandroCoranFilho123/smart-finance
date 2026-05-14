package app.util;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class FormatadorData {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final StringConverter<LocalDate> DATE_CONVERTER = new StringConverter<>() {
        @Override
        public String toString(LocalDate data) {
            return formatar(data);
        }

        @Override
        public LocalDate fromString(String texto) {
            if (texto == null || texto.isBlank()) {
                return null;
            }
            return LocalDate.parse(texto.trim(), DATE_FORMATTER);
        }
    };

    private FormatadorData() {
    }

    public static void configurar(DatePicker... datePickers) {
        for (DatePicker datePicker : datePickers) {
            if (datePicker != null) {
                datePicker.setConverter(DATE_CONVERTER);
            }
        }
    }

    public static String formatar(LocalDate data) {
        return data == null ? "" : DATE_FORMATTER.format(data);
    }

    public static StringConverter<LocalDate> criarConversor() {
        return DATE_CONVERTER;
    }
}
