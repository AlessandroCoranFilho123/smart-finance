package app.controller;

import app.model.Meta;
import app.model.Transacao;
import app.repository.MetaDAO;
import app.repository.TransacaoDAO;
import app.service.MetaService;
import app.service.TransacaoService;
import app.util.CssManager;
import app.util.IconManager;
import app.view.MetaCell;
import app.view.TransacaoCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/* Gerencia a navegação entre telas,
   carrega os dados no dashboard,
   abre diálogos e alterna temas (claro/escuro) */

@SuppressWarnings("unused")
public class MainController {
    // Usado para formatar números padrão Brasil
    private static final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    // Registrar avisos e erros
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private BorderPane rootContainer;
    @FXML
    private Button btnInicio;
    @FXML
    private Button btnTransacao;
    @FXML
    private Button btnMetas;
    @FXML
    private Button btnTema;
    @FXML
    private ImageView imgTema;
    @FXML
    private Label lblTema;
    @FXML
    private Label lblSaldo;
    @FXML
    private Label lblReceitasMes;
    @FXML
    private Label lblDespesasMes;
    @FXML
    private ListView<Transacao> listTransacoes;
    @FXML
    private Button btnNovaTransacao;
    @FXML
    private Button btnVerTodasTransacoes;
    @FXML
    private ListView<Meta> listMetas;
    @FXML
    private Button btnNovaMeta;
    @FXML
    private Button btnVerTodasMetas;

    private TransacaoService transacaoService;
    private MetaService metaService;
    private boolean darkTheme = false;
    private static final Preferences PREFS = Preferences.userNodeForPackage(MainController.class);

    @FXML
    public void initialize() {
        try {
            inicializarServicos(); // Cria DAO, MetaService e TransacaoService
            configurarCelulasCustomizadas(); // Define o visual personalizado para os itens das listas
            configurarEventos(); // Configurar ações dos botões
            carregarDados(); // Carrega o DB
            marcarBotaoAtivo(btnInicio); // Botão início selecionado
            aplicarTemaSalvo(); // Restaura tema da sessão anterior

        } catch (Exception e) {
            logger.error("Erro ao inicializar MainController: {}", e.getMessage());
        }
    }

    // Cria TransacaoDAO, MetaDAO, MetaService e TransacaoService
    private void inicializarServicos() {
        TransacaoDAO transacaoDAO = new TransacaoDAO();
        MetaDAO metaDAO = new MetaDAO();

        transacaoService = new TransacaoService(transacaoDAO, metaDAO);
        metaService = new MetaService(metaDAO);
    }

    // Define o visual personalizado para os itens das listas.
    private void configurarCelulasCustomizadas() {
        listTransacoes.setCellFactory(lv -> new TransacaoCell());
        listMetas.setCellFactory(lv -> new MetaCell());
    }

