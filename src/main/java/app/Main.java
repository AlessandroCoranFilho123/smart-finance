package app;

import app.database.Database;
import app.util.CssManager;
import app.util.IconManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Database.inicializar(); // Garante que o schema está atualizado
        FXMLLoader loader = new FXMLLoader( // Prepara FXML da tela inicial
                getClass().getResource("/app/view/main.fxml")
        );
        Scene scene = new Scene(loader.load()); // Carrega a interface do FXML
        CssManager.aplicarCss(scene); // Carrega o CSS (package util CssManager)
        IconManager.setAppIcon(stage); // Carrega Ícone do aplicativo (package util IconManager)
        stage.setTitle("Smart Finance v2.1.0"); // Título do aplicativo
        stage.setMinWidth(1200); // Define a largura mínima
        stage.setMinHeight(700); // Define a altura mínima
        stage.setScene(scene); // Carrega a cena na janela
        stage.show(); // Exibe a janela
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        launch(args);
    }
}
