package app.controller;

import app.model.Categoria;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.repository.TransacaoDAO;
import app.util.TransacaoCsvExporter;
import app.util.TransacaoSearchMatcher;
import app.util.FormatadorData;
import app.util.TransacaoTxtExporter;
import app.view.TransacaoCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TransacoesViewController {

    @FXML
    private Button btnNova;
    @FXML
    private Button btnExportarCsv;
    @FXML
    private Button btnExportarTxt;
    @FXML
    private Button btnLimparFiltros;
    @FXML
    private Button btnDetalhes;

    @FXML
    private ComboBox<String> cmbTipo;
    @FXML
    private ComboBox<Categoria> cmbCategoria;
    @FXML
    private TextField txtBusca;
    @FXML
    private DatePicker dateInicio;
    @FXML
    private DatePicker dateFim;

    @FXML
    private Label lblTotalEntradas;
    @FXML
    private Label lblTotalSaidas;
    @FXML
    private Label lblSaldoPeriodo;
    @FXML
    private Label lblTotal;

    @FXML
    private ListView<Transacao> listTransacoes;

    private TransacaoDAO transacaoDAO;
    private ObservableList<Transacao> todasTransacoes;
    private ObservableList<Transacao> transacoesFiltradas;

    // Usado para formatar números padrão Brasil
    private final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private Runnable onNovaTransacao;
    private TransacaoEditCallback onEditarTransacao;

    @FXML
    public void initialize() {
        transacaoDAO = new TransacaoDAO();

        listTransacoes.setCellFactory(lv -> new TransacaoCell());
        FormatadorData.configurar(dateInicio, dateFim);

        cmbTipo.setItems(FXCollections.observableArrayList("Todos", "Entrada", "Saida"));
        cmbTipo.setValue("Todos");

        List<Categoria> cats = Arrays.asList(Categoria.values());
        cmbCategoria.setItems(FXCollections.observableArrayList(cats));
        cmbCategoria.setPromptText("Todas");

        btnNova.setOnAction(e -> {
            if (onNovaTransacao != null) {
                onNovaTransacao.run();
                carregarTransacoes();
            }
        });

        btnExportarCsv.setOnAction(e -> exportarCsv());
        btnDetalhes.setOnAction(e -> abrirDetalhesSelecionada());
        btnExportarTxt.setOnAction(e -> exportarTxt());
        btnLimparFiltros.setOnAction(e -> limparFiltros());

        txtBusca.textProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        cmbTipo.valueProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        cmbCategoria.valueProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        dateInicio.valueProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        dateFim.valueProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        listTransacoes.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> atualizarEstadoBotaoDetalhes()
        );
        listTransacoes.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                abrirDetalhesSelecionada();
            }
        });
        listTransacoes.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                abrirDetalhesSelecionada();
            }
        });

        carregarTransacoes();
    }

    public void setOnNovaTransacao(Runnable callback) {
        this.onNovaTransacao = callback;
    }

    public void setOnEditarTransacao(TransacaoEditCallback callback) {
        this.onEditarTransacao = callback;
        atualizarEstadoBotaoDetalhes();
    }

    @FunctionalInterface
    public interface TransacaoEditCallback {
        void editar(Transacao transacao, Window owner);
    }

    private void carregarTransacoes() {
        // Carrega todas as transações registradas
        List<Transacao> lista = transacaoDAO.listarTodas();
        todasTransacoes = FXCollections.observableArrayList(lista);
        transacoesFiltradas = FXCollections.observableArrayList(lista);
        listTransacoes.setItems(transacoesFiltradas);
        aplicarFiltros();
        atualizarEstadoBotaoDetalhes();
    }

    private void aplicarFiltros() {
        List<Transacao> filtradas = todasTransacoes.stream()
                .filter(this::passaFiltroBusca)
                .filter(this::passaFiltroTipo)
                .filter(this::passaFiltroCategoria)
                .filter(this::passaFiltroData)
                .collect(Collectors.toList());

        transacoesFiltradas.setAll(filtradas);
        atualizarEstatisticas();
    }

    private boolean passaFiltroBusca(Transacao transacao) {
        return TransacaoSearchMatcher.corresponde(transacao, txtBusca.getText());
    }

    private boolean passaFiltroTipo(Transacao t) {
        String tipo = cmbTipo.getValue();
        if (tipo == null || tipo.equals("Todos")) return true;
        return (tipo.equals("Entrada") && t.tipo() == TipoTransacao.Entrada) ||
                (tipo.equals("Saida") && t.tipo() == TipoTransacao.Saida);
    }

    private boolean passaFiltroCategoria(Transacao t) {
        Categoria cat = cmbCategoria.getValue();
        if (cat == null) return true; // Nenhuma categoria selecionada = mostrar todas
        return cat.equals(t.categoria());
    }

    private boolean passaFiltroData(Transacao t) {
        LocalDate inicio = dateInicio.getValue();
        LocalDate fim = dateFim.getValue();
        if (inicio != null && t.data().isBefore(inicio)) return false;
        return fim == null || !t.data().isAfter(fim);
    }

    private void atualizarEstatisticas() {
        long totalEntradas = 0;
        long totalSaidas = 0;

        for (Transacao t : transacoesFiltradas) {
            if (t.tipo() == TipoTransacao.Entrada) {
                totalEntradas += t.valorCentavos();
            } else {
                totalSaidas += t.valorCentavos();
            }
        }

        long saldoPeriodo = totalEntradas - totalSaidas;

        lblTotalEntradas.setText(currencyFormatter.format(totalEntradas / 100.0));
        lblTotalSaidas.setText(currencyFormatter.format(totalSaidas / 100.0));
        lblSaldoPeriodo.setText(currencyFormatter.format(saldoPeriodo / 100.0));
        lblTotal.setText(String.format("Total: %d transacoes", transacoesFiltradas.size()));
    }

    // Reseta todos os filtros de data ou tipo/categoria setados
    private void limparFiltros() {
        txtBusca.clear();
        cmbTipo.setValue("Todos");
        cmbCategoria.setValue(null);
        dateInicio.setValue(null);
        dateFim.setValue(null);
    }

    private void exportarCsv() {
        exportarArquivo(
                "Exportar transações em CSV",
                "transacoes-" + LocalDate.now() + ".csv",
                new FileChooser.ExtensionFilter("Arquivo CSV", "*.csv"),
                path -> TransacaoCsvExporter.exportar(path, List.copyOf(transacoesFiltradas))
        );
    }

    private void exportarTxt() {
        exportarArquivo(
                "Exportar transações em TXT",
                "transacoes-" + LocalDate.now() + ".txt",
                new FileChooser.ExtensionFilter("Arquivo de texto", "*.txt"),
                path -> TransacaoTxtExporter.exportar(path, List.copyOf(transacoesFiltradas))
        );
    }

    private void exportarArquivo(
            String tituloJanela,
            String nomeInicial,
            FileChooser.ExtensionFilter filtro,
            ExportadorArquivo exportador
    ) {
        if (transacoesFiltradas == null || transacoesFiltradas.isEmpty()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Exportar transações",
                    "Não há transações para exportar.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(tituloJanela);
        chooser.setInitialFileName(nomeInicial);
        chooser.getExtensionFilters().add(filtro);

        File file = chooser.showSaveDialog(btnNova.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            exportador.exportar(file.toPath());
            mostrarAlerta(Alert.AlertType.INFORMATION, "Exportar transações",
                    "Arquivo exportado com sucesso.");
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Exportar transações",
                    "Não foi possível exportar o arquivo: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ExportadorArquivo {
        void exportar(java.nio.file.Path path) throws IOException;
    }

    private void mostrarAlerta(Alert.AlertType type, String titulo, String mensagem) {
        Alert alert = new Alert(type);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.initOwner(btnNova.getScene().getWindow());
        alert.showAndWait();
    }

    private void abrirDetalhesSelecionada() {
        Transacao selecionada = listTransacoes.getSelectionModel().getSelectedItem();
        if (selecionada == null || onEditarTransacao == null) {
            return;
        }

        onEditarTransacao.editar(selecionada, listTransacoes.getScene().getWindow());
        carregarTransacoes();
    }

    private void atualizarEstadoBotaoDetalhes() {
        boolean desabilitar = onEditarTransacao == null ||
                listTransacoes.getSelectionModel().getSelectedItem() == null;
        btnDetalhes.setDisable(desabilitar);
    }
}
