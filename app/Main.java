package app;

import app.model.*;
import app.service.TransacaoService;
import app.view.TransacaoDetalheView;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class Main extends Application {

    private final List<Transacao> transacoes = new ArrayList<>();
    private final List<Meta> metas = new ArrayList<>();

    private TransacaoService service;

    private final ListView<Transacao> listaTransacoes = new ListView<>();
    private final VBox painelMetas = new VBox(10);

    private final Label lblSaldo = new Label();
    private final Label lblMediaMensal = new Label();
    private final Label lblMediaSemanal = new Label();

    private final NumberFormat nf = NumberFormat.getCurrencyInstance();

    @Override
    public void start(Stage stage) {

        service = new TransacaoService(transacoes, metas);

        Button btnNovaTransacao = new Button("Nova Transação");
        Button btnNovaMeta = new Button("Nova Meta");
        Button btnExcluirTransacao = new Button("Excluir Transação");

        btnNovaTransacao.setOnAction(_ -> novaTransacao());
        btnNovaMeta.setOnAction(_ -> novaMeta());
        btnExcluirTransacao.setOnAction(_ -> excluirTransacao());

            listaTransacoes.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    Transacao t = listaTransacoes.getSelectionModel().getSelectedItem();
                    if (t != null) {
                        TransacaoDetalheView.abrir(t);
                        atualizarUI();
                    }
                }
            });

        listaTransacoes.setCellFactory(_ -> new ListCell<>() {

            @Override
            protected void updateItem(Transacao t, boolean empty) {
                super.updateItem(t, empty);

                if (empty || t == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                String valor = nf.format(t.getValorCentavos() / 100.0);

                String titulo = t.getNome().isBlank()
                        ? t.getCategoria().name()
                        : t.getNome();

                String meta = t.getMetaNome().isBlank()
                        ? ""
                        : " → Meta: " + t.getMetaNome();

                String data = t.getDataHora()
                        .toLocalDate()
                        .toString();

                setText(
                        titulo + "\n" +
                                valor + meta + "\n" +
                                data
                );
            }
        });


        VBox layout = new VBox(
                10,
                new HBox(10, btnNovaTransacao, btnNovaMeta, btnExcluirTransacao),
                lblSaldo,
                lblMediaMensal,
                lblMediaSemanal,
                new Label("Metas"),
                painelMetas,
                new Label("Transações"),
                listaTransacoes
        );

        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-padding:20;");

        atualizarUI();

        stage.setScene(new Scene(layout, 640, 720));
        stage.setTitle("Aplicativo de Finanças");
        stage.show();
    }

    private void novaTransacao() {

        Stage stage = new Stage();

        ComboBox<TipoTransacao> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll(TipoTransacao.values());

        ComboBox<Categoria> cbCategoria = new ComboBox<>();
        ComboBox<Meta> cbMeta = new ComboBox<>();
        cbMeta.getItems().addAll(metas);

        TextField txtValor = new TextField();
        TextField txtComentario = new TextField();
        TextField txtTags = new TextField();

        cbMeta.visibleProperty().bind(
                cbCategoria.valueProperty().isEqualTo(Categoria.AdicionarMeta)
                        .or(cbCategoria.valueProperty().isEqualTo(Categoria.RetirarMeta))
        );
        cbMeta.managedProperty().bind(cbMeta.visibleProperty());

        cbTipo.valueProperty().addListener((_, _, tipo) -> {
            cbCategoria.getItems().clear();
            if (tipo == TipoTransacao.Entrada) {
                cbCategoria.getItems().addAll(
                        Categoria.Salario,
                        Categoria.Emprestimo,
                        Categoria.AdicionarMeta,
                        Categoria.Outros
                );
            } else {
                cbCategoria.getItems().addAll(
                        Categoria.Aluguel,
                        Categoria.Alimentacao,
                        Categoria.Internet,
                        Categoria.Agua,
                        Categoria.Luz,
                        Categoria.Compras,
                        Categoria.RetirarMeta,
                        Categoria.Outros
                );
            }
        });

        DatePicker datePicker = new DatePicker(LocalDate.now());

        Spinner<Integer> horaSpinner =
                new Spinner<>(0, 23, LocalTime.now().getHour());
        Spinner<Integer> minutoSpinner =
                new Spinner<>(0, 59, LocalTime.now().getMinute());

        HBox horaBox = new HBox(5,
                new Label("Hora"),
                horaSpinner,
                new Label("Min"),
                minutoSpinner
        );


        Button salvar = new Button("Salvar");
        salvar.setOnAction(_ -> {
            try {

                if (cbTipo.getValue() == null || cbCategoria.getValue() == null) {
                    alerta("Selecione tipo e categoria");
                    return;
                }

                if (txtValor.getText().isBlank()) {
                    alerta("Informe um valor");
                    return;
                }

                BigDecimal valor = new BigDecimal(
                        txtValor.getText().replace(",", ".")
                );

                long centavos = valor
                        .setScale(2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .longValueExact();

                Set<String> tags = txtTags.getText().isBlank()
                        ? Set.of()
                        : Arrays.stream(txtTags.getText().split(","))
                        .map(String::trim)
                        .filter( s -> !s.isEmpty())
                        .collect(Collectors.toSet());

                LocalDate data = datePicker.getValue();
                if (data == null) {
                    alerta("Selecione uma data");
                    return;
                }

                LocalDateTime dataHora = data.atTime(
                        horaSpinner.getValue(),
                        minutoSpinner.getValue()
                );

                service.registrar(
                        cbTipo.getValue(),
                        cbCategoria.getValue(),
                        centavos,
                        cbMeta.getValue(),
                        txtComentario.getText(),
                        tags,
                        dataHora
                );

                atualizarUI();
                stage.close();

            } catch (Exception e) {
                alerta(e.getMessage());
            }
        });


        VBox layout = new VBox(10,
                new Label("Tipo"), cbTipo,
                new Label("Valor"), txtValor,
                new Label("Categoria"), cbCategoria,
                new Label("Meta"), cbMeta,
                new Label("Comentário"), txtComentario,
                new Label("Tags (separadas por vírgula)"), txtTags,
                salvar
        );

        layout.setStyle("-fx-padding:20;");
        stage.setScene(new Scene(layout, 380, 520));
        stage.show();
    }

    private void novaMeta() {

        Stage s = new Stage();

        TextField nome = new TextField();
        TextField alvo = new TextField();

        Button salvar = new Button("Salvar");
        salvar.setOnAction(_ -> {
            try {
                Long alvoCentavos = alvo.getText().isBlank()
                        ? null
                        : Math.round(Double.parseDouble(alvo.getText()) * 100);

                metas.add(new Meta(nome.getText(), alvoCentavos));
                atualizarUI();
                s.close();

            } catch (Exception e) {
                alerta("Dados inválidos");
            }
        });

        s.setScene(new Scene(new VBox(10,
                new Label("Nome da Meta"), nome,
                new Label("Valor Alvo"), alvo,
                salvar
        ), 300, 260));
        s.show();
    }

    private void excluirTransacao() {
        int index = listaTransacoes.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            alerta("Selecione uma transação");
            return;
        }
        service.excluirTransacao(index);
        atualizarUI();
    }

    private void atualizarUI() {

        listaTransacoes.getItems().setAll(transacoes);

        painelMetas.getChildren().clear();
        for (Meta m : metas) {

            Label nome = new Label(m.getNome());
            Label valor = new Label(nf.format(m.getAtualCentavos() / 100.0));

            ProgressBar pb = new ProgressBar(m.progresso());
            pb.setMaxWidth(Double.MAX_VALUE);

            Button excluir = new Button("Excluir");
            excluir.setOnAction(_ -> {
                service.excluirMeta(m);
                atualizarUI();
            });

            VBox card = new VBox(6, nome, valor, pb, excluir);
            card.setStyle("-fx-padding:10; -fx-background-color:#ddd; -fx-background-radius:10;");

            painelMetas.getChildren().add(card);
        }

        lblSaldo.setText("Saldo disponível: " +
                nf.format(service.calcularSaldoDisponivelCentavos() / 100.0));

        lblMediaMensal.setText("Gasto médio mensal: " +
                nf.format(service.gastoMedioMensal()));

        lblMediaSemanal.setText("Gasto médio semanal: " +
                nf.format(service.gastoMedioSemanal()));
    }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}
