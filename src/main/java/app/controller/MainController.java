package app.controller;

import app.model.Transacao;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

@SuppressWarnings("unused")
public class MainController {
    @FXML
    private Label lblSaldo;
    @FXML
    private ListView<Transacao> listTransacoes;

    @FXML
    @SuppressWarnings("unused")
    public void initialize() {
        lblSaldo.setText("R$ ");
    }
}
