package app.view;

import app.model.Meta;
import app.util.IconManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MetaCell extends ListCell<Meta> {

    private final HBox root;
    private final ImageView icon;
    private final Label nomeLabel;
    private final Label valorLabel;
    private final ProgressBar progressBar;
    private final Label percentLabel;

    public MetaCell() {
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        valorLabel = new Label();
        valorLabel.getStyleClass().add("cell-subtitle");

        headerBox.getChildren().addAll(nomeLabel, spacer, valorLabel);

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(8);
        // Classe base — cor controlada pelo CSS de acordo com o tema
        progressBar.getStyleClass().add("progress-bar-meta");

        percentLabel = new Label();
        percentLabel.getStyleClass().add("cell-subtitle");

        container.getChildren().addAll(headerBox, progressBar, percentLabel);
        HBox.setHgrow(container, Priority.ALWAYS);

        root = new HBox(15);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(4, 0, 4, 0));
        root.getChildren().addAll(icon, container);

        root.setMaxWidth(Double.MAX_VALUE);
        setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    protected void updateItem(Meta meta, boolean empty) {
        super.updateItem(meta, empty);

        if (empty || meta == null) {
            setGraphic(null);
            setText(null);
        } else {
            nomeLabel.setText(meta.getNome());

            double atual = meta.getAtualCentavos() / 100.0;
            double alvo = meta.getAlvoCentavos() / 100.0;
            valorLabel.setText(String.format("R$ %.2f / R$ %.2f", atual, alvo));

            double progresso = meta.progresso();
            progressBar.setProgress(progresso);

            Image iconeImg = IconManager.getImage("/app/icons/categoria/meta.png");
            if (iconeImg != null) icon.setImage(iconeImg);

            if (progresso >= 1.0) {
                // Classe CSS para barra concluída — cor definida nos arquivos de tema
                progressBar.getStyleClass().removeAll("progress-bar-meta-ativa");
                if (!progressBar.getStyleClass().contains("progress-bar-meta-concluida")) {
                    progressBar.getStyleClass().add("progress-bar-meta-concluida");
                }
                percentLabel.setText("Meta concluida!");
                percentLabel.getStyleClass().removeAll("cell-subtitle");
                if (!percentLabel.getStyleClass().contains("cell-meta-concluida")) {
                    percentLabel.getStyleClass().add("cell-meta-concluida");
                }
            } else {
                // Classe CSS para barra em andamento
                progressBar.getStyleClass().removeAll("progress-bar-meta-concluida");
                if (!progressBar.getStyleClass().contains("progress-bar-meta-ativa")) {
                    progressBar.getStyleClass().add("progress-bar-meta-ativa");
                }
                percentLabel.setText(String.format("%.1f%% concluido", progresso * 100));
                percentLabel.getStyleClass().removeAll("cell-meta-concluida");
                if (!percentLabel.getStyleClass().contains("cell-subtitle")) {
                    percentLabel.getStyleClass().add("cell-subtitle");
                }
            }

            setGraphic(root);
        }
    }
}
