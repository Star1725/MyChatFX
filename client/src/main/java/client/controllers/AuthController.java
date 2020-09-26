package client.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthController implements Initializable {
    public Button regBtn;
    public Label labelSecToClose;
    public TextField txtFldNameHost;
    public Button btnSetNameHost;
    public TextField txtFldForPort;

    public void setTimeout(int timeout) {
        this.timeout = timeout/1000;
        System.out.println(String.format("class AuthController - timeout = %s sec", this.timeout));
    }

    private int timeout;

    public Stage getRegStage() {
        return regStage;
    }

    private Stage regStage;
    public TextField loginTxtFld;
    public PasswordField passTxtFld;
    public Button loginBtn;

    public void setAuthentication(boolean authentication) {
        isAuthentication = authentication;
        System.out.println("class AuthController - Аутентификация - " + authentication);
    }

    private boolean isAuthentication;

    private Thread authThread;

    public void setReadWriteNetHandler(ReadWriteNetHandler readWriteNetHandler) {
        this.readWriteNetHandler = readWriteNetHandler;
    }

    private ReadWriteNetHandler readWriteNetHandler;
    private RegController regController;

    public void onActionRegBtn(ActionEvent actionEvent) {
        System.out.println("class AuthController - Попытка регистрации");
        if (readWriteNetHandler.getSocket() == null || readWriteNetHandler.getSocket().isClosed()){
            readWriteNetHandler.connectAndReadChat();
        }
        readWriteNetHandler.sendMsg("/timeout_off");
        if (authThread != null){
            Platform.runLater(() -> {
                labelSecToClose.setVisible(false);
            });
        }
        createRegWindow();
        regStage.show();
    }

    public void onActionLoginBtn(ActionEvent actionEvent) {
        System.out.println("class AuthController - нажали кнопку - войти");
        if (readWriteNetHandler.getSocket() == null || readWriteNetHandler.getSocket().isClosed()){
            readWriteNetHandler.connectAndReadChat();
        }
        Platform.runLater(() -> {
            txtFldNameHost.setText(readWriteNetHandler.getIPaddress());
            txtFldForPort.setText(String.valueOf(readWriteNetHandler.getPort()));
        });
        if (!loginTxtFld.equals("") || !passTxtFld.getText().isEmpty() || !txtFldNameHost.getText().isEmpty()){
            labelSecToClose.setVisible(true);
            readWriteNetHandler.tryAuth(loginTxtFld.getText().trim().toLowerCase(), passTxtFld.getText().trim());
            //поток ожидания аутентификации
            if (authThread == null || !authThread.isAlive()){
                authThread = new Thread(() -> {
                while (!isAuthentication){
                    try {
                        Thread.sleep(1000);
                        System.out.println("class AuthController - " + Thread.currentThread().getName() + " - до конца аутентификации осталось - " + (timeout -= 1));
                        Platform.runLater(() -> {
                            labelSecToClose.setText(String.valueOf(timeout));
                        });
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (timeout == 0){
                        Platform.runLater(() -> {
                            labelSecToClose.setText("");
                        });
                        break;
                    }
                    System.out.println("class AuthController - " + Thread.currentThread().getName() + " - isAuthentication - " + isAuthentication);
                    }
                    if (isAuthentication){
                        Platform.runLater(() -> {
                            loginBtn.getScene().getWindow().hide();
                            labelSecToClose.setText("");
                        });
                    }
                });
                authThread.setDaemon(true);
                authThread.start();
            }
        }
    }

    public void showAlertWindow(String title, String text){
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(text);
            alert.setHeaderText("");
            alert.showAndWait();
        });
    }

    private void createRegWindow(){
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/windows/regWindow.fxml"));
        Parent parent = null;
        try {
            parent = loader.load();
        } catch (IOException | IllegalStateException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        regStage = new Stage();
            regStage.setTitle("Регистрация");
            regStage.setScene(new Scene(parent, 400, 230));
            regController = loader.getController();
            regController.setReadWriteNetHandler(readWriteNetHandler);
            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.setResizable(false);
            regController.setAuthController(this);
    }
    
    @FXML
    public void onActionSetNameHostAndPort(ActionEvent actionEvent) {
        readWriteNetHandler.setIPaddress(txtFldNameHost.getText().trim());
        readWriteNetHandler.setPort(Integer.parseInt(txtFldForPort.getText().trim()));
        Platform.runLater(() -> {
            txtFldNameHost.setText(readWriteNetHandler.getIPaddress());
            txtFldForPort.setText(String.valueOf(readWriteNetHandler.getPort()));
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }
}

