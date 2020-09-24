package server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StartServer extends Application {

    Controller controller;

    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader mainWindowLoader = new FXMLLoader();
        mainWindowLoader.setLocation(getClass().getResource("../simpleGUIForServer.fxml"));
        Parent mainRoot = mainWindowLoader.load();
        primaryStage.setTitle("GUI for server");
        primaryStage.setScene(new Scene(mainRoot, 600, 400));
        System.out.println("show mainWindow");
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.exit();
                System.out.println("Server close");
            }
        });
        controller = mainWindowLoader.getController();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static String getCurTime() {
        Calendar calendar = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        return dateFormat.format(calendar.getTime());
    }
}
