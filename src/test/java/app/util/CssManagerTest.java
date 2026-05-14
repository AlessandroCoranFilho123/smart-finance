package app.util;

import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CssManager")
class CssManagerTest {

    @Test
    @DisplayName("aplicarCss adiciona stylesheets à scene")
    void adicionarStylesheets() {
        Scene scene = new Scene(new VBox());
        int initialSize = scene.getStylesheets().size();
        
        CssManager.aplicarCss(scene);
        
        assertTrue(scene.getStylesheets().size() > initialSize);
    }

    @Test
    @DisplayName("aplicarCss adiciona exatamente 4 stylesheets")
    void quantidadeCorretaDeStylesheets() {
        Scene scene = new Scene(new VBox());
        CssManager.aplicarCss(scene);
        assertEquals(4, scene.getStylesheets().size());
    }

    @Test
    @DisplayName("aplicarCss lança NPE para scene nula")
    void sceneNulaLancaNPE() {
        assertThrows(NullPointerException.class, () -> CssManager.aplicarCss(null));
    }
}
