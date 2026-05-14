package app.smoke;

import app.database.Database;
import app.support.FxTestSupport;
import app.util.CssManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Smoke - dashboard no tema escuro")
class DarkThemeDashboardSmokeTest {

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
    @DisplayName("card de saldo usa a mesma superfície dos demais cards")
    void cardDeSaldoUsaSuperficieEscuraNeutra() {
        Database.overrideUrlForTests("jdbc:sqlite:" + tempDir.resolve("dark-dashboard.db").toAbsolutePath());
        Database.inicializar();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/main.fxml"));
        Parent root = FxTestSupport.callOnFxThreadAndWait(loader::load);

        FxTestSupport.runOnFxThreadAndWait(() -> {
            root.getStyleClass().add("dark-theme");
            Scene scene = new Scene(root);
            CssManager.aplicarCss(scene);
            root.applyCss();
            root.layout();
        });

        Region saldoBanner = (Region) root.lookup(".saldo-banner");
        Label saldoLabel = (Label) root.lookup(".saldo-label");
        Label saldoValor = (Label) root.lookup(".saldo-valor");
        VBox dashboardContent = (VBox) root.lookup("#dashboardContent");

        assertNotNull(saldoBanner);
        assertNotNull(dashboardContent);
        assertEquals(20.0, dashboardContent.getPadding().getTop());
        assertEquals(20.0, dashboardContent.getPadding().getBottom());
        assertEquals(true, saldoBanner.getStyleClass().contains("card"));
        assertFalse(saldoBanner.getBackground().getFills().isEmpty());
        assertEquals(Color.web("#1b2840"), saldoBanner.getBackground().getFills().getFirst().getFill());
        assertEquals(Color.web("#94a3b8"), saldoLabel.getTextFill());
        assertEquals(Color.web("#f8fafc"), saldoValor.getTextFill());
    }
}
