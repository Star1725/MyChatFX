package client.controllers;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ReadWriteNetHandler {
    public Socket getSocket() {
        return socket;
    }

    private Socket socket;

    public void setPort(int port) {
        this.port = port;
    }

    private int port = 8189;

    public void setIPaddress(String IPaddress) {
        this.IPaddress = IPaddress;
    }

    public int getPort() {
        return port;
    }

    public String getIPaddress() {
        return IPaddress;
    }

    private String IPaddress = "localhost";
    private DataInputStream inputStreamNet;
    private DataOutputStream outputStreamNet;

    private ChatController chatController;

    private AuthController authController;

    public void setRegController(RegController regController) {
        this.regController = regController;
    }

    private RegController regController;

    public ReadWriteNetHandler(ChatController chatController, AuthController authController) {
        this.chatController = chatController;
        this.authController = authController;
    }

    public void connectAndReadChat(){
        try {
            socket = new Socket(IPaddress, port);
            System.out.println("class ReadWriteNetHandler - socket: getInetAddress - " + socket.getInetAddress() + "\n" +
                    "        getReuseAddress - " + socket.getReuseAddress() + "\n" +
                    "        getLocalAddress - " + socket.getLocalAddress() + "\n" +
                    "        getLocalPort - " + socket.getLocalPort() + "\n" +
                    "        getRemoteSocketAddress - " + socket.getRemoteSocketAddress() + "\n" +
                    "        getLocalSocketAddress - " + socket.getLocalSocketAddress() + "\n" +
                    "        getPort - " + socket.getPort() + "\n" +
                    "        getTcpNoDelay - " + socket.getTcpNoDelay());
            inputStreamNet = new DataInputStream(socket.getInputStream());
            outputStreamNet = new DataOutputStream(socket.getOutputStream());
            System.out.println("class ReadWriteNetHandler - Создали поток для приема данных от сервера");
            Thread threadReadMsgFromNet = new Thread(() -> {
                try {
                    //аутентификация
                    while (true){
                        System.out.println("class ReadWriteNetHandler - Ждем данные аутентификации от сервера");
                        String data = inputStreamNet.readUTF();
                        String[] token = data.split("\\s", 2);
                        System.out.println("class ReadWriteNetHandler - Цикл аутентификации получил от сервера данные: " + data);
                        //установка timeout соединения при неудачной авторизации
                        if(data.startsWith("/timeout_on")){
                            authController.setTimeout(Integer.parseInt(token[1]));
                        } else if (data.startsWith("/authok")){
                            chatController.setTitle(token[1]);
                            authController.setAuthentication(true);
                            break;
                        } else if(data.startsWith("/error1")){
                            authController.showAlertWindow("Ошибка", token[1]);
                        } else if (data.startsWith("/error2")){
                            authController.showAlertWindow("Ошибка", token[1]);
                        } else if (data.startsWith("/regok")){
                            authController.showAlertWindow("Информация", token[1]);
                            Platform.runLater(() -> {
                                authController.getRegStage().hide();
                            });
                        } else if (data.startsWith("/regno")) {
                            authController.showAlertWindow("Ошибка", token[1]);
                            clearTxtFldsForRegController();
                        } else if (data.startsWith("/endtime")) {
                            authController.showAlertWindow("Ошибка", token[1]);
                            throw new IOException(token[1]);
                        }
                    }
                    //работа
                    while (true){
                        System.out.println("class ReadWriteNetHandler - Цикл работы, ждем сообщения от сервера");
                        String msg = inputStreamNet.readUTF();
                        if (msg.startsWith("/")){
                            System.out.println("class ReadWriteNetHandler - получили служебное:");
                            if (msg.equals("/end")){
                                break;
                            }
                            if (msg.startsWith("/clientlist")){
                                String[] token = msg.split("\\s+");
                                chatController.updatedListViewContacts(token);
                            }
                            if (msg.startsWith("/w")){
                                System.out.println("class ReadWriteNetHandler - получил личное сообщение " + msg);
                                chatController.getMsg(msg);
                            }
                            if (msg.startsWith("/his")){
                                System.out.println("class ReadWriteNetHandler - получил сообщение из истории чата " + msg);
                                chatController.getMsg(msg);
                            }
                        } else {
                            chatController.getMsg(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                        authController.setAuthentication(false);
                        Platform.runLater(() -> {
                            chatController.vBoxForFieldChat.getChildren().clear();
                            chatController.splitPaneMainWindow.setVisible(false);
                            ((Stage)authController.loginBtn.getScene().getWindow()).show();
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            threadReadMsgFromNet.setDaemon(true);
            threadReadMsgFromNet.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearTxtFldsForRegController() {
        regController.loginTxtFldForReg.clear();
        regController.nickNameTxtFldForReg.clear();
        regController.passTxtFldForReg.clear();
        regController.passTxtFldForRegConfirm.clear();
    }

    public void sendMsg(String msg){
        try {
            outputStreamNet.writeUTF(msg);
            System.out.println("class ReadWriteNetHandler - отправил сообщение: " + msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryAuth(String log, String pas) {
        if (socket != null || socket.isClosed()){
            System.out.println(socket.toString());
            try {
                System.out.println("class ReadWriteNetHandler - Отправляем сереверу логин: " + log + " и пароль: " + pas);
                outputStreamNet.writeUTF(String.format("/auth %s %s", log, pas));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void tryReg(String login, String password, String nickName){
        String msgReg = String.format("/reg %s %s %s", login, password, nickName);
        try {
            outputStreamNet.writeUTF(msgReg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(msgReg);
    }
}
