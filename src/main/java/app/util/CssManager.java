package app.util;

import javafx.scene.Scene;

import java.util.List;

public final class CssManager {

    private CssManager() {
    }

    private static final List<String> BASE_STYLES = List.of(
            "/app/style/base.css",
            "/app/style/views.css",
            "/app/style/theme/light.css",
            "/app/style/theme/dark.css"
    );

    public static void aplicarCss(Scene scene) {
        BASE_STYLES.forEach(path -> {
            var resource = CssManager.class.getResource(path);
            if (resource != null) {
                scene.getStylesheets().add(resource.toExternalForm());
            } else {
                System.err.println("Erro: Arquivo CSS não encontrado no caminho: " + path);
            }
        });
    }
}
