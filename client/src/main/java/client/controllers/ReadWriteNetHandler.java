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
            System.out.println("socket: getInetAddress - " + socket.getInetAddress() + "\n" +
                    "        getReuseAddress - " + socket.getReuseAddress() + "\n" +
                    "        getLocalAddress - " + socket.getLocalAddress() + "\n" +
                    "        getLocalPort - " + socket.getLocalPort() + "\n" +
                    "        getRemoteSocketAddress - " + socket.getRemoteSocketAddress() + "\n" +
                    "        getLocalSocketAddress - " + socket.getLocalSocketAddress() + "\n" +
                    "        getPort - " + socket.getPort() + "\n" +
                    "        getTcpNoDelay - " + socket.getTcpNoDelay());
            showInGUI("socket: getInetAddress - " + socket.getInetAddress() + "\n" +
                    "        getReuseAddress - " + socket.getReuseAddress() + "\n" +
                    "        getLocalAddress - " + socket.getLocalAddress() + "\n" +
                    "        getLocalPort - " + socket.getLocalPort() + "\n" +
                    "        getRemoteSocketAddress - " + socket.getRemoteSocketAddress() + "\n" +
                    "        getLocalSocketAddress - " + socket.getLocalSocketAddress() + "\n" +
                    "        getPort - " + socket.getPort() + "\n" +
                    "        getTcpNoDelay - " + socket.getTcpNoDelay() + "\n");
            inputStreamNet = new DataInputStream(socket.getInputStream());
            outputStreamNet = new DataOutputStream(socket.getOutputStream());
            System.out.println("Создали поток для приема данных от сервера");
            showInGUI("Создали поток для приема данных от сервера\n");
            Thread threadReadMsgFromNet = new Thread(() -> {
                try {
                    //аутентификация
                    while (true){
                        System.out.println("Ждем данные аутентификации от сервера");
                        showInGUI("Ждем данные аутентификации от сервера\n");
                        String data = inputStreamNet.readUTF();
                        String[] token = data.split("\\s", 2);
                        System.out.println("Цикл аутентификации получил от сервера данные: " + data);
                        showInGUI("Цикл аутентификации получил от сервера данные: " + data + "\n");
                        //установка timeout соединения при неудачной авторизации
                        if(data.startsWith("/timeout_on")){
                            authController.setTimeout(Integer.parseInt(token[1]));
                        } else if (data.startsWith("/authok")){
                            authController.setAuthentication(true);
                            chatController.setTitle(token[1]);
                            break;
                        } else if(data.startsWith("/error1")){
                            System.out.println("Показать окно ошибки \"" + data + "\"");
                            showInGUI("Показать окно ошибки \"" + data + "\"\n");
                            authController.showAlertWindow("Ошибка", token[1]);
                        } else if (data.startsWith("/error2")){
                            System.out.println("Показать окно ошибки \" " + data + "\"");
                            showInGUI("Показать окно ошибки \" " + data + "\"\n");
                            authController.showAlertWindow("Ошибка", token[1]);
                        } else if (data.startsWith("/regok")){
                            System.out.println("Показать окно удачной регистрации \" " + data + "\"");
                            showInGUI("Показать окно удачной регистрации \" " + data + "\"\n");
                            authController.showAlertWindow("Информация", token[1]);
                            Platform.runLater(() -> {
                                authController.getRegStage().hide();
                            });
                        } else if (data.startsWith("/regno")) {
                            System.out.println("Показать окно неудачной регистрации \" " + data + "\"");
                            showInGUI("Показать окно неудачной регистрации \" " + data + "\"\n");
                            authController.showAlertWindow("Ошибка", token[1]);
                        } else if (data.startsWith("/endtime")) {
                            System.out.println("Показать окно timeout \"" + data + "\"");
                            showInGUI("Показать окно timeout \"" + data + "\"\n");
                            authController.showAlertWindow("Ошибка", token[1]);
                            throw new IOException(token[1]);
                        }
                    }
                    //работа
                    while (true){
                        System.out.println("Ждем сообщения от сервера");
                        showInGUI("Ждем сообщения от сервера\n");
                        String msg = inputStreamNet.readUTF();

                        if (msg.startsWith("/")){
                            if (msg.equals("/end")){
                                break;
                            }
                            if (msg.startsWith("/clientlist")){
                                System.out.println("Разбиваем сообщение на слова");
                                String[] token = msg.split("\\s+");
                                chatController.updatedListViewContacts(token);
                            }
                            if (msg.startsWith("/w")){
                                System.out.println("Клиент " + chatController.getNickName() + " получил личное сообщение " + msg);
                                showInGUI("Клиент " + chatController.getNickName() + " получил личное сообщение " + msg + "\n");
                                chatController.getMsg(msg);
                            }
                        } else {
                            System.out.println("Клиент " + chatController.getNickName() + " получил сообщение " + msg);
                            showInGUI("Клиент " + chatController.getNickName() + " получил сообщение " + msg + "\n");
                            chatController.getMsg(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Client " + chatController.getNickName() + " disconnect from server");
                    showInGUI("Client " + chatController.getNickName() + " disconnect from server\n");
                    try {
                        socket.close();
                        Platform.runLater(() -> {
                            authController.setAuthentication(false);
                            chatController.splitPaneMainWindow.getItems().remove(0);
                            chatController.vBoxForFieldChat.setVisible(false);
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

    private void showInGUI(String str) {
        Platform.runLater(() -> {
            authController.txtAreaForConsole.appendText(str);
        });
    }

    public void sendMsg(String msg){
        try {
            outputStreamNet.writeUTF(msg);
            System.out.println("Клиент отправил сообщение: " + msg);
            showInGUI("Клиент отправил сообщение: " + msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryAuth(String log, String pas) {
        if (socket != null || socket.isClosed()){
            System.out.println(socket.toString());
            try {
                System.out.println("Отправляем сереверу логин: " + log + " и пароль: " + pas);
                showInGUI("Отправляем сереверу логин: " + log + " и пароль: " + pas + "\n");
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
