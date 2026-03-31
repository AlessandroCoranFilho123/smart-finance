package app.smoke;

import app.Main;
import app.controller.MainController;
import app.controller.MetaDialogController;
import app.controller.MetasViewController;
import app.controller.TransacaoDialogController;
import app.controller.TransacoesViewController;
import app.database.Database;
import app.support.FxTestSupport;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Smoke - inicialização e FXML")
class AppSmokeTest {

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
    @DisplayName("carrega todos os FXML com seus controllers reais")
    void carregaTodosOsFxml() {
        Database.overrideUrlForTests(jdbcUrl(tempDir.resolve("smoke-fxml.db")));
        Database.inicializar();

        List<FxmlExpectation> expectations = List.of(
                new FxmlExpectation("/app/view/main.fxml", MainController.class),
                new FxmlExpectation("/app/view/meta_dialog.fxml", MetaDialogController.class),
                new FxmlExpectation("/app/view/metas_view.fxml", MetasViewController.class),
                new FxmlExpectation("/app/view/transacao_dialog.fxml", TransacaoDialogController.class),
                new FxmlExpectation("/app/view/transacoes_view.fxml", TransacoesViewController.class)
        );

        for (FxmlExpectation expectation : expectations) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(expectation.resourcePath()));
            Parent root = FxTestSupport.callOnFxThreadAndWait(loader::load);

            assertAll(
                    () -> assertNotNull(root, "FXML deveria gerar uma raiz"),
                    () -> assertNotNull(loader.getController(), "FXML deveria instanciar controller"),
                    () -> assertInstanceOf(expectation.controllerType(), loader.getController())
            );
        }
    }

    @Test
    @DisplayName("Main.start sobe a janela principal com cena, CSS e ícone")
    void mainStartSobeAplicacao() {
        Database.overrideUrlForTests(jdbcUrl(tempDir.resolve("smoke-main.db")));

        Stage stage = FxTestSupport.callOnFxThreadAndWait(() -> {
            Stage localStage = new Stage();
            new Main().start(localStage);
            return localStage;
        });

        try {
            Scene scene = stage.getScene();
            assertAll(
                    () -> assertTrue(stage.isShowing()),
                    () -> assertEquals("Smart Finance v2.0", stage.getTitle()),
                    () -> assertEquals(1200.0, stage.getMinWidth()),
                    () -> assertEquals(700.0, stage.getMinHeight()),
                    () -> assertNotNull(scene),
                    () -> assertNotNull(scene.getRoot()),
                    () -> assertFalse(scene.getStylesheets().isEmpty()),
                    () -> assertFalse(stage.getIcons().isEmpty())
            );
        } finally {
            FxTestSupport.runOnFxThreadAndWait(stage::close);
        }
    }

    private static String jdbcUrl(Path path) {
        return "jdbc:sqlite:" + path.toAbsolutePath();
    }

    private record FxmlExpectation(String resourcePath, Class<?> controllerType) {
    }
}
