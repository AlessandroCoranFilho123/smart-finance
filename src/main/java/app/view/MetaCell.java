package app.view;

import app.model.Meta;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MetaCell extends ListCell<Meta> {

    private final VBox container;
    private final HBox headerBox;
    private final Label nomeLabel;
    private final Label valorLabel;
    private final ProgressBar progressBar;
    private final Label percentLabel;
    private final Region spacer;

    public MetaCell() {
        // Container principal
        container = new VBox(10);
        container.setPadding(new Insets(15));

        // Header (nome + valor)
        headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);

        nomeLabel = new Label();
        nomeLabel.setStyle(
                "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-text-fill: #1B2559;"
        );

        spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        valorLabel = new Label();
        valorLabel.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: #A3AED0;"
        );

        headerBox.getChildren().addAll(nomeLabel, spacer, valorLabel);

        // Barra de progresso
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(8);
        progressBar.setStyle(
                "-fx-accent: #4318FF; " +
                        "-fx-background-radius: 4px;"
        );

        percentLabel = new Label();
        percentLabel.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: #A3AED0;"
        );

        container.getChildren().addAll(headerBox, progressBar, percentLabel);
    }

    @Override
    protected void updateItem(Meta meta, boolean empty) {
        super.updateItem(meta, empty);

        if (empty || meta == null) {
            setGraphic(null);
            setText(null);
        } else {
            // Nome da meta
            nomeLabel.setText(meta.getNome());

            // Valores (atual / alvo)
            double atual = meta.getAtualCentavos() / 100.0;
            double alvo = meta.getAlvoCentavos() / 100.0;

            valorLabel.setText(
                    String.format("R$ %.2f / R$ %.2f", atual, alvo)
            );

            // Progresso
            double progresso = meta.progresso();
            progressBar.setProgress(progresso);

            // Porcentagem
            double percentual = progresso * 100;
            percentLabel.setText(String.format("%.1f%% concluído", percentual));

            // Mudar cor da barra se meta concluída
            if (progresso >= 1.0) {
                progressBar.setStyle(
                        "-fx-accent: #4ADE80; " + // Verde
                                "-fx-background-radius: 4px;"
                );
                percentLabel.setText("Meta concluída!");
                percentLabel.setStyle(
                        "-fx-font-size: 12px; " +
                                "-fx-text-fill: #4ADE80; " + // Verde
                                "-fx-font-weight: bold;"
                );
            }

            setGraphic(container);
        }
    }
}