package app.view;

import app.model.Categoria;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.support.FxTestSupport;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TransacaoCell")
class TransacaoCellTest {

    @BeforeAll
    static void initJavaFx() {
        FxTestSupport.initToolkit();
    }

    @Test
    @DisplayName("renderiza data em dd/MM/yyyy junto do comentário")
    void renderizaDataEmDdMmAaaaJuntoDoComentario() {
        Transacao transacao = new Transacao(
                UUID.randomUUID(),
                "Mercado",
                "compras do mês",
                350_00L,
                TipoTransacao.Saida,
                LocalDate.of(2026, 3, 30),
                null,
                Categoria.Alimentacao
        );

        String texto = FxTestSupport.callOnFxThreadAndWait(() -> {
            TransacaoCell cell = new TransacaoCell();
            cell.updateItem(transacao, false);
            return obterTextoData(cell);
        });

        assertEquals("30/03/2026 · compras do mês", texto);
    }

    @Test
    @DisplayName("renderiza apenas a data quando não há comentário")
    void renderizaApenasDataQuandoNaoHaComentario() {
        Transacao transacao = new Transacao(
                UUID.randomUUID(),
                "Salário",
                "",
                5000_00L,
                TipoTransacao.Entrada,
                LocalDate.of(2026, 3, 30),
                null,
                Categoria.Salario
        );

        String texto = FxTestSupport.callOnFxThreadAndWait(() -> {
            TransacaoCell cell = new TransacaoCell();
            cell.updateItem(transacao, false);
            return obterTextoData(cell);
        });

        assertEquals("30/03/2026", texto);
    }

    @Test
    @DisplayName("limpa a célula ao receber item vazio")
    void limpaCelulaAoReceberItemVazio() {
        TransacaoCell cell = FxTestSupport.callOnFxThreadAndWait(() -> {
            TransacaoCell localCell = new TransacaoCell();
            localCell.updateItem(null, true);
            return localCell;
        });

        assertAll(
                () -> assertNull(cell.getGraphic()),
                () -> assertNull(cell.getText())
        );
    }

    @Test
    @DisplayName("trunca textos longos e preserva espaço do valor à direita")
    void truncaTextosLongosEPreservaEspacoDoValorADireita() {
        Transacao transacao = new Transacao(
                UUID.randomUUID(),
                "Compra muito longa feita em um estabelecimento com nome gigantesco",
                "comentário muito longo que precisa ser cortado visualmente para não empurrar o valor para fora da área visível da célula",
                1234_56L,
                TipoTransacao.Saida,
                LocalDate.of(2026, 3, 30),
                null,
                Categoria.Compras
        );

        FxAssertions resultado = FxTestSupport.callOnFxThreadAndWait(() -> {
            TransacaoCell cell = new TransacaoCell();
            cell.updateItem(transacao, false);
            cell.setPrefWidth(320);
            cell.resize(320, 84);

            StackPane root = new StackPane(cell);
            Scene scene = new Scene(root, 320, 100);
            root.applyCss();
            root.layout();
            cell.applyCss();
            cell.layout();

            Label descricaoLabel = obterDescricaoLabel(cell);
            Label dataLabel = obterDataLabel(cell);
            Label valorLabel = obterValorLabel(cell);
            HBox container = (HBox) cell.getGraphic();

            double bordaDireitaValor = valorLabel.getBoundsInParent().getMaxX();

            return new FxAssertions(
                    descricaoLabel.getTextOverrun(),
                    dataLabel.getTextOverrun(),
                    valorLabel.getMinWidth(),
                    container.prefWidthProperty().isBound(),
                    bordaDireitaValor,
                    container.getWidth()
            );
        });

        assertAll(
                () -> assertEquals(OverrunStyle.ELLIPSIS, resultado.descricaoOverrun()),
                () -> assertEquals(OverrunStyle.ELLIPSIS, resultado.dataOverrun()),
                () -> assertEquals(Region.USE_PREF_SIZE, resultado.valorMinWidth()),
                () -> assertTrue(resultado.containerBound()),
                () -> assertTrue(resultado.bordaDireitaValor() <= resultado.larguraContainer() + 1.0)
        );
    }

    @Test
    @DisplayName("respeita o padding maior da lista de histórico sem empurrar o valor para fora")
    void respeitaPaddingDaListaDeHistorico() {
        Transacao transacao = new Transacao(
                UUID.randomUUID(),
                "Compra parcelada em loja com descrição extensa",
                "comentário grande que deve perder parte do texto visível antes de deslocar o valor para fora da área útil da célula de histórico",
                789_45L,
                TipoTransacao.Saida,
                LocalDate.of(2026, 4, 1),
                null,
                Categoria.Compras
        );

        HistoricoAssertions resultado = FxTestSupport.callOnFxThreadAndWait(() -> {
            TransacaoCell cell = new TransacaoCell();
            cell.setPadding(new Insets(15, 20, 15, 20));
            cell.updateItem(transacao, false);
            cell.setPrefWidth(420);
            cell.resize(420, 84);

            StackPane root = new StackPane(cell);
            Scene scene = new Scene(root, 420, 100);
            root.applyCss();
            root.layout();
            cell.applyCss();
            cell.layout();

            HBox container = (HBox) cell.getGraphic();
            Label valorLabel = obterValorLabel(cell);
            double larguraUtil = cell.getWidth() - cell.snappedLeftInset() - cell.snappedRightInset();

            return new HistoricoAssertions(
                    container.getWidth(),
                    larguraUtil,
                    valorLabel.getBoundsInParent().getMaxX()
            );
        });

        assertAll(
                () -> assertTrue(resultado.larguraContainer() <= resultado.larguraUtil() + 1.0),
                () -> assertTrue(resultado.bordaDireitaValor() <= resultado.larguraContainer() + 1.0)
        );
    }

    private static String obterTextoData(TransacaoCell cell) {
        HBox container = (HBox) cell.getGraphic();
        VBox infoBox = (VBox) container.getChildren().get(1);
        Label dataLabel = (Label) infoBox.getChildren().get(1);
        return dataLabel.getText();
    }

    private static Label obterDescricaoLabel(TransacaoCell cell) {
        HBox container = (HBox) cell.getGraphic();
        VBox infoBox = (VBox) container.getChildren().get(1);
        return (Label) infoBox.getChildren().get(0);
    }

    private static Label obterDataLabel(TransacaoCell cell) {
        HBox container = (HBox) cell.getGraphic();
        VBox infoBox = (VBox) container.getChildren().get(1);
        return (Label) infoBox.getChildren().get(1);
    }

    private static Label obterValorLabel(TransacaoCell cell) {
        HBox container = (HBox) cell.getGraphic();
        return (Label) container.getChildren().get(2);
    }

    private record FxAssertions(
            OverrunStyle descricaoOverrun,
            OverrunStyle dataOverrun,
            double valorMinWidth,
            boolean containerBound,
            double bordaDireitaValor,
            double larguraContainer
    ) {
    }

    private record HistoricoAssertions(
            double larguraContainer,
            double larguraUtil,
            double bordaDireitaValor
    ) {
    }
}
