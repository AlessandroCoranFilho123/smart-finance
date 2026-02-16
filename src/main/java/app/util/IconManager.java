package app.util;

import app.model.Categoria;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

/* Carrega e faz cache de ícones,
mapeia categorias para ícones,
defini ícone da janela principal e
fornece fallbacks para ícones não encontrados */

public final class IconManager {

    private IconManager() {
    }

    // Cache para evitar recarregamento
    private static final Map<String, Image> CACHE = new HashMap<>();

    // Ícone do aplicativo
    private static final String APP_ICON = "/app/icons/app_64.png";

    // Ícones da sidebar
    private static final String ICON_INICIO = "/app/icons/categoria/inicio.png";
    private static final String ICON_TRANSACAO = "/app/icons/transaction_64.png";
    private static final String ICON_META = "/app/icons/metas_64.png";
    private static final String ICON_TEMA = "/app/icons/sun_24.png";
    private static final String ICON_CARTEIRA = "/app/icons/carteira.png";

    private static final Map<Categoria, String> CATEGORIA_ICONS = new HashMap<>();

    static {
        // Entradas
        CATEGORIA_ICONS.put(Categoria.Salario, "/app/icons/categoria/salario.png");
        CATEGORIA_ICONS.put(Categoria.Emprestimo, "/app/icons/categoria/emprestimo.png");
        CATEGORIA_ICONS.put(Categoria.RetirarMeta, "/app/icons/categoria/retirar-meta.png");

        // Despesas Fixas
        CATEGORIA_ICONS.put(Categoria.Aluguel, "/app/icons/rent.png");
        CATEGORIA_ICONS.put(Categoria.Internet, "/app/icons/internet.png");
        CATEGORIA_ICONS.put(Categoria.Agua, "/app/icons/water.png");
        CATEGORIA_ICONS.put(Categoria.Luz, "/app/icons/electricity.png");

        // Despesas Variáveis
        CATEGORIA_ICONS.put(Categoria.Alimentacao, "/app/icons/food.png");
        CATEGORIA_ICONS.put(Categoria.Transporte, "/app/icons/transport.png");
        CATEGORIA_ICONS.put(Categoria.Compras, "/app/icons/shopping.png");
        CATEGORIA_ICONS.put(Categoria.Educacao, "/app/icons/education.png");
        CATEGORIA_ICONS.put(Categoria.AdicionarMeta, "/app/icons/add-goal.png");
        CATEGORIA_ICONS.put(Categoria.Outros, "/app/icons/other.png");
    }

    public static void setAppIcon(Stage stage) {
        try {
            Image icon = getImage(APP_ICON);
            if (icon != null) {
                stage.getIcons().add(icon);
            } else {
                System.err.println("Ícone do app não encontrado em: " + APP_ICON);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar ícone do app: " + e.getMessage());
        }
    }

    public static Image getInicioIcon() {
        return getImage(ICON_INICIO);
    }

    public static Image getTransacaoIcon() {
        return getImage(ICON_TRANSACAO);
    }

    public static Image getMetaIcon() {
        return getImage(ICON_META);
    }

    public static Image getTemaIcon() {
        return getImage(ICON_TEMA);
    }

    public static Image getCarteiraIcon() {
        return getImage(ICON_CARTEIRA);
    }

    public static Image getImage(String path) {
        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }

        try {
            var resourceStream = IconManager.class.getResourceAsStream(path);

            if (resourceStream != null) {
                Image image = new Image(resourceStream);
                CACHE.put(path, image);
                return image;
            } else {
                System.err.println("Recurso não encontrado: " + path);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Erro ao carregar ícone " + path + ": " + e.getMessage());
            return null;
        }
    }

    public static Image getImage(String path, double width, double height) {
        String cacheKey = path + "_" + width + "x" + height;

        if (CACHE.containsKey(cacheKey)) {
            return CACHE.get(cacheKey);
        }

        try {
            var resourceStream = IconManager.class.getResourceAsStream(path);

            if (resourceStream != null) {
                Image image = new Image(resourceStream, width, height, true, true);
                CACHE.put(cacheKey, image);
                return image;
            } else {
                System.err.println("Recurso não encontrado: " + path);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Erro ao carregar ícone " + path + ": " + e.getMessage());
            return null;
        }
    }

    public static boolean iconExists(String path) {
        return IconManager.class.getResourceAsStream(path) != null;
    }

    public static void clearCache() {
        CACHE.clear();
        System.out.println("Cache de ícones limpo");
    }

    public static int getCacheSize() {
        return CACHE.size();
    }

    public static void preloadCommonIcons() {
        System.out.println("Pre-carregando ícones...");

        getImage(ICON_INICIO);
        getImage(ICON_TRANSACAO);
        getImage(ICON_META);
        getImage(ICON_TEMA);
        getImage(ICON_CARTEIRA);

        CATEGORIA_ICONS.values().forEach(IconManager::getImage);

        System.out.println("Ícones pre-carregados: " + getCacheSize());
    }
}