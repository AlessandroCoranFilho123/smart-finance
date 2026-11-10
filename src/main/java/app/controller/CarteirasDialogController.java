package app.controller;

import app.repository.CarteiraDAO;
import app.service.CarteiraService;
import app.util.ValorMonetario;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CarteirasDialogController {

    @FXML
    private TextField txtBanco;
    @FXML
    private TextField txtDinheiroFisico;
    @FXML
    private Label lblErro;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnSalvar;

    private CarteiraService carteiraService;
    private boolean confirmado;

    @FXML
    public void initialize() {
        carteiraService = new CarteiraService();
        configurarMascaraMoeda(txtBanco);
        configurarMascaraMoeda(txtDinheiroFisico);
        btnSalvar.setOnAction(e -> salvar());
        btnCancelar.setOnAction(e -> fechar(false));
        carregarValores();
    }

    public void setCarteiraService(CarteiraService carteiraService) {
        this.carteiraService = carteiraService;
        carregarValores();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    private void carregarValores() {
        if (carteiraService == null || txtBanco == null || txtDinheiroFisico == null) {
            return;
        }

        var banco = carteiraService.buscarPorId(CarteiraDAO.BANCO_ID);
        var dinheiro = carteiraService.buscarPorId(CarteiraDAO.DINHEIRO_FISICO_ID);
        if (banco != null) {
            txtBanco.setText(formatarValor(banco.saldoInicialCentavos()));
        }
        if (dinheiro != null) {
            txtDinheiroFisico.setText(formatarValor(dinheiro.saldoInicialCentavos()));
        }
    }

    private void salvar() {
        limparErro();
        try {
            carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, parseValorCentavos(txtBanco.getText()));
            carteiraService.atualizarSaldoInicial(CarteiraDAO.DINHEIRO_FISICO_ID, parseValorCentavos(txtDinheiroFisico.getText()));
            fechar(true);
        } catch (RuntimeException e) {
            mostrarErro(e.getMessage());
        }
    }

    private void configurarMascaraMoeda(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[0-9.,]*")) {
                field.setText(oldValue);
            }
        });
    }

    private long parseValorCentavos(String texto) {
        return ValorMonetario.parseCentavos(texto);
    }

    private String formatarValor(long centavos) {
        return ValorMonetario.formatarCentavos(centavos);
    }

    private void mostrarErro(String mensagem) {
        lblErro.setText(mensagem);
        lblErro.setVisible(true);
        lblErro.setManaged(true);
    }

    private void limparErro() {
        lblErro.setVisible(false);
        lblErro.setManaged(false);
    }

    private void fechar(boolean confirmado) {
        this.confirmado = confirmado;
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}
