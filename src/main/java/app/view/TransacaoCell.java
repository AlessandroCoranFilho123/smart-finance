package app.view;

import app.model.Categoria;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.util.FormatadorData;
import app.util.IconManager;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

public class TransacaoCell extends ListCell<Transacao> {

    private final HBox container;
    private final ImageView icon;
    private final VBox infoBox;
    private final Label descricaoLabel;
    private final Label dataLabel;
    private final Label valorLabel;

    public TransacaoCell() {
        container = new HBox(15);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(12, 0, 12, 0));
        container.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0, getWidth() - snappedLeftInset() - snappedRightInset()),
                widthProperty(),
                paddingProperty()
        ));
        container.maxWidthProperty().bind(container.prefWidthProperty());

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        icon = new ImageView();
        icon.setFitWidth(40);
        icon.setFitHeight(40);
        icon.setPreserveRatio(true);

        infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setMinWidth(0);
        infoBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        descricaoLabel = new Label();
        descricaoLabel.getStyleClass().add("cell-title");
        configurarTextoTruncado(descricaoLabel);

        dataLabel = new Label();
        dataLabel.getStyleClass().add("cell-subtitle");
        configurarTextoTruncado(dataLabel);

        infoBox.getChildren().addAll(descricaoLabel, dataLabel);

        valorLabel = new Label();
        valorLabel.getStyleClass().add("cell-valor");
        valorLabel.setAlignment(Pos.CENTER_RIGHT);
        valorLabel.setWrapText(false);
        valorLabel.setMinWidth(Region.USE_PREF_SIZE);
        valorLabel.setMaxWidth(Region.USE_PREF_SIZE);

        container.getChildren().addAll(icon, infoBox, valorLabel);
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

    private void configurarTextoTruncado(Label label) {
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    protected void updateItem(Transacao transacao, boolean empty) {
        super.updateItem(transacao, empty);

        if (empty || transacao == null) {
            setGraphic(null);
            setText(null);
        } else {
            descricaoLabel.setText(transacao.descricao());

            String data = FormatadorData.formatar(transacao.data());
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
