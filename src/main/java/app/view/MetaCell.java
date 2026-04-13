package app.view;

import app.model.Meta;
import app.util.IconManager;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class MetaCell extends ListCell<Meta> {

    private static final String PROGRESSO_VERMELHO = "progress-bar-meta-vermelho";
    private static final String PROGRESSO_LARANJA = "progress-bar-meta-laranja";
    private static final String PROGRESSO_VERDE = "progress-bar-meta-verde";

    private final HBox root;
    private final ImageView icon;
    private final Label nomeLabel;
    private final Label valorLabel;
    private final StackPane progressTrack;
    private final Region progressFill;
    private final Label percentLabel;
    private final boolean compacto;

    public MetaCell() {
        this(false);
    }

    public MetaCell(boolean compacto) {
        this.compacto = compacto;

        icon = new ImageView();
        icon.setFitWidth(40);
        icon.setFitHeight(40);
        icon.setPreserveRatio(true);

        VBox container = new VBox(10);
        container.setPadding(new Insets(12, 0, 12, 0));
        VBox.setVgrow(container, Priority.ALWAYS);

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);

        nomeLabel = new Label();
        nomeLabel.getStyleClass().add("cell-title");
        nomeLabel.setMaxWidth(Double.MAX_VALUE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        valorLabel = new Label();
        valorLabel.getStyleClass().add("cell-subtitle");

        headerBox.getChildren().addAll(nomeLabel, spacer, valorLabel);

        progressTrack = new StackPane();
        progressTrack.getStyleClass().add("meta-progress-track");
        progressTrack.setMinHeight(8);
        progressTrack.setPrefHeight(8);
        progressTrack.setMaxWidth(Double.MAX_VALUE);

        progressFill = new Region();
        progressFill.getStyleClass().add("meta-progress-fill");
        progressFill.setMinHeight(8);
        progressFill.setPrefHeight(8);
        progressFill.setMinWidth(0);
        progressFill.setMaxWidth(Region.USE_PREF_SIZE);
        progressFill.setVisible(false);
        StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);
        progressTrack.getChildren().add(progressFill);

        Rectangle progressClip = new Rectangle();
        progressClip.setArcWidth(16);
        progressClip.setArcHeight(16);
        progressClip.widthProperty().bind(progressTrack.widthProperty());
        progressClip.heightProperty().bind(progressTrack.heightProperty());
        progressTrack.setClip(progressClip);

        percentLabel = new Label();
        percentLabel.getStyleClass().addAll("cell-subtitle", "meta-progress-label");

        if (compacto) {
            nomeLabel.setWrapText(true);
            container.getChildren().addAll(nomeLabel, progressTrack, percentLabel);
        } else {
            container.getChildren().addAll(headerBox, progressTrack, percentLabel);
        }
        HBox.setHgrow(container, Priority.ALWAYS);

        root = new HBox(15);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(compacto ? new Insets(4, 8, 4, 0) : new Insets(4, 0, 4, 0));
        root.getChildren().addAll(icon, container);

        root.setMaxWidth(Double.MAX_VALUE);
        setMaxWidth(Double.MAX_VALUE);
    }

    private void aplicarFaixaDeCor(double progresso) {
        progressFill.getStyleClass().removeAll(
                PROGRESSO_VERMELHO,
                PROGRESSO_LARANJA,
                PROGRESSO_VERDE,
                "progress-bar-meta-ativa",
                "progress-bar-meta-concluida"
        );

        if (progresso >= 0.75) {
            progressFill.getStyleClass().add(PROGRESSO_VERDE);
        } else if (progresso >= 0.50) {
            progressFill.getStyleClass().add(PROGRESSO_LARANJA);
        } else {
            progressFill.getStyleClass().add(PROGRESSO_VERMELHO);
        }
    }

    private void aplicarPreenchimento(double progresso) {
        double progressoAjustado = Math.max(0.0, Math.min(1.0, progresso));

        progressFill.prefWidthProperty().unbind();
        progressFill.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> progressTrack.getWidth() * progressoAjustado,
                progressTrack.widthProperty()
        ));
        progressFill.setVisible(progressoAjustado > 0);
    }

    private void atualizarTextoProgresso(double progresso) {
        if (progresso >= 1.0) {
            percentLabel.setText("Meta concluída!");
            percentLabel.getStyleClass().removeAll("cell-subtitle");
            if (!percentLabel.getStyleClass().contains("cell-meta-concluida")) {
                percentLabel.getStyleClass().add("cell-meta-concluida");
            }
        } else {
            percentLabel.setText(String.format("%.1f%% concluído", progresso * 100));
            percentLabel.getStyleClass().removeAll("cell-meta-concluida");
            if (!percentLabel.getStyleClass().contains("cell-subtitle")) {
                percentLabel.getStyleClass().add("cell-subtitle");
            }
        }
    }

    @Override
    protected void updateItem(Meta meta, boolean empty) {
        super.updateItem(meta, empty);

        if (empty || meta == null) {
            progressFill.prefWidthProperty().unbind();
            setGraphic(null);
            setText(null);
        } else {
            nomeLabel.setText(meta.getNome());

            double progresso = meta.progresso();
            aplicarPreenchimento(progresso);
            aplicarFaixaDeCor(progresso);

            Image iconeImg = IconManager.getImage("/app/icons/categoria/meta.png");
            if (iconeImg != null) icon.setImage(iconeImg);

            atualizarTextoProgresso(progresso);

            if (!compacto) {
                double atual = meta.getAtualCentavos() / 100.0;
                double alvo = meta.getAlvoCentavos() / 100.0;
                valorLabel.setText(String.format("R$ %.2f / R$ %.2f", atual, alvo));
            }

            setGraphic(root);
        }
    }
}
