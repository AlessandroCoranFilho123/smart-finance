package app.view;

import app.model.Transacao;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;

public class TransacaoDetalheView {

    private static final NumberFormat nf =
            NumberFormat.getCurrencyInstance();

    public static void abrir(Transacao t, Runnable onSave) {

        Stage stage = new Stage();

        Label lblNome = new Label("Nome:");
        TextField txtNome = new TextField(t.getNome());
        txtNome.setDisable(true);

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

        VBox layout = new VBox(10,
                lblNome, txtNome,
                lblValor, txtValor,
                lblCategoria, txtCategoria,
                lblMeta, txtMeta,
                lblComentario, txtComentario,
                lblTags, txtTags
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