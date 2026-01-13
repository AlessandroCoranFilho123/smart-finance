package app;

import app.model.*;
import app.service.PersistenciaService;
import app.service.TransacaoService;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Main extends Application {

    private final List<Transacao> transacoes = new ArrayList<>();
    private final List<Meta> metas = new ArrayList<>();
    private TransacaoService service;
    private final ListView<Transacao> listaTransacoes = new ListView<>();
    private final VBox painelMetas = new VBox(10);
    private final Label lblSaldo = new Label();
    private final NumberFormat nf = NumberFormat.getCurrencyInstance();

    @Override
    public void start(Stage stage) {

        transacoes.addAll(PersistenciaService.carregarTransacoes());
        metas.addAll(PersistenciaService.carregarMetas());

        service = new TransacaoService(transacoes, metas);

        Button btnNovaTransacao = new Button("Nova Transação");
        Button btnNovaMeta = new Button("Nova Meta");
        Button btnExcluirTransacao = new Button("Excluir Transação");

        btnExcluirTransacao.getStyleClass().add("danger");

        btnNovaTransacao.setOnAction(_ -> novaTransacao());
        btnNovaMeta.setOnAction(_ -> novaMeta());
        btnExcluirTransacao.setOnAction(_ -> excluirTransacao());

        HBox barraBotoes = new HBox(10,
                btnNovaTransacao,
                btnNovaMeta,
                btnExcluirTransacao
        );
        barraBotoes.setAlignment(Pos.CENTER);

        lblSaldo.getStyleClass().add("subtitle");

        lblSaldo.setAlignment(Pos.CENTER);
        lblSaldo.setMaxWidth(Double.MAX_VALUE);

        VBox painelStatus = new VBox(6,
                lblSaldo
        );
        painelStatus.setAlignment(Pos.CENTER);

        listaTransacoes.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {

                Transacao t = listaTransacoes.getSelectionModel().getSelectedItem();
                if (t == null) return;

                Label lblNome = new Label(t.getNome());
                Label lblTipo = new Label("Tipo: " + t.getTipo());
                Label lblCategoria = new Label("Categoria: " + t.getCategoria());
                Label lblValor = new Label(
                        "Valor: " + nf.format(t.getValorCentavos() / 100.0)
                );
                Label lblTags = new Label("Tags: " + t.getTags());

                if (t.temMeta()) {
                    lblCategoria.setText(
                            lblCategoria.getText() + " (Meta: " + t.getMetaNome() + ")"
                    );
                }
                lblNome.getStyleClass().add("title");

                TextArea areaComentario = new TextArea(t.getComentario());
                areaComentario.setWrapText(true);
                areaComentario.setPrefRowCount(5);

                VBox conteudo = new VBox(10,
                        lblNome,
                        lblTipo,
                        lblCategoria,
                        lblValor,
                        lblTags,
                        new Separator(),
                        new Label("Comentário"),
                        areaComentario
                );
                conteudo.setPadding(new Insets(10));

                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Detalhes da Transação");
                dialog.getDialogPane().setContent(conteudo);

                dialog.getDialogPane().getButtonTypes().addAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );

                dialog.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) {

                        service.editarComentario(t, areaComentario.getText());
                        PersistenciaService.salvarTransacoes(transacoes);
                        atualizarUI();
                    }
                });
            }
        });

        listaTransacoes.setCellFactory(_ -> new ListCell<>() {

            @Override
            protected void updateItem(Transacao t, boolean empty) {
                super.updateItem(t, empty);

                if (empty || t == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                String valor = nf.format(t.getValorCentavos() / 100.0);

                String titulo = t.getNome().isBlank()
                        ? t.getCategoria().name()
                        : t.getNome();

                String meta = t.getMetaNome().isBlank()
                        ? ""
                        : " → Meta: " + t.getMetaNome();


                setText(
                        titulo + "\n" +
                                valor + meta +
                                (t.getComentario().isBlank() ? "" : " " + t.getComentario())
                );

            }
        });

        VBox secaoTransacoes = new VBox(6,
                new Label("Transações"),
                listaTransacoes
        );
        secaoTransacoes.getStyleClass().add("section");

        Label tituloMetas = new Label("Metas");
        tituloMetas.getStyleClass().add("subtitle");

        painelMetas.setSpacing(10);
        ScrollPane scrollMetas = new ScrollPane(painelMetas);

        scrollMetas.setFitToWidth(true);
        scrollMetas.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollMetas.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scrollMetas.setPrefHeight(305);
        scrollMetas.setMaxHeight(300);

        VBox secaoMetas = new VBox(6,
                tituloMetas,
                scrollMetas
        );

        secaoMetas.getStyleClass().add("section");




        VBox layout = new VBox(14,
                barraBotoes,
                painelStatus,
                secaoMetas,
                secaoTransacoes
        );
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-padding:20;");

        atualizarUI();

        Scene scene = new Scene(layout, 1600, 900);
        stage.setMaximized(true);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass()
                                .getResource("/app/style/app.css"))
                        .toExternalForm()
        );

        stage.setTitle("Aplicativo de Finanças");
        stage.setScene(scene);
        stage.show();
    }

    private void novaTransacao() {

        Stage stage = new Stage();

        ComboBox<TipoTransacao> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll(TipoTransacao.values());

        ComboBox<Categoria> cbCategoria = new ComboBox<>();
        ComboBox<Meta> cbMeta = new ComboBox<>();
        cbMeta.getItems().addAll(metas);

        TextField txtValor = new TextField();
        TextField txtComentario = new TextField();
        TextField txtTags = new TextField();

        cbMeta.visibleProperty().bind(
                cbCategoria.valueProperty().isEqualTo(Categoria.AdicionarMeta)
                        .or(cbCategoria.valueProperty().isEqualTo(Categoria.RetirarMeta))
        );
        cbMeta.managedProperty().bind(cbMeta.visibleProperty());

        cbTipo.valueProperty().addListener((_, _, tipo) -> {
            cbCategoria.getItems().clear();
            if (tipo == TipoTransacao.Entrada) {
                cbCategoria.getItems().addAll(
                        Categoria.Salario,
                        Categoria.Emprestimo,
                        Categoria.AdicionarMeta,
                        Categoria.Outros
                );
            } else {
                cbCategoria.getItems().addAll(
                        Categoria.Aluguel,
                        Categoria.Alimentacao,
                        Categoria.Internet,
                        Categoria.Agua,
                        Categoria.Luz,
                        Categoria.Compras,
                        Categoria.RetirarMeta,
                        Categoria.Outros
                );
            }
        });

        Button salvar = new Button("Salvar");
        salvar.setOnAction(_ -> {
            try {

                if (cbTipo.getValue() == null || cbCategoria.getValue() == null) {
                    alerta("Selecione tipo e categoria");
                    return;
                }

                if (txtValor.getText().isBlank()) {
                    alerta("Informe um valor");
                    return;
                }

                BigDecimal valor = new BigDecimal(
                        txtValor.getText().replace(",", ".")
                );

                long centavos = valor
                        .setScale(2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .longValueExact();

                Set<String> tags = txtTags.getText().isBlank()
                        ? Set.of()
                        : Arrays.stream(txtTags.getText().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());

                service.registrar(
                        cbTipo.getValue(),
                        cbCategoria.getValue(),
                        centavos,
                        cbMeta.getValue(),
                        txtComentario.getText(),
                        tags
                );

                PersistenciaService.salvarTransacoes(transacoes);
                PersistenciaService.salvarMetas(metas);

                atualizarUI();
                stage.close();

            } catch (Exception e) {
                alerta(e.getMessage());
            }
        });

        VBox layout = new VBox(10,
                new Label("Tipo"), cbTipo,
                new Label("Valor"), txtValor,
                new Label("Categoria"), cbCategoria,
                new Label("Meta"), cbMeta,
                new Label("Comentário"), txtComentario,
                new Label("Tags (separadas por vírgula)"), txtTags,
                salvar
        );

        layout.setStyle("-fx-padding:20;");
        stage.setScene(new Scene(layout, 380, 520));
        stage.show();
    }

    private void novaMeta() {

        Stage s = new Stage();

        TextField nome = new TextField();
        TextField alvo = new TextField();

        Button salvar = new Button("Salvar");
        salvar.setOnAction(_ -> {
            try {
                Long alvoCentavos = alvo.getText().isBlank()
                        ? null
                        : Math.round(Double.parseDouble(alvo.getText()) * 100);

                metas.add(new Meta(nome.getText(), alvoCentavos));

                PersistenciaService.salvarMetas(metas);

                atualizarUI();
                s.close();

            } catch (Exception e) {
                alerta("Dados inválidos");
            }
        });

        s.setScene(new Scene(new VBox(10,
                new Label("Nome da Meta"), nome,
                new Label("Valor Alvo"), alvo,
                salvar
        ), 300, 260));
        s.show();
    }

    private void excluirTransacao() {
        int index = listaTransacoes.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            alerta("Selecione uma transação");
            return;
        }
        service.excluirTransacao(index);

        PersistenciaService.salvarTransacoes(transacoes);
        PersistenciaService.salvarMetas(metas);

        atualizarUI();
    }

    private void atualizarUI() {

        listaTransacoes.getItems().setAll(transacoes);

        painelMetas.getChildren().clear();
        for (Meta m : metas) {

            Label nome = new Label(m.getNome());
            Label valor = new Label(nf.format(m.getAtualCentavos() / 100.0));

            ProgressBar pb = new ProgressBar(m.progresso());
            pb.setMaxWidth(Double.MAX_VALUE);

            Button excluir = new Button("Excluir");
            excluir.setOnAction(_ -> {
                service.excluirMeta(m);

                PersistenciaService.salvarMetas(metas);
                PersistenciaService.salvarTransacoes(transacoes);

                atualizarUI();
            });


            VBox card = new VBox(6, nome, valor, pb, excluir);
            card.getStyleClass().add("meta-card");

            painelMetas.getChildren().add(card);
        }

        lblSaldo.setText("Saldo disponível: " +
                nf.format(service.calcularSaldoDisponivelCentavos() / 100.0));
    }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}
