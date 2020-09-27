package client;

import client.controllers.AuthController;
import client.controllers.ChatController;
import client.controllers.ReadWriteNetHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class StartClient extends Application {

    ChatController chatController;
    AuthController authController;
    ReadWriteNetHandler readWriteNetHandler;

    @Override
    public void start(Stage primaryStage) throws Exception{

        primaryStage.setTitle("Флудилка");
        FXMLLoader mainWindowLoader = new FXMLLoader();
        mainWindowLoader.setLocation(getClass().getResource("/windows/mainWindowChat.fxml"));
        //Parent mainRoot = mainWindowLoader.load(getClass().getResource("sample.windows/mainWindowChat.fxml"));
        Parent mainRoot = mainWindowLoader.load();
        primaryStage.setScene(new Scene(mainRoot, 500, 600));
        primaryStage.setMinWidth(360);
        primaryStage.setMinHeight(400);
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.out.println("Пользователь закрыл клиента " + chatController.getNickName());
                if (readWriteNetHandler.getSocket() != null && !readWriteNetHandler.getSocket().isClosed()){
                    readWriteNetHandler.sendMsg("/end");
                }
                Platform.exit();
            }
        });
        primaryStage.show();
        chatController = mainWindowLoader.getController();

        Stage modalStage = new Stage();
        modalStage.setTitle("Авторизация");
        FXMLLoader modalWindowLoader = new FXMLLoader();
        modalWindowLoader.setLocation(getClass().getResource("/windows/authWindow.fxml"));
        Parent modalRoot = modalWindowLoader.load();
        authController = modalWindowLoader.getController();
        modalStage.setScene(new Scene(modalRoot, 400, 300));
        modalStage.setResizable(false);
        modalStage.initModality(Modality.WINDOW_MODAL);
        modalStage.initOwner(primaryStage);
        modalStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.exit();
            }
        });
        modalStage.setOnHidden(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {

            }
        });
        modalStage.show();

        readWriteNetHandler = new ReadWriteNetHandler(chatController, authController);
        chatController.setReadWriteNetHandler(readWriteNetHandler);
        authController.setReadWriteNetHandler(readWriteNetHandler);
        authController.setChatController(chatController);
    }



    public static void main(String[] args) {
        launch(args);
    }
}
