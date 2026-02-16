package app;

import app.util.CssManager;
import app.util.IconManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader( // Carrega tela inicial do FXML
                getClass().getResource("/app/view/main.fxml")
        );

        Scene scene = new Scene(loader.load());
        CssManager.aplicarCss(scene); // Carrega o CSS
        IconManager.setAppIcon(stage); // Ícone do aplicativo
        stage.setTitle("Smart Finance v1.2.0"); // Título do aplicativo
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
