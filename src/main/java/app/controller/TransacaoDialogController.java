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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class TransacaoDialogController {

    @FXML private ToggleButton btnEntrada;
    @FXML private ToggleButton btnSaida;
    @FXML private ToggleGroup tipoGroup;
    @FXML private TextField txtValor;
    @FXML private DatePicker dateTransacao;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private ComboBox<Meta> cmbMeta;
    @FXML private TextArea txtComentario;
    @FXML private VBox boxMeta;
    @FXML private Label lblInfoMeta;
    @FXML private VBox boxResumo;
    @FXML private Label lblSaldoAtual;
    @FXML private Label lblNovoSaldo;
    @FXML private Label lblErroValor;
    @FXML private Label lblErroGeral;
    @FXML private Button btnCancelar;
    @FXML private Button btnSalvar;

    private TransacaoService transacaoService;
    private MetaService metaService;
    private boolean confirmado = false;
    private static final Logger logger = LoggerFactory.getLogger(TransacaoDialogController.class);
    private app.model.Transacao transacaoEmEdicao;

    private final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    @FXML
    public void initialize() {
        TransacaoDAO transacaoDAO = new TransacaoDAO();
        MetaDAO metaDAO = new MetaDAO();
        transacaoService = new TransacaoService(transacaoDAO, metaDAO);
        metaService = new MetaService(metaDAO);

        cmbCategoria.setItems(FXCollections.observableArrayList(Categoria.values()));

        // Data padrao = hoje
        dateTransacao.setValue(LocalDate.now());

        configurarMascaraMoeda();
        configurarListeners();

        btnEntrada.setSelected(true);
        atualizarEstadoFormulario();

        txtValor.requestFocus();
    }

    public void configurarParaEditar(app.model.Transacao transacao) {
        transacaoEmEdicao = transacao;

        if (transacao.tipo() == TipoTransacao.Entrada) {
            btnEntrada.setSelected(true);
        } else {
            btnSaida.setSelected(true);
        }
        atualizarEstadoFormulario();

        txtValor.setText(formatarValor(transacao.valorCentavos()));

        // Preenche data da transacao
        if (transacao.data() != null) {
            dateTransacao.setValue(transacao.data());
        }

        if (transacao.metaId() != null) {
            MetaDAO metaDAO = new MetaDAO();
            Meta meta = metaDAO.buscarPorId(transacao.metaId());
            cmbMeta.setValue(meta);
        }

        txtComentario.setText(transacao.descricao());
        btnSalvar.setText("Atualizar");
    }

    private void configurarListeners() {
        tipoGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> atualizarEstadoFormulario());
        cmbCategoria.valueProperty().addListener((obs, old, newCategoria) -> atualizarVisibilidadeMeta(newCategoria));
        cmbMeta.valueProperty().addListener((obs, old, newMeta) -> atualizarInfoMeta(newMeta));
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
                    logger.debug("Formatacao ignorada: {}", e.getMessage());
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
            LocalDate data = dateTransacao.getValue();
            Categoria categoria = cmbCategoria.getValue();
            Meta meta = cmbMeta.getValue();
            String comentario = txtComentario.getText().trim();

            if (valorCentavos <= 0) {
                mostrarErroValor();
                txtValor.requestFocus();
                return;
            }

            if (data == null) {
                mostrarErro("Selecione uma data");
                dateTransacao.requestFocus();
                return;
            }

            if (categoria == null) {
                mostrarErro("Selecione uma categoria");
                cmbCategoria.requestFocus();
                return;
            }

            boolean exigeMeta = categoria == Categoria.AdicionarMeta ||
                                 categoria == Categoria.RetirarMeta;

            if (exigeMeta && meta == null) {
                mostrarErro("Selecione uma meta");
                cmbMeta.requestFocus();
                return;
            }

            if (comentario.isEmpty()) {
                comentario = categoria.toString();
            }

            if (transacaoEmEdicao != null) {
                transacaoService.excluirTransacao(transacaoEmEdicao.id(), categoria);
                transacaoService.registrar(tipo, categoria, valorCentavos, meta, comentario, data);
            } else {
                transacaoService.registrar(tipo, categoria, valorCentavos, meta, comentario, data);
            }

            confirmado = true;
            fecharDialog();

        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        } catch (Exception e) {
            mostrarErro("Erro ao registrar transacao: " + e.getMessage());
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

        if (cmbCategoria.getValue() != null && !categorias.contains(cmbCategoria.getValue())) {
            cmbCategoria.setValue(null);
        }
    }

    private void atualizarVisibilidadeMeta(Categoria categoria) {
        if (categoria == null) {
            boxMeta.setVisible(false);
            boxMeta.setManaged(false);
            return;
        }

        boolean precisaMeta = categoria == Categoria.AdicionarMeta ||
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
            lblInfoMeta.setText(
                    String.format("Faltam %s para atingir a meta",
                            currencyFormatter.format(restante / 100.0))
            );
        } else if (categoria == Categoria.RetirarMeta) {
            lblInfoMeta.setText(
                    String.format("Disponivel: %s",
                            currencyFormatter.format(meta.getAtualCentavos() / 100.0))
            );
        }
    }

    private long parseValorCentavos(String texto) throws IllegalArgumentException {
        if (texto == null || texto.trim().isEmpty()) return 0;
        try {
            texto = texto.replace("R$", "").trim();
            texto = texto.replace(".", "").replace(",", ".");
            double valor = Double.parseDouble(texto);
            return Math.round(valor * 100);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor invalido");
        }
    }

    private String formatarValor(long centavos) {
        return String.format("%.2f", centavos / 100.0);
    }

    private void mostrarErro(String mensagem) {
        lblErroGeral.setText(mensagem);
        lblErroGeral.setVisible(true);
        lblErroGeral.setManaged(true);
    }

    private void mostrarErroValor() {
        lblErroValor.setText("Valor deve ser maior que zero");
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
