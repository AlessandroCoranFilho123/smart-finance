package app.view;

import app.model.Categoria;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.util.IconManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class TransacaoCell extends ListCell<Transacao> {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

    private final HBox container;
    private final ImageView icon;
    private final Label descricaoLabel;
    private final Label dataLabel;
    private final Label valorLabel;

    public TransacaoCell() {
        container = new HBox(15);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(12, 0, 12, 0));

        icon = new ImageView();
        icon.setFitWidth(40);
        icon.setFitHeight(40);
        icon.setPreserveRatio(true);

        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        descricaoLabel = new Label();
        descricaoLabel.getStyleClass().add("cell-title");

        dataLabel = new Label();
        dataLabel.getStyleClass().add("cell-subtitle");

        infoBox.getChildren().addAll(descricaoLabel, dataLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        valorLabel = new Label();
        valorLabel.getStyleClass().add("cell-valor");

        container.getChildren().addAll(icon, infoBox, spacer, valorLabel);
    }

    private javafx.scene.image.Image resolverIcone(Transacao transacao) {
        Categoria categoria = transacao.categoria();
        boolean isEntrada = transacao.tipo() == TipoTransacao.Entrada;

        if (categoria == null || categoria == Categoria.Outros) {
            return isEntrada
                    ? IconManager.getImage("/app/icons/categoria/entrada.png")
                    : IconManager.getImage("/app/icons/categoria/saida.png");
        }

        javafx.scene.image.Image icone = IconManager.getCategoriaIcon(categoria);

        // Fallback para seta se o arquivo de ícone não existir
        if (icone == null) {
            return isEntrada
                    ? IconManager.getImage("/app/icons/categoria/entrada.png")
                    : IconManager.getImage("/app/icons/categoria/saida.png");
        }

        return icone;
    }

    @Override
    protected void updateItem(Transacao transacao, boolean empty) {
        super.updateItem(transacao, empty);

        if (empty || transacao == null) {
            setGraphic(null);
            setText(null);
        } else {
            descricaoLabel.setText(transacao.descricao());

            String data = transacao.data().format(DATE_FORMATTER);
            String comentario = transacao.comentario();
            if (comentario != null && !comentario.isBlank()) {
                dataLabel.setText(data + " · " + comentario);
            } else {
                dataLabel.setText(data);
            }

            double valor = transacao.valorCentavos() / 100.0;
            String valorTexto = String.format("R$ %.2f", valor);
            boolean isEntrada = transacao.tipo() == TipoTransacao.Entrada;
            icon.setImage(resolverIcone(transacao));

            if (isEntrada) {
                valorLabel.setText("+ " + valorTexto);
                valorLabel.getStyleClass().removeAll("cell-valor-saida");
                valorLabel.getStyleClass().add("cell-valor-entrada");
            } else {
                valorLabel.setText("- " + valorTexto);
                valorLabel.getStyleClass().removeAll("cell-valor-entrada");
                valorLabel.getStyleClass().add("cell-valor-saida");
            }

            setGraphic(container);
        }
    }
}