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

        // Container de conteudo
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
        progressBar.setStyle("-fx-accent: #4318FF; -fx-background-radius: 4px;");

        percentLabel = new Label();
        percentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #A3AED0;");

        container.getChildren().addAll(headerBox, progressBar, percentLabel);
        HBox.setHgrow(container, Priority.ALWAYS);

        // Root com icone + conteudo
        root = new HBox(15);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(4, 0, 4, 0));
        root.getChildren().addAll(icon, container);

        // Impede que a celula expanda horizontalmente causando scroll
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

            // Tenta carregar icone da meta, usa fallback se nao existir
            Image iconeImg = IconManager.getImage("/app/icons/categoria/meta.png");
            if (iconeImg != null) icon.setImage(iconeImg);

            if (progresso >= 1.0) {
                progressBar.setStyle("-fx-accent: #4ADE80; -fx-background-radius: 4px;");
                percentLabel.setText("Meta concluida!");
                percentLabel.getStyleClass().removeAll("cell-subtitle");
                percentLabel.getStyleClass().add("cell-meta-concluida");
            } else {
                progressBar.setStyle("-fx-accent: #4318FF; -fx-background-radius: 4px;");
                percentLabel.setText(String.format("%.1f%% concluido", progresso * 100));
                percentLabel.getStyleClass().removeAll("cell-meta-concluida");
                percentLabel.getStyleClass().add("cell-subtitle");
            }

            setGraphic(root);
        }
    }
}