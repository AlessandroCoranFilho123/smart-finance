package app.view;

import app.model.Meta;
import app.support.FxTestSupport;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MetaCell")
class MetaCellTest {

    @BeforeAll
    static void initJavaFx() {
        FxTestSupport.initToolkit();
    }

    @Test
    @DisplayName("usa vermelho até 49 por cento")
    void usaVermelhoAte49PorCento() {
        Meta meta = new Meta(UUID.randomUUID(), "Reserva", 1000_00L, 490_00L);

        MetaRender render = renderizar(meta);

        assertAll(
                () -> assertEquals(0.49, render.progresso(), 0.001),
                () -> assertTrue(render.classesBarra().contains("progress-bar-meta-vermelho")),
                () -> assertEquals("49.0% concluído", normalizarNumero(render.textoPercentual())),
                () -> assertEquals(render.larguraTrilha() * 0.49, render.larguraPreenchimento(), 1.5)
        );
    }

    @Test
    @DisplayName("usa laranja de 50 a 74 por cento")
    void usaLaranjaDe50A74PorCento() {
        Meta meta = new Meta(UUID.randomUUID(), "Viagem", 1000_00L, 500_00L);

        MetaRender render = renderizar(meta);

        assertAll(
                () -> assertEquals(0.50, render.progresso(), 0.001),
                () -> assertTrue(render.classesBarra().contains("progress-bar-meta-laranja")),
                () -> assertEquals("50.0% concluído", normalizarNumero(render.textoPercentual())),
                () -> assertEquals(render.larguraTrilha() * 0.50, render.larguraPreenchimento(), 1.5)
        );
    }

    @Test
    @DisplayName("usa verde a partir de 75 por cento e mantém mensagem de concluída em 100 por cento")
    void usaVerdeAPartirDe75PorCento() {
        Meta meta = new Meta(UUID.randomUUID(), "Carro", 1000_00L, 1000_00L);

        MetaRender render = renderizar(meta);

        assertAll(
                () -> assertEquals(1.0, render.progresso(), 0.001),
                () -> assertTrue(render.classesBarra().contains("progress-bar-meta-verde")),
                () -> assertEquals("Meta concluída!", render.textoPercentual()),
                () -> assertTrue(render.classesPercentual().contains("cell-meta-concluida")),
                () -> assertEquals(render.larguraTrilha(), render.larguraPreenchimento(), 1.5)
        );
    }

    @Test
    @DisplayName("limpa a célula ao receber item vazio")
    void limpaCelulaAoReceberItemVazio() {
        MetaCell cell = FxTestSupport.callOnFxThreadAndWait(() -> {
            MetaCell localCell = new MetaCell();
            localCell.updateItem(null, true);
            return localCell;
        });

        assertAll(
                () -> assertNull(cell.getGraphic()),
                () -> assertNull(cell.getText())
        );
    }

    private static MetaRender renderizar(Meta meta) {
        return FxTestSupport.callOnFxThreadAndWait(() -> {
            MetaCell cell = new MetaCell();
            cell.updateItem(meta, false);
            cell.setPrefWidth(420);
            cell.resize(420, 96);

            StackPane root = new StackPane(cell);
            Scene scene = new Scene(root, 420, 120);
            root.applyCss();
            root.layout();
            cell.applyCss();
            cell.layout();

            StackPane progressTrack = obterTrilha(cell);
            Region progressFill = obterPreenchimento(cell);
            Label percentLabel = obterLabelPercentual(cell);

            return new MetaRender(
                    meta.progresso(),
                    String.join(" ", progressFill.getStyleClass()),
                    percentLabel.getText(),
                    String.join(" ", percentLabel.getStyleClass()),
                    progressTrack.getWidth(),
                    progressFill.getWidth()
            );
        });
    }

    private static StackPane obterTrilha(MetaCell cell) {
        return localizarNode(cell.getGraphic(), StackPane.class, "meta-progress-track");
    }

    private static Region obterPreenchimento(MetaCell cell) {
        return localizarNode(cell.getGraphic(), Region.class, "meta-progress-fill");
    }

    private static Label obterLabelPercentual(MetaCell cell) {
        return localizarNode(cell.getGraphic(), Label.class, "meta-progress-label");
    }

    private static <T extends Node> T localizarNode(Node root, Class<T> tipo, String styleClass) {
        T encontrado = buscarNode(root, tipo, styleClass);
        if (encontrado == null) {
            throw new AssertionError("Nao foi possivel localizar node com styleClass '%s'".formatted(styleClass));
        }
        return encontrado;
    }

    private static <T extends Node> T buscarNode(Node root, Class<T> tipo, String styleClass) {
        if (tipo.isInstance(root) && root.getStyleClass().contains(styleClass)) {
            return tipo.cast(root);
        }

        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T encontrado = buscarNode(child, tipo, styleClass);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }

        return null;
    }

    private static String normalizarNumero(String texto) {
        return texto.replace(',', '.');
    }

    private record MetaRender(
            double progresso,
            String classesBarra,
            String textoPercentual,
            String classesPercentual,
            double larguraTrilha,
            double larguraPreenchimento
    ) {
    }
}
