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

    static boolean createdFileDB = false;
    Controller controller;

    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader mainWindowLoader = new FXMLLoader();
        mainWindowLoader.setLocation(getClass().getResource("../simpleGUIForServer.fxml"));
        Parent mainRoot = mainWindowLoader.load();
        primaryStage.setTitle("GUI for server");
        primaryStage.setScene(new Scene(mainRoot, 600, 80));
        System.out.println("show mainWindow");
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                DatabaseHandler.disconnect();
                Platform.exit();
                System.out.println("Server close");
            }
        });
        controller = mainWindowLoader.getController();
        createdFileDB();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static String getCurTime() {
        Calendar calendar = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        return dateFormat.format(calendar.getTime());
    }

    private void createdFileDB(){
        File newFile = new File("myDB.db");
        // создадим новый файл
        try {
            createdFileDB = newFile.createNewFile();

        } catch (IOException e) {
            e.printStackTrace();
        }
        if(createdFileDB){
            System.out.println("class StartServer - файл базы данных создан");
            DatabaseHandler.createTablesInDB();
        }
        else System.out.println("class StartServer - файл myDB уже был создан");
    }
}

