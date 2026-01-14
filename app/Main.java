package app;

import app.model.*;
import app.service.PersistenciaService;
import app.service.TransacaoService;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
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
    private final VBox painelMetas = new VBox(12);
    private final Label lblSaldo = new Label();

    private final NumberFormat nf = NumberFormat.getCurrencyInstance();

    @Override
    public void start(Stage stage) {

        transacoes.addAll(PersistenciaService.carregarTransacoes());
        metas.addAll(PersistenciaService.carregarMetas());

        service = new TransacaoService(transacoes, metas);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(criarHeader());
        root.setLeft(criarPainelMetas());
        root.setCenter(criarSecaoTransacoes());

        atualizarUI();

        Scene scene = new Scene(root, 1100, 650);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/app/style/app.css")
                ).toExternalForm()
        );

        stage.setScene(scene);
        stage.setTitle("Aplicativo de Finanças");
        stage.getIcons().add(
                new Image(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream("/app/icons/app.png")
                        )
                )
        );

        stage.show();
    }

    private Node criarHeader() {

        Button btnNovaTransacao = new Button("Nova Transação");
        Button btnNovaMeta = new Button("Nova Meta");
        Button btnExcluir = new Button("Excluir Transação");

        btnNovaTransacao.getStyleClass().add("btn-primary");
        btnNovaMeta.getStyleClass().add("btn-secondary");
        btnExcluir.getStyleClass().add("btn-danger-header");
        btnExcluir.getStyleClass().add("danger");

        btnNovaTransacao.setOnAction(_ -> novaTransacao());
        btnNovaMeta.setOnAction(_ -> novaMeta());
        btnExcluir.setOnAction(_ -> excluirTransacao());

        HBox botoes = new HBox(10, btnNovaTransacao, btnNovaMeta, btnExcluir);
        botoes.setAlignment(Pos.CENTER);

        lblSaldo.getStyleClass().add("saldo-label");
        lblSaldo.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);

        GridPane header = new GridPane();
        header.getStyleClass().add("header");

        // 3 colunas: esquerda | centro | direita
        ColumnConstraints colLeft = new ColumnConstraints();
        colLeft.setHgrow(Priority.ALWAYS);

        ColumnConstraints colCenter = new ColumnConstraints();
        colCenter.setHgrow(Priority.NEVER);

        ColumnConstraints colRight = new ColumnConstraints();
        colRight.setHgrow(Priority.ALWAYS);

        header.getColumnConstraints().addAll(colLeft, colCenter, colRight);

        // adiciona elementos
        header.add(lblSaldo, 0, 0);
        header.add(botoes, 1, 0);

        // alinhamentos verticais
        GridPane.setValignment(lblSaldo, VPos.CENTER);
        GridPane.setValignment(botoes, VPos.CENTER);

        return header;

    }

    private void abrirDetalhesTransacao(Transacao t) {

        Stage stage = new Stage();
        stage.setTitle("Detalhes da Transação");

        Label nome = new Label("Nome: " +
                (t.getNome().isBlank() ? "-" : t.getNome()));

        Label tipo = new Label("Tipo: " + t.getTipo());
        Label categoria = new Label("Categoria: " + t.getCategoria());
        Label valor = new Label("Valor: " +
                nf.format(t.getValorCentavos() / 100.0));

        Label tags = new Label("Tags: " +
                (t.getTags().isEmpty()
                        ? "-"
                        : String.join(", ", t.getTags())));

        TextArea comentario = new TextArea(t.getComentario());
        comentario.setWrapText(true);

        Button salvar = new Button("Salvar Comentário");
        salvar.setOnAction(_ -> {
            t.setComentario(comentario.getText());
            PersistenciaService.salvarTransacoes(transacoes);
            atualizarUI();
            stage.close();
        });

        VBox layout = new VBox(10,
                nome,
                tipo,
                categoria,
                valor,
                tags,
                new Label("Comentário"),
                comentario,
                salvar
        );
        layout.getStyleClass().add("dialog-form");

        Scene scene = new Scene(layout, 420, 420);
        stage.getIcons().add(
                new Image(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream("/app/icons/details.png")
                        )
                )
        );
        stage.setTitle("Detalhes da Transação");

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/app/style/app.css")
                ).toExternalForm()
        );

        stage.setScene(scene);
        stage.show();
    }

    private Node criarSecaoTransacoes() {

        listaTransacoes.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Transacao t, boolean empty) {
                super.updateItem(t, empty);

                if (empty || t == null) {
                    setGraphic(null);
                    setOnMouseClicked(null);
                    return;
                }

                Label titulo = new Label(
                        t.getNome().isBlank()
                                ? t.getCategoria().name()
                                : t.getNome()
                );
                titulo.getStyleClass().add("transacao-titulo");

                Label valor = new Label(
                        nf.format(t.getValorCentavos() / 100.0)
                );
                valor.getStyleClass().add("transacao-valor");

                String extraTexto =
                        (t.getMetaNome().isBlank() ? "" : "Meta: " + t.getMetaNome()) +
                                (t.getComentario().isBlank() ? "" : " • " + t.getComentario());

                Label extra = new Label(extraTexto);
                extra.getStyleClass().add("transacao-extra");

                VBox box = new VBox(4, titulo, valor, extra);
                box.getStyleClass().add("transacao-item");

                setGraphic(box);

                setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && !isEmpty()) {
                        abrirDetalhesTransacao(t);
                    }
                });
            }
        });

        Label titulo = new Label("Transações");
        titulo.getStyleClass().add("section-title");
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setAlignment(Pos.CENTER);

        VBox container = new VBox(8, titulo, listaTransacoes);
        container.getStyleClass().add("section");

        VBox.setVgrow(listaTransacoes, Priority.ALWAYS);
        return container;
    }

    private Node criarPainelMetas() {

        painelMetas.getStyleClass().add("metas-panel");
        painelMetas.setFillWidth(true);
        ScrollPane scroll = new ScrollPane(painelMetas);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(300);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Label titulo = new Label("Metas");
        titulo.getStyleClass().add("section-title");
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setAlignment(Pos.CENTER);

        VBox container = new VBox(8, titulo, scroll);
        container.getStyleClass().add("section");

        return container;
    }

    private void atualizarUI() {

        listaTransacoes.getItems().setAll(transacoes);
        painelMetas.getChildren().clear();

        for (Meta m : metas) {

            Label nome = new Label(m.getNome());
            nome.getStyleClass().add("meta-nome");

            Label valor = new Label(
                    nf.format(m.getAtualCentavos() / 100.0)
            );
            valor.getStyleClass().add("meta-valor");

            ProgressBar pb = new ProgressBar(m.progresso());

            Button excluir = new Button("Excluir");
            excluir.getStyleClass().add("danger");
            excluir.setOnAction(_ -> {
                service.excluirMeta(m);
                PersistenciaService.salvarMetas(metas);
                PersistenciaService.salvarTransacoes(transacoes);
                atualizarUI();
            });

            VBox card = new VBox(6, nome, valor, pb, excluir);
            card.getStyleClass().add("meta-card");
            card.setMaxWidth(Double.MAX_VALUE);

            painelMetas.getChildren().add(card);
        }

        lblSaldo.setText(
                "Saldo disponível:\n " +
                        nf.format(service.calcularSaldoDisponivelCentavos() / 100.0)
        );
        lblSaldo.setAlignment(Pos.CENTER);
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
        layout.getStyleClass().add("dialog-form");

        Scene scene = new Scene(layout, 380, 520);
        stage.getIcons().add(
                new Image(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream("/app/icons/transaction.png")
                        )
                )
        );
        stage.setTitle("Nova Transação");

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/app/style/app.css")
                ).toExternalForm()
        );

        stage.setScene(scene);
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

        VBox layout = new VBox(10,
                new Label("Nome da Meta"), nome,
                new Label("Valor Alvo"), alvo,
                salvar
        );
        layout.getStyleClass().add("dialog-form");

        Scene scene = new Scene(layout, 300, 260);
        s.getIcons().add(
                new Image(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream("/app/icons/meta.png")
                        )
                )
        );
        s.setTitle("Nova Meta");

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/app/style/app.css")
                ).toExternalForm()
        );

        s.setScene(scene);
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

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}
