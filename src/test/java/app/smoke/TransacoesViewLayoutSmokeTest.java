package app.smoke;

import app.database.Database;
import app.support.FxTestSupport;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Smoke - layout da view de transações")
class TransacoesViewLayoutSmokeTest {

    private static final double FILTER_CONTROL_HEIGHT = 40.0;

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
    @DisplayName("controles de filtro têm a mesma altura")
    void controlesDeFiltroTemMesmaAltura() {
        Database.overrideUrlForTests("jdbc:sqlite:" + tempDir.resolve("transacoes-layout.db").toAbsolutePath());
        Database.inicializar();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/transacoes_view.fxml"));
        Parent root = FxTestSupport.callOnFxThreadAndWait(loader::load);

        TextField txtBusca = (TextField) loader.getNamespace().get("txtBusca");
        ComboBox<?> cmbTipo = (ComboBox<?>) loader.getNamespace().get("cmbTipo");
        ComboBox<?> cmbCategoria = (ComboBox<?>) loader.getNamespace().get("cmbCategoria");
        DatePicker dateInicio = (DatePicker) loader.getNamespace().get("dateInicio");
        DatePicker dateFim = (DatePicker) loader.getNamespace().get("dateFim");
        Button btnLimparFiltros = (Button) loader.getNamespace().get("btnLimparFiltros");

        assertAll(
                () -> assertEquals(FILTER_CONTROL_HEIGHT, txtBusca.getPrefHeight()),
                () -> assertEquals(FILTER_CONTROL_HEIGHT, cmbTipo.getPrefHeight()),
                () -> assertEquals(FILTER_CONTROL_HEIGHT, cmbCategoria.getPrefHeight()),
                () -> assertEquals(FILTER_CONTROL_HEIGHT, dateInicio.getPrefHeight()),
                () -> assertEquals(FILTER_CONTROL_HEIGHT, dateFim.getPrefHeight()),
                () -> assertEquals(FILTER_CONTROL_HEIGHT, btnLimparFiltros.getPrefHeight())
        );
    }
}
