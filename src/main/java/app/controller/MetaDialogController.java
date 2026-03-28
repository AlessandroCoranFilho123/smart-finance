package app.controller;

import app.model.Meta;
import app.service.MetaService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.Locale;

/* Cria e edita metas,
   exibe o progresso, e
   valida e formata valor centavos */
@SuppressWarnings("unused")
public class MetaDialogController {
    @FXML
    private Label lblTitulo;
    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtValorAlvo;
    @FXML
    private Label lblErroValor;
    @FXML
    private Label lblErroGeral;
    @FXML
    private VBox boxProgresso;
    @FXML
    private Label lblValorAtual;
    @FXML
    private Label lblRestante;
    @FXML
    private Label lblProgresso;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnSalvar;
    @FXML
    private Button btnExcluir;

    private MetaService metaService;
    private Meta metaEmEdicao;
    private boolean confirmado = false;

    // Usado para formatar números padrão Brasil
    private final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    // Registrar avisos e erros
    private static final Logger logger = LoggerFactory.getLogger(MetaDialogController.class);

    @FXML
    public void initialize() {
        metaService = new MetaService();
        configurarMascaraMoeda();
        txtNome.requestFocus();
    }

    // Modo criação: esconde progresso e botão excluir
    public void configurarParaCriar() {
        lblTitulo.setText("Nova Meta");
        metaEmEdicao = null;
        boxProgresso.setVisible(false);
        boxProgresso.setManaged(false);
        btnExcluir.setVisible(false);
        btnExcluir.setManaged(false);
    }

    // Modo edição: preenche campos, exibe progresso e botão excluir
    public void configurarParaEditar(Meta meta) {
        lblTitulo.setText("Editar Meta");
        metaEmEdicao = meta;

        txtNome.setText(meta.getNome());
        txtValorAlvo.setText(formatarValor(meta.getAlvoCentavos()));

        boxProgresso.setVisible(true);
        boxProgresso.setManaged(true);
        atualizarProgresso(meta);

        btnExcluir.setVisible(true);
        btnExcluir.setManaged(true);
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    @FXML
    private void handleSalvar() { // Valida os campos, salva ou atualiza a meta e fecha o dialog
        limparErros();

        try {
            String nome = txtNome.getText().trim();
            long valorAlvoCentavos = parseValorCentavos(txtValorAlvo.getText());

            if (nome.isEmpty()) { // Nome da meta não pode ser nulo
                mostrarErro("Nome é obrigatório");
                txtNome.requestFocus();
                return;
            }

            if (valorAlvoCentavos <= 0) { // Valor alvo não pode ser menor ou igual a 0
                mostrarErroValor();
                txtValorAlvo.requestFocus();
                return;
            }

            if (metaEmEdicao == null) {
                metaService.criarMeta(nome, valorAlvoCentavos);
            } else {
                Meta metaAtualizada = new Meta(
                        metaEmEdicao.getId(),
                        nome,
                        valorAlvoCentavos,
                        metaEmEdicao.getAtualCentavos()
                );
                metaService.atualizarMeta(metaAtualizada);
            }

            confirmado = true;
            fecharDialog();

        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro ao salvar meta: {}", e.getMessage());
        }
    }

    @FXML
    private void handleExcluir() {
        if (metaEmEdicao == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir Meta");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Tem certeza que deseja excluir a meta \"" + metaEmEdicao.getNome() + "\"?\n" +
                        "As transações vinculadas serão mantidas no histórico."
        );

        confirm.showAndWait().ifPresent(resposta -> {
            if (resposta == ButtonType.OK) {
                try {
                    metaService.deletarMeta(metaEmEdicao.getId());
                    confirmado = true;
                    fecharDialog();
                } catch (Exception e) {
                    mostrarErro("Erro ao excluir meta: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleCancelar() {  // Cancela a operação e fecha o dialog sem salvar
        confirmado = false;
        fecharDialog();
    }

    // Impede caracteres inválidos no campo de valor
    private void configurarMascaraMoeda() {
        txtValorAlvo.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[0-9.,]*")) {
                txtValorAlvo.setText(oldValue);
            }
        });

        txtValorAlvo.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !txtValorAlvo.getText().isEmpty()) {
                try {
                    long centavos = parseValorCentavos(txtValorAlvo.getText());
                    txtValorAlvo.setText(formatarValor(centavos));
                } catch (Exception e) {
                    logger.debug("Formatacao ignorada: {}", e.getMessage());
                }
            }
        });
    }

    private long parseValorCentavos(String texto) throws IllegalArgumentException {
        if (texto == null || texto.trim().isEmpty()) {
            return 0;
        }

        try {
            texto = texto.trim();

            boolean temVirgulaDecimal = texto.contains(",");
            if (temVirgulaDecimal) {
                // padrão BR: ponto é milhar, vírgula é decimal
                texto = texto.replace(".", "").replace(",", ".");
            }
            double valor = Double.parseDouble(texto);
            return Math.round(valor * 100);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido");
        }
    }

    // Converte os centavos para String formatada com duas casas decimais
    private String formatarValor(long centavos) {
        double valor = centavos / 100.0;
        return String.format("%.2f", valor);
    }

    // Atualiza os labels de valor atual, restante e porcentagem de progresso
    private void atualizarProgresso(Meta meta) {
        double atual = meta.getAtualCentavos() / 100.0;
        double restante = meta.restanteParaAlvo() / 100.0;
        double progresso = metaService.calcularProgresso(meta);

        lblValorAtual.setText(currencyFormatter.format(atual));
        lblRestante.setText(currencyFormatter.format(restante));
        lblProgresso.setText(String.format("%.1f%%", progresso));
    }

    // Exibe mensagem de erro
    private void mostrarErro(String mensagem) {
        lblErroGeral.setText(mensagem);
        lblErroGeral.setVisible(true);
        lblErroGeral.setManaged(true);
    }

    // Helper method para erros de valor
    private void mostrarErroValor() {
        lblErroValor.setText("Valor deve ser maior que zero");
        lblErroValor.setVisible(true);
        lblErroValor.setManaged(true);
    }

    // Oculta todos os labels de erro
    private void limparErros() {
        lblErroGeral.setVisible(false);
        lblErroGeral.setManaged(false);
        lblErroValor.setVisible(false);
        lblErroValor.setManaged(false);
    }

    // Fecha a janela
    private void fecharDialog() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}