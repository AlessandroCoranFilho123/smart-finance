package app.controller;

import app.model.Categoria;
import app.model.Meta;
import app.model.TipoTransacao;
import app.repository.MetaDAO;
import app.repository.TransacaoDAO;
import app.service.MetaService;
import app.service.TransacaoService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TransacaoDialogController {
    @FXML
    private ToggleButton btnEntrada;
    @FXML
    private ToggleButton btnSaida;
    @FXML
    private ToggleGroup tipoGroup;
    @FXML
    private TextField txtValor;
    @FXML
    private ComboBox<Categoria> cmbCategoria;
    @FXML
    private ComboBox<Meta> cmbMeta;
    @FXML
    private TextArea txtComentario;
    @FXML
    private VBox boxMeta;
    @FXML
    private Label lblInfoMeta;
    @FXML
    private VBox boxResumo;
    @FXML
    private Label lblSaldoAtual;
    @FXML
    private Label lblNovoSaldo;
    @FXML
    private Label lblErroValor;
    @FXML
    private Label lblErroGeral;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnSalvar;

    private TransacaoService transacaoService;
    private MetaService metaService;
    private boolean confirmado = false;

    private final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    @FXML
    public void initialize() {
        TransacaoDAO transacaoDAO = new TransacaoDAO();
        MetaDAO metaDAO = new MetaDAO();
        transacaoService = new TransacaoService(transacaoDAO, metaDAO);
        metaService = new MetaService(metaDAO);

        cmbCategoria.setItems(FXCollections.observableArrayList(Categoria.values()));

        configurarMascaraMoeda();

        configurarListeners();

        btnEntrada.setSelected(true);
        atualizarEstadoFormulario();

        txtValor.requestFocus();
    }

    private void configurarListeners() {
        tipoGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            atualizarEstadoFormulario();
        });

        cmbCategoria.valueProperty().addListener((obs, old, newCategoria) -> {
            atualizarVisibilidadeMeta(newCategoria);
        });

        txtValor.textProperty().addListener((obs, old, newValue) -> {
            atualizarResumo();
        });

        cmbMeta.valueProperty().addListener((obs, old, newMeta) -> {
            atualizarInfoMeta(newMeta);
        });
    }

    private void configurarMascaraMoeda() {
        txtValor.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[0-9.,]*")) {
                txtValor.setText(oldValue);
            }
        });

        txtValor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !txtValor.getText().isEmpty()) {
                try {
                    long centavos = parseValorCentavos(txtValor.getText());
                    txtValor.setText(formatarValor(centavos));
                } catch (Exception e) {
                }
            }
        });
    }

    @FXML
    private void handleSalvar() {
        limparErros();

        try {
            TipoTransacao tipo = btnEntrada.isSelected() ?
                    TipoTransacao.Entrada : TipoTransacao.Saida;

            long valorCentavos = parseValorCentavos(txtValor.getText());
            Categoria categoria = cmbCategoria.getValue();
            Meta meta = cmbMeta.getValue();
            String comentario = txtComentario.getText().trim();

            if (valorCentavos <= 0) {
                mostrarErroValor("Valor deve ser maior que zero");
                txtValor.requestFocus();
                return;
            }

            if (categoria == null) {
                mostrarErro("Selecione uma categoria");
                cmbCategoria.requestFocus();
                return;
            }

            boolean exigeMeta =
                    categoria == Categoria.AdicionarMeta ||
                            categoria == Categoria.RetirarMeta;

            if (exigeMeta && meta == null) {
                mostrarErro("Selecione uma meta");
                cmbMeta.requestFocus();
                return;
            }

            if (comentario.isEmpty()) {
                comentario = categoria.toString();
            }

            transacaoService.registrar(tipo, categoria, valorCentavos, meta, comentario);

            confirmado = true;
            fecharDialog();

        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        } catch (Exception e) {
            mostrarErro("Erro ao registrar transação: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelar() {
        confirmado = false;
        fecharDialog();
    }

    private void atualizarEstadoFormulario() {
        TipoTransacao tipo = btnEntrada.isSelected() ?
                TipoTransacao.Entrada : TipoTransacao.Saida;

        List<Categoria> categorias;
        if (tipo == TipoTransacao.Entrada) {
            categorias = List.of(
                    Categoria.Salario,
                    Categoria.Emprestimo,
                    Categoria.RetirarMeta,
                    Categoria.Outros
            );
        } else {
            categorias = List.of(
                    Categoria.Emprestimo,
                    Categoria.Alimentacao,
                    Categoria.Aluguel,
                    Categoria.Transporte,
                    Categoria.Internet,
                    Categoria.Agua,
                    Categoria.Luz,
                    Categoria.Compras,
                    Categoria.Educacao,
                    Categoria.AdicionarMeta,
                    Categoria.Outros
            );
        }

        cmbCategoria.setItems(FXCollections.observableArrayList(categorias));
        cmbCategoria.setValue(null);

        atualizarResumo();
    }

    private void atualizarVisibilidadeMeta(Categoria categoria) {
        if (categoria == null) {
            boxMeta.setVisible(false);
            boxMeta.setManaged(false);
            return;
        }

        boolean precisaMeta =
                categoria == Categoria.AdicionarMeta ||
                        categoria == Categoria.RetirarMeta;

        boxMeta.setVisible(precisaMeta);
        boxMeta.setManaged(precisaMeta);

        if (precisaMeta) {
            carregarMetas();
        }
    }

    private void carregarMetas() {
        List<Meta> metas = metaService.listarTodasMetas();
        cmbMeta.setItems(FXCollections.observableArrayList(metas));
        cmbMeta.setValue(null);
    }

    private void atualizarInfoMeta(Meta meta) {
        if (meta == null) {
            lblInfoMeta.setText("");
            return;
        }

        Categoria categoria = cmbCategoria.getValue();

        if (categoria == Categoria.AdicionarMeta) {
            long restante = meta.restanteParaAlvo();
            double valor = restante / 100.0;
            lblInfoMeta.setText(
                    String.format("Faltam %s para atingir a meta",
                            currencyFormatter.format(valor))
            );
        } else if (categoria == Categoria.RetirarMeta) {
            double valor = meta.getAtualCentavos() / 100.0;
            lblInfoMeta.setText(
                    String.format("Disponível: %s",
                            currencyFormatter.format(valor))
            );
        }
    }

    private void atualizarResumo() {
        try {
            long saldoAtual = transacaoService.calcularSaldoDisponivelCentavos();
            long valorTransacao = parseValorCentavos(txtValor.getText());

            TipoTransacao tipo = btnEntrada.isSelected() ?
                    TipoTransacao.Entrada : TipoTransacao.Saida;

            long novoSaldo = tipo == TipoTransacao.Entrada ?
                    saldoAtual + valorTransacao :
                    saldoAtual - valorTransacao;

            lblSaldoAtual.setText(currencyFormatter.format(saldoAtual / 100.0));
            lblNovoSaldo.setText(currencyFormatter.format(novoSaldo / 100.0));

            boxResumo.setVisible(true);
            boxResumo.setManaged(true);

        } catch (Exception e) {
            boxResumo.setVisible(false);
            boxResumo.setManaged(false);
        }
    }

    private long parseValorCentavos(String texto) throws IllegalArgumentException {
        if (texto == null || texto.trim().isEmpty()) {
            return 0;
        }

        try {
            texto = texto.replace("R$", "").trim();
            texto = texto.replace(".", "").replace(",", ".");
            double valor = Double.parseDouble(texto);
            return Math.round(valor * 100);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido");
        }
    }

    private String formatarValor(long centavos) {
        double valor = centavos / 100.0;
        return String.format("%.2f", valor);
    }

    private void mostrarErro(String mensagem) {
        lblErroGeral.setText(mensagem);
        lblErroGeral.setVisible(true);
        lblErroGeral.setManaged(true);
    }

    private void mostrarErroValor(String mensagem) {
        lblErroValor.setText(mensagem);
        lblErroValor.setVisible(true);
        lblErroValor.setManaged(true);
    }

    private void limparErros() {
        lblErroGeral.setVisible(false);
        lblErroGeral.setManaged(false);
        lblErroValor.setVisible(false);
        lblErroValor.setManaged(false);
    }

    private void fecharDialog() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    public boolean isConfirmado() {
        return confirmado;
    }
}