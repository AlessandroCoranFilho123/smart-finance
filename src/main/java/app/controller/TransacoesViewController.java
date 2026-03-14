package app.controller;

import app.model.Categoria;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.repository.TransacaoDAO;
import app.view.TransacaoCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

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
    private Button btnFechar;
    @FXML
    private Button btnLimparFiltros;

    @FXML
    private ComboBox<String> cmbTipo;
    @FXML
    private ComboBox<Categoria> cmbCategoria;
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

    @FXML
    public void initialize() {
        transacaoDAO = new TransacaoDAO();

        listTransacoes.setCellFactory(lv -> new TransacaoCell());

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

        btnFechar.setOnAction(e -> fechar());
        btnLimparFiltros.setOnAction(e -> limparFiltros());

        cmbTipo.valueProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        cmbCategoria.valueProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        dateInicio.valueProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        dateFim.valueProperty().addListener((obs, old, newVal) -> aplicarFiltros());

        carregarTransacoes();
    }

    public void setOnNovaTransacao(Runnable callback) {
        this.onNovaTransacao = callback;
    }

    private void carregarTransacoes() {
        // Carrega todas as transações registradas
        List<Transacao> lista = transacaoDAO.listarTodas();
        todasTransacoes = FXCollections.observableArrayList(lista);
        transacoesFiltradas = FXCollections.observableArrayList(lista);
        listTransacoes.setItems(transacoesFiltradas);
        atualizarEstatisticas();
    }

    private void aplicarFiltros() {
        List<Transacao> filtradas = todasTransacoes.stream()
                .filter(this::passaFiltroTipo)
                .filter(this::passaFiltroCategoria)
                .filter(this::passaFiltroData)
                .collect(Collectors.toList());

        transacoesFiltradas.setAll(filtradas);
        atualizarEstatisticas();
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
        cmbTipo.setValue("Todos");
        cmbCategoria.setValue(null);
        dateInicio.setValue(null);
        dateFim.setValue(null);
    }

    // Botão para fechar tela de todas as transações
    private void fechar() {
        Stage stage = (Stage) btnFechar.getScene().getWindow();
        stage.close();
    }
}