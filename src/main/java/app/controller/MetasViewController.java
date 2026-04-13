package app.controller;

import app.model.Meta;
import app.service.MetaService;
import app.view.MetaCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/* Lista todas as metas,
   filtra por status (todas, em andamento, concluídas),
   buscar por nome,
   visualiza estatísticas, e
   edita meta existente (duplo clique) */

public class MetasViewController {
    private static final Locale APP_LOCALE = Locale.of("pt", "BR");

    @FXML
    private Button btnNova;
    @FXML
    private Button btnLimparBusca;
    @FXML
    private Button btnTodas;
    @FXML
    private Button btnAndamento;
    @FXML
    private Button btnConcluidas;
    @FXML
    private TextField txtBusca;
    @FXML
    private Label lblTotalMetas;
    @FXML
    private Label lblMetasConcluidas;
    @FXML
    private Label lblMetasAndamento;
    @FXML
    private Label lblTotalAcumulado;
    @FXML
    private Label lblSecao;
    @FXML
    private ListView<Meta> listMetas;
    @FXML
    private Label lblProgresso;
    @FXML
    private Label lblTotal;

    private MetaService metaService;
    private ObservableList<Meta> todasMetas;
    private ObservableList<Meta> metasFiltradas;

    private FiltroStatus filtroAtual = FiltroStatus.TODAS;

    // Usado para formatar números padrão Brasil
    private final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(APP_LOCALE);

    private Runnable onNovaMeta;
    private MetaEditCallback onEditarMeta;

    @FXML
    public void initialize() {
        metaService = new MetaService();

        listMetas.setCellFactory(lv -> new MetaCell());
        configurarEventos();
        configurarListeners();
        carregarMetas();
        marcarAbaAtiva(btnTodas); // Inicia janela na aba filtro todas
    }

    private void configurarEventos() {
        btnNova.setOnAction(e -> {
            if (onNovaMeta != null) {
                onNovaMeta.run();
                carregarMetas();
            }
        });

        btnLimparBusca.setOnAction(e -> limparBusca());

        // Aba de todas as metas
        btnTodas.setOnAction(e -> {
            filtroAtual = FiltroStatus.TODAS;
            marcarAbaAtiva(btnTodas);
            aplicarFiltros();
        });

        // Aba de metas em andamento
        btnAndamento.setOnAction(e -> {
            filtroAtual = FiltroStatus.EM_ANDAMENTO;
            marcarAbaAtiva(btnAndamento);
            aplicarFiltros();
        });

        // Aba de metas concluídas
        btnConcluidas.setOnAction(e -> {
            filtroAtual = FiltroStatus.CONCLUIDAS;
            marcarAbaAtiva(btnConcluidas);
            aplicarFiltros();
        });

        // Duplo clique para editar meta
        listMetas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Meta selecionada = listMetas.getSelectionModel().getSelectedItem();
                if (selecionada != null && onEditarMeta != null) {
                    onEditarMeta.editar(selecionada);
                    carregarMetas();
                }
            }
        });
    }

    private void configurarListeners() {
        txtBusca.textProperty().addListener((obs, old, newValue) -> {
            btnLimparBusca.setVisible(!newValue.isEmpty());
            aplicarFiltros();
        });
    }

    public void setOnNovaMeta(Runnable callback) {
        this.onNovaMeta = callback;
    }

    public void setOnEditarMeta(MetaEditCallback callback) {
        this.onEditarMeta = callback;
    }

    @FunctionalInterface
    public interface MetaEditCallback {
        void editar(Meta meta);
    }

    private void carregarMetas() {
        List<Meta> lista = metaService.listarTodasMetas();
        todasMetas = FXCollections.observableArrayList(lista);
        metasFiltradas = FXCollections.observableArrayList(lista);

        listMetas.setItems(metasFiltradas);

        atualizarEstatisticas();
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        List<Meta> filtradas = todasMetas.stream()
                .filter(this::passaFiltroStatus)
                .filter(this::passaFiltroBusca)
                .collect(Collectors.toList());

        metasFiltradas.setAll(filtradas);
        atualizarLabels();
    }

    private boolean passaFiltroStatus(Meta meta) {
        boolean concluida = metaService.metaConcluida(meta);

        return switch (filtroAtual) {
            case TODAS -> true;
            case EM_ANDAMENTO -> !concluida;
            case CONCLUIDAS -> concluida;
        };
    }

    private boolean passaFiltroBusca(Meta meta) {
        String busca = txtBusca.getText().trim().toLowerCase();
        if (busca.isEmpty()) return true;

        return meta.getNome().toLowerCase().contains(busca);
    }

    private void limparBusca() {
        txtBusca.clear();
    }

    private void atualizarEstatisticas() {
        int total = todasMetas.size();
        long concluidas = metaService.contarMetasConcluidas();
        int emAndamento = (int) (total - concluidas);

        long totalAcumuladoCentavos = todasMetas.stream()
                .mapToLong(Meta::getAtualCentavos)
                .sum();

        double progressoMedio = todasMetas.stream()
                .mapToDouble(metaService::calcularProgresso)
                .average()
                .orElse(0.0);

        lblTotalMetas.setText(String.valueOf(total));
        lblMetasConcluidas.setText(String.valueOf(concluidas));
        lblMetasAndamento.setText(String.valueOf(emAndamento));
        lblTotalAcumulado.setText(
                currencyFormatter.format(totalAcumuladoCentavos / 100.0)
        );

        lblProgresso.setText(
                String.format(APP_LOCALE, "Progresso médio: %.1f%%", progressoMedio)
        );
    }

    private void atualizarLabels() {
        String titulo = switch (filtroAtual) {
            case TODAS -> "Todas as Metas";
            case EM_ANDAMENTO -> "Metas em Andamento";
            case CONCLUIDAS -> "Metas Concluídas";
        };
        lblSecao.setText(titulo);

        lblTotal.setText(
                String.format("Total: %d meta%s",
                        metasFiltradas.size(),
                        metasFiltradas.size() != 1 ? "s" : "")
        );
    }

    // Quando aba for clicada, fica destacada
    private void marcarAbaAtiva(Button botaoAtivo) {
        btnTodas.getStyleClass().remove("active");
        btnAndamento.getStyleClass().remove("active");
        btnConcluidas.getStyleClass().remove("active");

        if (!botaoAtivo.getStyleClass().contains("active")) {
            botaoAtivo.getStyleClass().add("active");
        }
    }

    // Filtro metas janela acessada pela sidebar
    private enum FiltroStatus {
        TODAS,
        EM_ANDAMENTO,
        CONCLUIDAS
    }
}
