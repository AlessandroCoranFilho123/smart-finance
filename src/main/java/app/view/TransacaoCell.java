package app.view;

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
    private final VBox infoBox;
    private final Label descricaoLabel;
    private final Label dataLabel;
    private final Label valorLabel;
    private final Region spacer;

    public TransacaoCell() {
        container = new HBox(15);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(12, 0, 12, 0));

        icon = new ImageView();
        icon.setFitWidth(40);
        icon.setFitHeight(40);
        icon.setPreserveRatio(true);

        infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        descricaoLabel = new Label();
        descricaoLabel.setStyle(
                "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-text-fill: #1B2559;"
        );

        dataLabel = new Label();
        dataLabel.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: #A3AED0;"
        );

        infoBox.getChildren().addAll(descricaoLabel, dataLabel);

        spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        valorLabel = new Label();
        valorLabel.setStyle(
                "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold;"
        );

        container.getChildren().addAll(icon, infoBox, spacer, valorLabel);
    }

    @Override
    protected void updateItem(Transacao transacao, boolean empty) {
        super.updateItem(transacao, empty);

        if (empty || transacao == null) {
            setGraphic(null);
            setText(null);
        } else {
            descricaoLabel.setText(transacao.getDescricao());

            dataLabel.setText(transacao.getData().format(DATE_FORMATTER));

            double valor = transacao.getValorCentavos() / 100.0;
            String valorTexto = String.format("R$ %.2f", valor);

            if (transacao.getTipo() == TipoTransacao.Entrada) {
                icon.setImage(IconManager.getImage("/app/icons/categoria/entrada.png"));
                valorLabel.setText("+ " + valorTexto);
                valorLabel.setStyle(
                        "-fx-font-size: 16px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-text-fill: #4ADE80;" // Verde para entrada
                );
            } else {
                icon.setImage(IconManager.getImage("/app/icons/categoria/saida.png"));
                valorLabel.setText("- " + valorTexto);
                valorLabel.setStyle(
                        "-fx-font-size: 16px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-text-fill: #F87171;" // Vermelho para saída
                );
            }

            setGraphic(container);
        }
    }
}