    //  Configura todos os botões
    private void configurarEventos() {
        btnInicio.setOnAction(e -> navegarParaInicio());
        btnTema.setOnAction(e -> alternarTema());

        btnTransacao.setOnAction(e -> navegarParaTransacoes());
        btnNovaTransacao.setOnAction(e -> abrirDialogNovaTransacao());
        btnVerTodasTransacoes.setOnAction(e -> navegarParaTransacoes());

        btnMetas.setOnAction(e -> navegarParaMetas());
        btnNovaMeta.setOnAction(e -> abrirDialogNovaMeta());
        btnVerTodasMetas.setOnAction(e -> navegarParaMetas());

        // Duplo clique abre detalhes transações
        listTransacoes.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Transacao selecionada = listTransacoes.getSelectionModel().getSelectedItem();
                if (selecionada != null) {
                    abrirDetalhesTransacao(selecionada);
                }
            }
        });

        // Duplo clique abre detalhes metas
        listMetas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Meta selecionada = listMetas.getSelectionModel().getSelectedItem();
                if (selecionada != null) {
                    abrirDetalhesMeta(selecionada);
                }
            }
        });
    }

    public void carregarDados() {
        carregarSaldo();
        carregarTransacoesRecentes();
        carregarMetas();
    }

    private void carregarSaldo() {
        try {
            long saldoCentavos = transacaoService.calcularSaldoDisponivelCentavos();
            lblSaldo.setText(currencyFormatter.format(saldoCentavos / 100.0));

            YearMonth mesAtual = YearMonth.now();
            long receitasCentavos = transacaoService.calcularReceitasMes(mesAtual);
            long despesasCentavos = transacaoService.calcularDespesasMes(mesAtual);

            lblReceitasMes.setText(currencyFormatter.format(receitasCentavos / 100.0));
            lblDespesasMes.setText(currencyFormatter.format(despesasCentavos / 100.0));

        } catch (Exception e) {
            logger.error("Erro ao carregar saldo: {}", e.getMessage());
            lblSaldo.setText("R$ 0,00");
        }
    }

    private void carregarTransacoesRecentes() {
        try {
            TransacaoDAO dao = new TransacaoDAO();
            List<Transacao> lista = dao.listarRecentes(10);

            ObservableList<Transacao> transacoes = FXCollections.observableArrayList(lista);
            listTransacoes.setItems(transacoes);

        } catch (Exception e) {
            logger.error("Erro ao carregar transações: {}", e.getMessage());
        }
    }

    private void carregarMetas() {
        try {
            List<Meta> lista = metaService.listarTodasMetas();

            ObservableList<Meta> metas = FXCollections.observableArrayList(lista);
            listMetas.setItems(metas);

        } catch (Exception e) {
            logger.error("Erro ao carregar metas: {}", e.getMessage());
        }
    }

    // Tela inicial (main.fxml)
    private void navegarParaInicio() {
        marcarBotaoAtivo(btnInicio);
        carregarDados();
    }

    // Tela transações (transacoes_view.fxml)
    private void navegarParaTransacoes() {
        marcarBotaoAtivo(btnTransacao);
        abrirViewTransacoes();
    }

    // Tela metas (metas_view.fxml)
    private void navegarParaMetas() {
        marcarBotaoAtivo(btnMetas);
        abrirViewMetas();
    }

    // Janela de transações acessada pela sidebar
    private void abrirViewTransacoes() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/transacoes_view.fxml")
            );

            VBox dialogRoot = loader.load();
            TransacoesViewController controller = loader.getController();

            controller.setOnNovaTransacao(this::abrirDialogNovaTransacao);

            Stage stage = new Stage();
            stage.setTitle("Transações");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(btnTransacao.getScene().getWindow());

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);

            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }

            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            IconManager.setTransacaoIcon(stage);

            stage.showAndWait();

            carregarDados();

        } catch (Exception e) {
            logger.error("Erro ao abrir view de transações: {}", e.getMessage());
        }
    }

    // Janela de metas acessada pela sidebar
    private void abrirViewMetas() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/metas_view.fxml")
            );

            VBox dialogRoot = loader.load();
            MetasViewController controller = loader.getController();

            controller.setOnNovaMeta(this::abrirDialogNovaMeta);
            controller.setOnEditarMeta(this::abrirDetalhesMeta);

            Stage stage = new Stage();
            stage.setTitle("Metas");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(btnMetas.getScene().getWindow());

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);

            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }

            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            IconManager.setMetaIcon(stage);

            stage.showAndWait();

            carregarDados();

        } catch (Exception e) {
            logger.error("Erro ao abrir view de metas: {}", e.getMessage());

        }
    }

    // Quando botão for clicado, fica destacado
    private void marcarBotaoAtivo(Button botaoAtivo) {
        btnInicio.getStyleClass().remove("active");
        btnTransacao.getStyleClass().remove("active");
        btnMetas.getStyleClass().remove("active");

        if (!botaoAtivo.getStyleClass().contains("active")) {
            botaoAtivo.getStyleClass().add("active");
        }
    }

    @FXML
    private void aplicarTemaSalvo() {
        darkTheme = PREFS.getBoolean("darkTheme", false);
        if (darkTheme) {
            if (rootContainer != null) {
                rootContainer.getStyleClass().add("dark-theme");
            }
            if (imgTema != null) {
                javafx.scene.image.Image icone = IconManager.getImage("/app/icons/tema/claro.png");
                if (icone != null) imgTema.setImage(icone);
            }
            if (lblTema != null) lblTema.setText("Tema claro");
        }
    }

    private void alternarTema() {
        darkTheme = !darkTheme;
        PREFS.putBoolean("darkTheme", darkTheme); // salva preferência

        if (rootContainer != null) {
            if (darkTheme) {
                rootContainer.getStyleClass().add("dark-theme");
            } else {
                rootContainer.getStyleClass().remove("dark-theme");
                rootContainer.setStyle("");
            }
        }

        if (imgTema != null) { // Altera entre img do sol e da lua
            String path = darkTheme ? "/app/icons/tema/claro.png" : "/app/icons/tema/escuro.png";
            javafx.scene.image.Image icone = IconManager.getImage(path);
            if (icone != null) imgTema.setImage(icone);
        }

        if (lblTema != null) {
            lblTema.setText(darkTheme ? "Tema claro" : "Tema escuro");
        }
    }

    // Janela aberta a partir de botão "Nota Transação"
    private void abrirDialogNovaTransacao() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/transacao_dialog.fxml")
            );

            VBox dialogRoot = loader.load();
            TransacaoDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Nova Transação");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(btnNovaTransacao.getScene().getWindow());

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);

            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }

            dialogStage.setScene(scene);
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(520);
            dialogStage.setMinHeight(650);
            dialogStage.setMaxWidth(600);
            IconManager.setTransacaoIcon(dialogStage);

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            logger.error("Erro ao abrir dialog de transação: {}", e.getMessage());
        }
    }

    // Janela duplo clique em uma transação
    private void abrirDetalhesTransacao(Transacao transacao) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/transacao_dialog.fxml")
            );

            VBox dialogRoot = loader.load();
            TransacaoDialogController controller = loader.getController();

            controller.configurarParaEditar(transacao);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Detalhes da Transação");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(listTransacoes.getScene().getWindow());

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);

            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }

            dialogStage.setScene(scene);
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(520);
            dialogStage.setMinHeight(500);
            dialogStage.setMaxWidth(600);
            IconManager.setDetalheIcon(dialogStage);

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            logger.error("Erro ao abrir detalhes da transação: {}", e.getMessage());
        }
    }

    // Janela aberta a partir de botão "Nota Meta"
    private void abrirDialogNovaMeta() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/meta_dialog.fxml")
            );

            VBox dialogRoot = loader.load();
            MetaDialogController controller = loader.getController();

            controller.configurarParaCriar();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Nova Meta");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(btnNovaMeta.getScene().getWindow());

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);

            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.setMinWidth(480);
            dialogStage.setMinHeight(380);
            IconManager.setMetaIcon(dialogStage);

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            logger.error("Erro ao abrir dialog de meta: {}", e.getMessage());
        }
    }

    // Janela duplo clique em uma meta
    private void abrirDetalhesMeta(Meta meta) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/meta_dialog.fxml")
            );

            VBox dialogRoot = loader.load();
            MetaDialogController controller = loader.getController();

            controller.configurarParaEditar(meta);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Editar Meta");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(listMetas.getScene().getWindow());

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);

            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }

            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.setMinWidth(480);
            dialogStage.setMinHeight(380);
            IconManager.setMetaIcon(dialogStage);

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            logger.error("Erro ao abrir detalhes da meta: {}", e.getMessage());
        }
    }
}