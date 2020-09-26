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
                            authController.setAuthentication(true);
                            chatController.setTitle(token[1]);
                            break;
                        } else if(data.startsWith("/error1")){
                            System.out.println("class ReadWriteNetHandler - Показать окно ошибки \"" + data + "\"");
                            authController.showAlertWindow("Ошибка", token[1]);
                        } else if (data.startsWith("/error2")){
                            System.out.println("class ReadWriteNetHandler - Показать окно ошибки \" " + data + "\"");
                            authController.showAlertWindow("Ошибка", token[1]);
                        } else if (data.startsWith("/regok")){
                            System.out.println("class ReadWriteNetHandler - Показать окно удачной регистрации \" " + data + "\"");
                            authController.showAlertWindow("Информация", token[1]);
                            Platform.runLater(() -> {
                                authController.getRegStage().hide();
                            });
                        } else if (data.startsWith("/regno")) {
                            System.out.println("class ReadWriteNetHandler - Показать окно неудачной регистрации \" " + data + "\"");

                            authController.showAlertWindow("Ошибка", token[1]);
                        } else if (data.startsWith("/endtime")) {
                            System.out.println("class ReadWriteNetHandler - Показать окно timeout \"" + data + "\"");
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
                                System.out.println("class ReadWriteNetHandler - " + msg);
                                break;
                            }
                            if (msg.startsWith("/clientlist")){
                                System.out.println("class ReadWriteNetHandler - " + msg + ".Разбиваем сообщение на слова");
                                String[] token = msg.split("\\s+");
                                System.out.println("class ReadWriteNetHandler - отдаём chatController.updatedListViewContacts()");
                                chatController.updatedListViewContacts(token);
                            }
                            if (msg.startsWith("/w")){
                                System.out.println("class ReadWriteNetHandler - получил личное сообщение " + msg);
                                chatController.getMsg(msg);
                            }
                        } else {
                            System.out.println("class ReadWriteNetHandler - получил all сообщение " + msg);
                            chatController.getMsg(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("class ReadWriteNetHandler - disconnect from server");
                    try {
                        socket.close();
                        authController.setAuthentication(false);
                        Platform.runLater(() -> {
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
