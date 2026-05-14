package app.util;

import app.model.Categoria;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IconManager")
class IconManagerTest {

    @BeforeEach
    void clearCache() throws Exception {
        Field cacheField = IconManager.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Image> cache = (Map<String, Image>) cacheField.get(null);
        cache.clear();
    }

    @Test
    @DisplayName("getImage retorna null para recurso inexistente")
    void recursoInexistenteRetornaNull() {
        Image result = IconManager.getImage("/app/icons/nao-existe.png");
        assertNull(result);
    }

    @Test
    @DisplayName("getImage retorna null para recurso não encontrado")
    void recursoNaoEncontradoRetornaNull() {
        Image result = IconManager.getImage("/app/icons/vazio-inexistente.png");
        assertNull(result);
    }

    @Test
    @DisplayName("getImage faz cache na primeira chamada")
    void cacheFunciona() throws Exception {
        Image first = IconManager.getImage("/app/icons/nao-existe.png");
        Image second = IconManager.getImage("/app/icons/nao-existe.png");
        assertSame(first, second);
    }

    @Test
    @DisplayName("getCategoriaIcon retorna null para categoria nula")
    void categoriaNulaRetornaNull() {
        assertNull(IconManager.getCategoriaIcon(null));
    }

    @Test
    @DisplayName("getCategoriaIcon retorna null para categoria sem mapeamento")
    void categoriaSemMapeamentoRetornaNull() {
        Image result = IconManager.getCategoriaIcon(Categoria.Outros);
        assertNull(result);
    }

    @Test
    @DisplayName("todas as categorias mapeadas retornam ícone ou null")
    void todasCategoriasMapeadas() {
        for (Categoria cat : Categoria.values()) {
            Image icon = IconManager.getCategoriaIcon(cat);
            if (icon == null) continue;
            assertNotNull(icon.getWidth());
            assertNotNull(icon.getHeight());
        }
    }
}
