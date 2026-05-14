package app.smoke;

import app.database.Database;
import app.support.FxTestSupport;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Smoke - rodapé da view de metas")
class MetasViewFooterSmokeTest {

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
    @DisplayName("exibe o progresso médio no rodapé da view de metas")
    void exibeProgressoMedioNoRodape() {
        Database.overrideUrlForTests("jdbc:sqlite:" + tempDir.resolve("metas-footer.db").toAbsolutePath());
        Database.inicializar();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/metas_view.fxml"));
        Parent root = FxTestSupport.callOnFxThreadAndWait(loader::load);
        Label lblProgresso = (Label) root.lookup("#lblProgresso");

        assertAll(
                () -> assertNotNull(lblProgresso),
                () -> assertTrue(lblProgresso.isVisible()),
                () -> assertTrue(lblProgresso.isManaged()),
                () -> assertEquals("Progresso médio: 0,0%", lblProgresso.getText())
        );
    }
}
