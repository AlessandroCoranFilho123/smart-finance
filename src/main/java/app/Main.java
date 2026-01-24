package app;

import app.model.Categoria;
import app.model.Meta;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.service.PersistenciaService;
import app.service.TransacaoService;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class Main extends Application {
    private Set<String> parseTags(String texto) {
        if (texto == null || texto.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private final List<Transacao> transacoes = new ArrayList<>();
    private final List<Meta> metas = new ArrayList<>();

    private TransacaoService service;

    private BorderPane root;
    private ListView<Transacao> listaTransacoes;
    private VBox painelMetas;
    private Label lblSaldo;

    private ImageView themeIcon;
    private boolean darkMode;

    private static final Preferences prefs =
            Preferences.userNodeForPackage(Main.class);

    private static final NumberFormat nf =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private static final Logger LOGGER =
            Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> LOGGER.log(Level.SEVERE, "Erro não tratado", e)
        );

        carregarDados();
        service = new TransacaoService(transacoes, metas);

        root = new BorderPane();
        root.getStyleClass().add("root-pane");
        listaTransacoes = new ListView<>();
        painelMetas = new VBox(8);
        lblSaldo = new Label();

        root.setTop(criarHeader());
        root.setLeft(criarPainelMetas());
        root.setCenter(criarSecaoTransacoes());

        Scene scene = new Scene(root, 1600, 900);
        aplicarCss(scene);

        carregarIcone(stage, "/app/icons/app_64.png");


        restaurarTema();
        atualizarUI();

        stage.setTitle("Aplicativo de Finanças v1.1.7");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> salvarDados());
        stage.show();
    }

    private Node criarHeader() {

        Button btnNovaTransacao =
                criarBotao("Nova Transação", "btn-primary", this::novaTransacao);

        Button btnNovaMeta =
                criarBotao("Nova Meta", "btn-secondary", this::novaMeta);

        Button btnExcluir =
                criarBotao("Excluir", "btn-excluir", this::excluirTransacao);

        HBox botoes = new HBox(16, btnNovaTransacao, btnNovaMeta, btnExcluir);
        botoes.getStyleClass().add("header-buttons");
        botoes.setAlignment(Pos.CENTER);

        lblSaldo.getStyleClass().add("saldo-label");


        themeIcon = new ImageView();
        themeIcon.setFitWidth(18);
        themeIcon.setFitHeight(18);
        Button toggleTheme = new Button();
        toggleTheme.setGraphic(themeIcon);
        toggleTheme.getStyleClass().add("btn-icon");
        toggleTheme.setOnAction(e -> alternarTema());

        BorderPane header = new BorderPane();
        header.getStyleClass().add("header");

        header.setLeft(lblSaldo);
        header.setCenter(botoes);
        header.setRight(toggleTheme);

        BorderPane.setAlignment(lblSaldo, Pos.CENTER_LEFT);
        BorderPane.setAlignment(botoes, Pos.CENTER);
        BorderPane.setAlignment(toggleTheme, Pos.CENTER_RIGHT);

        return header;
    }

    private void novaTransacao() {
        Stage stage = new Stage();

        ComboBox<TipoTransacao> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll(TipoTransacao.values());

        ComboBox<Categoria> cbCategoria = new ComboBox<>();
        ComboBox<Meta> cbMeta = new ComboBox<>(FXCollections.observableArrayList(metas));

        TextField txtValor = new TextField();
        TextField txtComentario = new TextField();
        TextField txtTags = new TextField();

        cbMeta.visibleProperty().bind(
                Bindings.createBooleanBinding(
                        () -> cbCategoria.getValue() == Categoria.AdicionarMeta
                                || cbCategoria.getValue() == Categoria.RetirarMeta,
                        cbCategoria.valueProperty()
                )
        );

        cbTipo.valueProperty().addListener((obs, o, tipo) -> {
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
        salvar.getStyleClass().add("salvar-button");
        salvar.setOnAction(e -> {
            try {
                if (cbTipo.getValue() == null || cbCategoria.getValue() == null) {
                    alerta("Selecione tipo e categoria");
                    return;
                }

                if (txtValor.getText().isBlank()) {
                    alerta("Informe um valor");
                    return;
                }

                String raw = txtValor.getText()
                        .replace("R$", "")
                        .replace(".", "")
                        .replace(",", ".")
                        .trim();

                BigDecimal valor = new BigDecimal(raw);
                if (valor.signum() <= 0) {
                    alerta("Valor deve ser maior que zero");
                    return;
                }

                if ((cbCategoria.getValue() == Categoria.AdicionarMeta
                        || cbCategoria.getValue() == Categoria.RetirarMeta)
                        && cbMeta.getValue() == null) {
                    alerta("Selecione uma meta");
                    return;
                }

                long centavos = valor.setScale(2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .longValueExact();

                service.registrar(
                        cbTipo.getValue(),
                        cbCategoria.getValue(),
                        centavos,
                        cbMeta.getValue(),
                        txtComentario.getText(),
                        parseTags(txtTags.getText())
                );

                salvarDados();
                atualizarUI();
                stage.close();

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Erro ao salvar transação", ex);
                alerta(ex.getMessage());
            }
        });

        HBox actions = new HBox(salvar);
        actions.setAlignment(Pos.CENTER);

        VBox layout = new VBox(10,
                new Label("Tipo"), cbTipo,
                new Label("Valor"), txtValor,
                new Label("Categoria"), cbCategoria,
                new Label("Meta"), cbMeta,
                new Label("Comentário"), txtComentario,
                new Label("Tags"), txtTags,
                actions
        );

        VBox wrapper = new VBox(layout);
        wrapper.setPadding(new Insets(18));
        wrapper.getStyleClass().add("dialog-root");
        wrapper.getStyleClass().add(darkMode ? "dark" : "light");

        Scene scene = new Scene(wrapper, 380, 520);
        aplicarCss(scene);
        carregarIcone(stage, "/app/icons/transaction_64.png");


        stage.setScene(scene);
        stage.setTitle("Nova Transação");
        stage.show();
    }


    private void novaMeta() {
        Stage stage = new Stage();
        TextField nome = new TextField();
        TextField alvo = new TextField();
        Button salvar = new Button("Salvar");
        salvar.getStyleClass().add("salvar-button");
        salvar.setOnAction(e -> {
            try {
                Long alvoCentavos = alvo.getText().isBlank() ? null : Math.round(Double.parseDouble(alvo.getText()) * 100);
                metas.add(new Meta(nome.getText(), alvoCentavos));
                salvarDados();
                atualizarUI();
                stage.close();
            } catch (Exception ex) {
                alerta("Dados inválidos");
            }
        });

        HBox actions = new HBox(salvar);
        actions.setAlignment(Pos.CENTER);

        VBox layout = new VBox(10,
                new Label("Nome da Meta"),
                nome,
                new Label("Valor Alvo"),
                alvo,
                actions
        );

        VBox wrapper = new VBox(layout);
        wrapper.setPadding(new Insets(18));
        wrapper.getStyleClass().add("dialog-root");
        wrapper.getStyleClass().add(darkMode ? "dark" : "light");

        Scene scene = new Scene(wrapper, 300, 260);

        aplicarCss(scene);
        carregarIcone(stage, "/app/icons/meta_64.png");


        stage.setScene(scene);
        stage.setTitle("Nova Meta");
        stage.show();
    }

    private void abrirDetalhesTransacao(Transacao t) {
        Stage stage = new Stage();

        TextArea comentario = new TextArea(t.getComentario());
        comentario.setWrapText(true);

        Button salvar = new Button("Salvar Comentário");
        salvar.getStyleClass().add("salvar-button");
        salvar.setOnAction(e -> {
            t.setComentario(comentario.getText());
            salvarDados();
            listaTransacoes.refresh();
            stage.close();
        });

        HBox actions = new HBox(salvar);
        actions.setAlignment(Pos.CENTER);

        VBox layout = new VBox(10,
                new Label("Tipo: " + t.getTipo()),
                new Label("Categoria: " + t.getCategoria()),
                new Label("Valor: " + nf.format(t.getValorCentavos() / 100.0)),
                new Label("Tags: " + t.getTags()),
                new Label("Meta: " + t.getMetaNome()),
                new Label("Comentário"),
                comentario,
                actions
        );

        VBox wrapper = new VBox(layout);
        wrapper.setPadding(new Insets(18));
        wrapper.getStyleClass().add("dialog-root");
        wrapper.getStyleClass().add(darkMode ? "dark" : "light");

        Scene scene = new Scene(wrapper, 420, 420);

        aplicarCss(scene);
        carregarIcone(stage, "/app/icons/details_64.png");


        stage.setScene(scene);
        stage.setTitle("Detalhes");
        stage.show();
    }

    private Node criarSecaoTransacoes() {

        listaTransacoes.setCellFactory(list -> new ListCell<>() {

            private final VBox box = new VBox(4);
            private final Label titulo = new Label();
            private final Label valor = new Label();
            private final Label extra = new Label();

            {
                titulo.getStyleClass().add("transacao-titulo");
                valor.getStyleClass().add("transacao-valor");
                extra.getStyleClass().add("transacao-extra");
                box.getChildren().addAll(titulo, valor, extra);
                box.getStyleClass().add("transacao-item");
                box.setPadding(new Insets(10));
            }

            @Override
            protected void updateItem(Transacao t, boolean empty) {
                super.updateItem(t, empty);

                if (empty || t == null) {
                    setGraphic(null);
                    return;
                }

                titulo.setText(
                        t.getNome().isBlank()
                                ? t.getCategoria().name()
                                : t.getNome()
                );

                valor.setText(
                        nf.format(t.getValorCentavos() / 100.0)
                );

                extra.setText(
                        (t.getMetaNome().isBlank() ? "" : "Meta: " + t.getMetaNome()) +
                                (t.getComentario().isBlank() ? "" : " • " + t.getComentario())
                );

                setGraphic(box);

                setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        abrirDetalhesTransacao(t);
                    }
                });
            }
        });

        VBox container = new VBox(
                14,
                criarTituloSecao("Transações"),
                listaTransacoes
        );
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

        VBox container = new VBox(
                14,
                criarTituloSecao("Metas"),
                scroll
        );
        container.getStyleClass().add("section");
        return container;
    }

    private void atualizarUI() {

        listaTransacoes.getItems().setAll(transacoes);
        painelMetas.getChildren().clear();

        for (Meta m : metas) {

            Label nome = new Label(m.getNome());
            Label valor = new Label(
                    nf.format(m.getAtualCentavos() / 100.0)
            );

            double progresso = Math.max(0, Math.min(1, m.progresso()));
            ProgressBar pb = new ProgressBar(progresso);

            Button excluir = new Button("Excluir");
            excluir.getStyleClass().add("btn-excluir");
            excluir.setOnAction(e -> {
                service.excluirMeta(m);
                salvarDados();
                atualizarUI();
            });

            VBox card = new VBox(6, nome, valor, pb, excluir);
            card.getStyleClass().add("meta-card");
            card.setPadding(new Insets(10));
            card.setMaxWidth(Double.MAX_VALUE);

            painelMetas.getChildren().add(card);
        }

        lblSaldo.setText(
                "Saldo disponível: " +
                        nf.format(service.calcularSaldoDisponivelCentavos() / 100.0)
        );
    }

    private void excluirTransacao() {
        int index = listaTransacoes.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            alerta("Selecione uma transação");
            return;
        }

        service.excluirTransacao(index);
        salvarDados();
        atualizarUI();
    }

    private void alternarTema() {
        darkMode = !darkMode;
        aplicarTema();
        prefs.putBoolean("darkMode", darkMode);
    }

    private void restaurarTema() {
        darkMode = prefs.getBoolean("darkMode", false);
        aplicarTema();
    }

    private void aplicarTema() {
        root.getStyleClass().removeAll("light", "dark");
        root.getStyleClass().add(darkMode ? "dark" : "light");

        themeIcon.setImage(
                carregarImagem(
                        darkMode
                                ? "/app/icons/sun_24.png"
                                : "/app/icons/moon_24.png"
                )
        );
    }

    private Button criarBotao(String texto, String classe, Runnable acao) {
        Button b = new Button(texto);
        b.getStyleClass().add(classe);
        b.setOnAction(e -> acao.run());
        return b;
    }

    private Label criarTituloSecao(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("section-title");
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private void aplicarCss(Scene scene) {
        URL css = Main.class.getResource("/app/style/app.css");
        if (css == null) {
            throw new RuntimeException("CSS não encontrado");
        }
        scene.getStylesheets().add(css.toExternalForm());
    }

    private void salvarDados() {
        synchronized (this) {
            PersistenciaService.salvarTransacoes(transacoes);
            PersistenciaService.salvarMetas(metas);
        }
    }

    private void carregarDados() {
        try {
            transacoes.addAll(PersistenciaService.carregarTransacoes());
            metas.addAll(PersistenciaService.carregarMetas());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Falha ao carregar dados", e);
        }
    }

    private Image carregarImagem(String path) {
        return new Image(
                Objects.requireNonNull(
                        getClass().getResourceAsStream(path)
                )
        );
    }

    private void carregarIcone(Stage stage, String path) {
        URL iconUrl = Main.class.getResource(path);
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
    }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
