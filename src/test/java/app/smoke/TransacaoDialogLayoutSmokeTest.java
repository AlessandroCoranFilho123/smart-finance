package app.smoke;

import app.database.Database;
import app.support.FxTestSupport;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Smoke - layout do dialog de transação")
class TransacaoDialogLayoutSmokeTest {

    private static final double FORM_CONTROL_HEIGHT = 40.0;

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
    @DisplayName("categoria e meta têm a mesma altura do campo data")
    void categoriaEMetaTemMesmaAlturaDoCampoData() {
        Database.overrideUrlForTests("jdbc:sqlite:" + tempDir.resolve("transacao-dialog-layout.db").toAbsolutePath());
        Database.inicializar();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/transacao_dialog.fxml"));
        FxTestSupport.callOnFxThreadAndWait(loader::load);

        DatePicker dateTransacao = (DatePicker) loader.getNamespace().get("dateTransacao");
        ComboBox<?> cmbCategoria = (ComboBox<?>) loader.getNamespace().get("cmbCategoria");
        ComboBox<?> cmbMeta = (ComboBox<?>) loader.getNamespace().get("cmbMeta");

        assertAll(
                () -> assertEquals(FORM_CONTROL_HEIGHT, dateTransacao.getPrefHeight()),
                () -> assertEquals(dateTransacao.getPrefHeight(), cmbCategoria.getPrefHeight()),
                () -> assertEquals(dateTransacao.getPrefHeight(), cmbMeta.getPrefHeight())
        );
    }
}
