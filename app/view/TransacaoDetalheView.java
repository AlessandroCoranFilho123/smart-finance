package app.view;

import app.model.Transacao;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

public class TransacaoDetalheView {

    private static final NumberFormat nf =
            NumberFormat.getCurrencyInstance();

    public static void abrir(Transacao t) {

        Stage stage = new Stage();

        Label lblNome = new Label("Nome:");
        TextField txtNome = new TextField(t.getNome());
        txtNome.setDisable(true); // nome não editável (por enquanto)

        Label lblValor = new Label("Valor:");
        Label txtValor = new Label(
                nf.format(t.getValorCentavos() / 100.0)
        );

        Label lblCategoria = new Label("Categoria:");
        Label txtCategoria = new Label(t.getCategoria().name());

        Label lblMeta = new Label("Meta:");
        Label txtMeta = new Label(
                t.getMetaNome().isBlank()
                        ? "—"
                        : t.getMetaNome()
        );

        Label lblComentario = new Label("Comentário:");
        TextArea txtComentario = new TextArea(t.getComentario());
        txtComentario.setEditable(false);
        txtComentario.setWrapText(true);

        Label lblTags = new Label("Tags:");
        Label txtTags = new Label(
                t.getTags().isEmpty()
                        ? "—"
                        : String.join(", ", t.getTags())
        );

        // ==== DATA EDITÁVEL ====
        Label lblData = new Label("Data:");

        DatePicker datePicker =
                new DatePicker(t.getDataHora().toLocalDate());

        Spinner<Integer> horaSpinner =
                new Spinner<>(0, 23, t.getDataHora().getHour());

        Spinner<Integer> minutoSpinner =
                new Spinner<>(0, 59, t.getDataHora().getMinute());

        HBox horaBox = new HBox(5,
                new Label("Hora"), horaSpinner,
                new Label("Min"), minutoSpinner
        );

        Button btnSalvarData = new Button("Salvar data");

        btnSalvarData.setOnAction(_ -> {

            LocalDate data = datePicker.getValue();
            if (data == null) {
                alerta("Selecione uma data válida");
                return;
            }

            LocalTime hora = LocalTime.of(
                    horaSpinner.getValue(),
                    minutoSpinner.getValue()
            );

            LocalDateTime novaData = data.atTime(hora);

            t.setDataHora(novaData);
            stage.close();
        });

        VBox layout = new VBox(10,
                lblNome, txtNome,
                lblValor, txtValor,
                lblCategoria, txtCategoria,
                lblMeta, txtMeta,
                lblComentario, txtComentario,
                lblTags, txtTags,
                lblData, datePicker,
                horaBox,
                btnSalvarData
        );

        layout.setPadding(new Insets(20));
        layout.setPrefWidth(350);

        stage.setTitle("Detalhes da Transação");
        stage.setScene(new Scene(layout));
        stage.show();
    }

    private static void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }
}