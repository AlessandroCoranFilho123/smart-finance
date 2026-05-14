package app.smoke;

import app.database.Database;
import app.support.FxTestSupport;
import app.util.CssManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Smoke - contraste e bordas do tema claro")
class LightThemeContrastSmokeTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void initJavaFx() {
        FxTestSupport.initToolkit();
    }

    @AfterEach
    void clearDbOverride() {
        Database.clearOverrideUrlForTests();
    }

    @Test
    @DisplayName("rodapé da view de transações mantém contraste legível no tema claro")
    void rodapeDaViewDeTransacoesMantemContrasteLegivelNoTemaClaro() {
        LoadedView view = carregarView("/app/view/transacoes_view.fxml", tempDir.resolve("light-transacoes.db"));
        Label lblTotal = (Label) view.loader().getNamespace().get("lblTotal");

        Color textFill = FxTestSupport.callOnFxThreadAndWait(() -> (Color) lblTotal.getTextFill());

        assertAll(
                () -> assertNotNull(lblTotal),
                () -> assertEquals(Color.web("#475569"), textFill)
        );
    }

    @Test
    @DisplayName("inputs do tema claro exibem borda visível na tela de transação")
    void inputsDoTemaClaroExibemBordaVisivelNaTelaDeTransacao() {
        LoadedView view = carregarView("/app/view/transacao_dialog.fxml", tempDir.resolve("light-dialog.db"));
        TextField txtValor = (TextField) view.loader().getNamespace().get("txtValor");
        DatePicker dateTransacao = (DatePicker) view.loader().getNamespace().get("dateTransacao");
        ComboBox<?> cmbCategoria = (ComboBox<?>) view.loader().getNamespace().get("cmbCategoria");
        TextArea txtComentario = (TextArea) view.loader().getNamespace().get("txtComentario");

        assertAll(
                () -> assertBorderColor(txtValor, Color.web("#cbd5e1")),
                () -> assertBorderColor(dateTransacao, Color.web("#cbd5e1")),
                () -> assertBorderColor(cmbCategoria, Color.web("#cbd5e1")),
                () -> assertBorderColor(txtComentario, Color.web("#cbd5e1"))
        );
    }

    @Test
    @DisplayName("tipo entrada selecionado fica visível no tema claro")
    void tipoEntradaSelecionadoFicaVisivelNoTemaClaro() {
        LoadedView view = carregarView("/app/view/transacao_dialog.fxml", tempDir.resolve("light-toggle.db"));
        ToggleButton btnEntrada = (ToggleButton) view.loader().getNamespace().get("btnEntrada");

        Color textFill = FxTestSupport.callOnFxThreadAndWait(() -> (Color) btnEntrada.getTextFill());

        assertAll(
                () -> assertNotNull(btnEntrada),
                () -> assertTrue(btnEntrada.isSelected()),
                () -> assertEquals(Color.web("#059669"), textFill),
                () -> assertBorderColor(btnEntrada, Color.web("#10b981"))
        );
    }

    @Test
    @DisplayName("tipo saída selecionado fica visível no tema claro")
    void tipoSaidaSelecionadoFicaVisivelNoTemaClaro() {
        LoadedView view = carregarView("/app/view/transacao_dialog.fxml", tempDir.resolve("light-toggle-saida.db"));
        ToggleButton btnEntrada = (ToggleButton) view.loader().getNamespace().get("btnEntrada");
        ToggleButton btnSaida = (ToggleButton) view.loader().getNamespace().get("btnSaida");

        FxTestSupport.runOnFxThreadAndWait(() -> {
            btnSaida.setSelected(true);
            view.root().applyCss();
            view.root().layout();
        });

        Color textFill = FxTestSupport.callOnFxThreadAndWait(() -> (Color) btnSaida.getTextFill());

        assertAll(
                () -> assertNotNull(btnSaida),
                () -> assertFalse(btnEntrada.isSelected()),
                () -> assertTrue(btnSaida.isSelected()),
                () -> assertEquals(Color.web("#e11d48"), textFill),
                () -> assertBorderColor(btnSaida, Color.web("#f43f5e"))
        );
    }

    private LoadedView carregarView(String resourcePath, Path dbPath) {
        Database.overrideUrlForTests("jdbc:sqlite:" + dbPath.toAbsolutePath());
        Database.inicializar();

        FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
        Parent root = FxTestSupport.callOnFxThreadAndWait(loader::load);
        Scene scene = FxTestSupport.callOnFxThreadAndWait(() -> {
            Scene localScene = new Scene(root);
            CssManager.aplicarCss(localScene);
            root.applyCss();
            root.layout();
            return localScene;
        });
        return new LoadedView(loader, root, scene);
    }

    private static void assertBorderColor(Region region, Color expectedColor) {
        assertNotNull(region);
        assertNotNull(region.getBorder());
        assertFalse(region.getBorder().getStrokes().isEmpty());

        var stroke = region.getBorder().getStrokes().get(0);
        assertAll(
                () -> assertEquals(expectedColor, stroke.getTopStroke()),
                () -> assertTrue(stroke.getWidths().getTop() > 0),
                () -> assertTrue(stroke.getWidths().getRight() > 0),
                () -> assertTrue(stroke.getWidths().getBottom() > 0),
                () -> assertTrue(stroke.getWidths().getLeft() > 0)
        );
    }

    private record LoadedView(FXMLLoader loader, Parent root, Scene scene) {
    }
}
