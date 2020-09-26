package server;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public Button btnSendPort;
    public Button btnStartServer;
    public TextField txtFldForPort;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private int port = 8189;

    public Circle circleStartServer;
    public Label labelCountOfClients;

    public final String COUNT_CLIENTS = "Кол-во клиентов - ";

    private Server server;


    public void onActionSetPort(ActionEvent actionEvent) {
        port = Integer.parseInt(txtFldForPort.getText().trim());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtFldForPort.setFocusTraversable(false);
        btnStartServer.setFocusTraversable(true);
        labelCountOfClients.setText(COUNT_CLIENTS);
    }

    public void onActionStartServer(ActionEvent actionEvent) {
        if (server == null){
            System.out.println("class Controller - создаём Сервер");
            server = new Server(this);

            Platform.runLater(() -> {
                txtFldForPort.setText(String.valueOf(port));
                btnStartServer.setText("Restart server");
            });
        } else {
            server.broadcastMsgEnd("/end");
            this.labelCountOfClients.setText(COUNT_CLIENTS);
            server.getThread().interrupt();
            try {
                server.getServerSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("class Controller - перезапускаем Сервер, номер порта - " + txtFldForPort.getText().trim());
            server = new Server(this);
        }
    }
}
