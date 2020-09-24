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
    public TextField txtFldILocalPAddress;
    public Button btnSendIP;
    public Button btnSendPort;
    public TextArea txtAreaForConsole;
    public Button btnStartServer;
    public TextField txtFldForPort;
    public Circle circleStartServer;
    public Label labelCountOfClients;

    public final String COUNT_CLIENTS = "Кол-во клиентов - ";

    public void setServer(Server server) {
        this.server = server;
    }

    private Server server;

    public void onActionSendIP(ActionEvent actionEvent) {
    }

    public void onActionSetPort(ActionEvent actionEvent) {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        labelCountOfClients.setText(COUNT_CLIENTS);
    }

    public void onActionStartServer(ActionEvent actionEvent) {
        if (server == null){
            server = new Server(this);
            Platform.runLater(() -> {
                txtFldForPort.setText(String.valueOf(server.getPort()));
                btnStartServer.setText("Restart server");
            });
        } else {
            server.broadcastMsgEnd("/end");
            txtAreaForConsole.clear();
            this.labelCountOfClients.setText(COUNT_CLIENTS);
            server.getThread().interrupt();
            try {
                server.getServerSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            server = new Server(this);
        }
    }

    public void showInGUI(String info){
        Platform.runLater(() -> {
            txtAreaForConsole.appendText(info);
        });
    }
}
