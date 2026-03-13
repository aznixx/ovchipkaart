package nl.ovchipkaart.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class OVChipkaartApp extends Application {

    private static Stage mainStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainStage = primaryStage;
        showLoginScreen();
    }

    public static void showLoginScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(OVChipkaartApp.class.getResource("login-view.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 450, 500);
        scene.getStylesheets().add(OVChipkaartApp.class.getResource("style.css").toExternalForm());
        mainStage.setTitle("OV-chipkaart - Login");
        mainStage.setScene(scene);
        mainStage.show();
    }

    public static void showMainScreen(nl.ovchipkaart.model.Account account) throws Exception {
        FXMLLoader loader = new FXMLLoader(OVChipkaartApp.class.getResource("main-view.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.setAccount(account);
        Scene scene = new Scene(root, 850, 650);
        scene.getStylesheets().add(OVChipkaartApp.class.getResource("style.css").toExternalForm());
        mainStage.setTitle("OV-chipkaart - " + account.getName());
        mainStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
