package app.controller;

import app.model.Meta;
import app.model.Transacao;
import app.repository.MetaDAO;
import app.repository.TransacaoDAO;
import app.service.MetaService;
import app.service.TransacaoService;
import app.util.CssManager;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/* Gerencia a navegação entre telas,
carrega os dados no dashboard,
abre diálogos e alterna temas (claro/escuro) */

public class MainController {
    private static final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

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
    private ObservableList<Transacao> transacoes;
    private ObservableList<Meta> metas;
    private boolean darkTheme = false;

    @FXML
    public void initialize() {
        try {
            inicializarServicos(); // Cria DAO, MetaService e TransacaoService
            configurarCelulasCustomizadas(); // Inicializa as células Listview
            configurarEventos(); // Configurar ações dos botões
            carregarDados(); // Carrega o DB
            marcarBotaoAtivo(btnInicio); // Botão início selecionado

        } catch (Exception e) {
            System.err.println("Erro ao inicializar MainController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void inicializarServicos() {
        TransacaoDAO transacaoDAO = new TransacaoDAO();
        MetaDAO metaDAO = new MetaDAO();

        transacaoService = new TransacaoService(transacaoDAO, metaDAO);
        metaService = new MetaService(metaDAO);
    }

    private void configurarCelulasCustomizadas() {
        listTransacoes.setCellFactory(lv -> new TransacaoCell());
        listMetas.setCellFactory(lv -> new MetaCell());
    }

    private void configurarEventos() {
        btnInicio.setOnAction(e -> navegarParaInicio());
        btnTema.setOnAction(e -> alternarTema());

        btnTransacao.setOnAction(e -> navegarParaTransacoes());
        btnNovaTransacao.setOnAction(e -> abrirDialogNovaTransacao());
        btnVerTodasTransacoes.setOnAction(e -> navegarParaTransacoes());

        btnMetas.setOnAction(e -> navegarParaMetas());
        btnNovaMeta.setOnAction(e -> abrirDialogNovaMeta());
        btnVerTodasMetas.setOnAction(e -> navegarParaMetas());

        listTransacoes.setOnMouseClicked(e -> { // Duplo clique abre detalhes transações
            if (e.getClickCount() == 2) {
                Transacao selecionada = listTransacoes.getSelectionModel().getSelectedItem();
                if (selecionada != null) {
                    abrirDetalhesTransacao(selecionada);
                }
            }
        });

        listMetas.setOnMouseClicked(e -> { // Duplo clique abre detalhes metas
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
            double saldo = saldoCentavos / 100.0;

            lblSaldo.setText(currencyFormatter.format(saldo));

            // Futuramente vou implementar o cálculo de receitas/despesas do mês
        } catch (Exception e) {
            System.err.println("Erro ao carregar saldo: " + e.getMessage());
            lblSaldo.setText("R$ 0,00");
        }
    }

    private void carregarTransacoesRecentes() {
        try {
            TransacaoDAO dao = new TransacaoDAO();
            List<Transacao> lista = dao.listarRecentes(10);

            transacoes = FXCollections.observableArrayList(lista);
            listTransacoes.setItems(transacoes);

        } catch (Exception e) {
            System.err.println("Erro ao carregar transações: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void carregarMetas() {
        try {
            List<Meta> lista = metaService.listarTodasMetas();

            metas = FXCollections.observableArrayList(lista);
            listMetas.setItems(metas);

        } catch (Exception e) {
            System.err.println("Erro ao carregar metas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navegarParaInicio() { // Tela inicial (main.fxml)
        marcarBotaoAtivo(btnInicio);
        carregarDados();
    }

    private void navegarParaTransacoes() { // Tela transações (transacoes_view.fxml)
        marcarBotaoAtivo(btnTransacao);
        abrirViewTransacoes();
    }

    private void navegarParaMetas() { // Tela metas (metas_view.fxml)
        marcarBotaoAtivo(btnMetas);
        abrirViewMetas();
    }

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

            stage.showAndWait();

            carregarDados();

        } catch (Exception e) {
            System.err.println("Erro ao abrir view de transações: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

            stage.showAndWait();

            carregarDados();

        } catch (Exception e) {
            System.err.println("Erro ao abrir view de metas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void marcarBotaoAtivo(Button botaoAtivo) {
        btnInicio.getStyleClass().remove("active");
        btnTransacao.getStyleClass().remove("active");
        btnMetas.getStyleClass().remove("active");

        if (!botaoAtivo.getStyleClass().contains("active")) {
            botaoAtivo.getStyleClass().add("active");
        }
    }

    private void alternarTema() {
        darkTheme = !darkTheme;

        if (darkTheme) {
            rootContainer.getStyleClass().add("dark-theme");
        } else {
            rootContainer.getStyleClass().remove("dark-theme");
        }
    }

    private void abrirDialogNovaTransacao() { // Tela de Nova Transação
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
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            System.err.println("Erro ao abrir dialog de transação: " + e.getMessage());
            e.printStackTrace();
        }
    }

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
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            System.err.println("Erro ao abrir detalhes da transação: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            System.err.println("Erro ao abrir dialog de meta: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            System.err.println("Erro ao abrir detalhes da meta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void atualizarDados() {
        carregarDados();
    }

    public boolean isDarkTheme() {
        return darkTheme;
    }
}