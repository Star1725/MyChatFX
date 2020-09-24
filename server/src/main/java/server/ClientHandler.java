package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler {
    private static final int TIMEOUT_CLOSE_CONNECT = 15000;
    private Server server;
    private Socket socket;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    private Controller controller;
    public String getNickName() {
        return nickName;
    }

    public String getLogin() {
        return login;
    }

    private String login;

    private String nickName;
    DataInputStream inputStreamNet;
    DataOutputStream outputStreamNet;

    public ClientHandler(Server server, Socket socket, Controller controller, DatabaseHandler databaseHandler) {
        try {
            this.server = server;
            this.socket = socket;
            this.controller = controller;
            inputStreamNet = new DataInputStream(socket.getInputStream());
            outputStreamNet = new DataOutputStream(socket.getOutputStream());
            System.out.println("Start Thread ClientHandler");
            controller.showInGUI(StartServer.getCurTime() + " - Start Thread ClientHandler\n");
            Thread threadReadMsgFromNet = new Thread(() -> {
                try {
                    //аутентификация
                    while (true){
                        System.out.println("Цикл аунтетификации");
                        controller.showInGUI(StartServer.getCurTime() + " - Цикл аунтетификации\n");
                        String data = inputStreamNet.readUTF();
                        System.out.println("Сервер получил данные аунтотификации " + data);
                        controller.showInGUI(StartServer.getCurTime() + " - Сервер получил данные аунтотификации " + data + "\n");
                        if (data.startsWith("/auth")){
                            System.out.println("Установка времени timeout");
                            controller.showInGUI(StartServer.getCurTime() + " - Установка времени timeout\n");
                            socket.setSoTimeout(TIMEOUT_CLOSE_CONNECT);
                            sendMsg(String.format("%s %s", "/timeout_on", TIMEOUT_CLOSE_CONNECT));
                            String[] token = data.split("\\s");
                            if (token.length < 3){
                                continue;
                            }
//                            String newNickName = server
//                                    .getAuthService()
//                                    .getNickNameByLoginAndPassword(token[1], token[2]);

                            String newNickName = server
                                    .getDatabaseHandler()
                                    .getNickNameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNickName != null){
                                if (!server.isAuthenticated(login)){
                                    nickName = newNickName;
                                    sendMsg(String.format("%s %s", "/authok", newNickName));
                                    server.subscribe(this);
                                    System.out.println("Клиент " + nickName + " подключился");
                                    controller.showInGUI(StartServer.getCurTime() + " - Клиент " + nickName + " подключился\n");
                                    socket.setSoTimeout(0);
                                    break;
                                } else {
                                    sendMsg("/error1 Данная учётная запись уже используется");
                                }
                            } else {
                                sendMsg("/error2 Неверный логин / пароль");
                            }
                        }
                        if (data.startsWith("/reg")){
                            String[] token = data.split("\\s");
                            if (token.length < 4){
                                continue;
                            }
//                            boolean b = server.getAuthService().registration(token[1], token[2], token[3]);
                            boolean b = server.getDatabaseHandler().registration(token[1], token[2], token[3]);
                            if (b){
                                sendMsg("/regok Регистрация успешна");
                            } else {
                                sendMsg("/regno Регистрация не прошла");
                            }
                        }
                        if (data.startsWith("/timeout_off")){
                            System.out.println("сброс времени timeout");
                            controller.showInGUI(StartServer.getCurTime() + " - сброс времени timeout\n");
                            socket.setSoTimeout(0);
                        }
                    }
                    //загрузка истории из БД
                    //databaseHandler.uploadHistoryForClientHandler(this);

                    //работа
                    while (true){
                        System.out.println("Цикл работы");

                        controller.showInGUI(StartServer.getCurTime() + " - Цикл работы\n");
                        String msg = inputStreamNet.readUTF();

                        if (msg.startsWith("/end")){
                            System.out.println("Сервер получил служебное сообщение /end от " + this.getNickName());
                            controller.showInGUI(StartServer.getCurTime() + " - Сервер получил служебное сообщение /end от " + this.getNickName() + "\n");
                            outputStreamNet.writeUTF(msg);
                            break;
                        }
                        if (msg.startsWith("/w")){
                            System.out.println("Сервер получил служебное сообщение \"" + msg + "\" от " + this.getNickName());
                            controller.showInGUI(StartServer.getCurTime() + " - Сервер получил служебное сообщение \"" + msg + "\" от " + this.getNickName() + "\n");
                            String[] token = msg.split("\\s", 3);
                            String forNickName = token[1];
                            String fromNickName = this.nickName;
                            msg = String.format("%s %s %s %s", token[0], forNickName, fromNickName, token[2]);
                            System.out.println("Сервер преобразовал сообщение для " + forNickName + " от " + fromNickName + ": " + msg);
                            controller.showInGUI(StartServer.getCurTime() + " - Сервер получил сообщение для " + forNickName + " от " + fromNickName + ": " + msg + "\n");
                            server.sendPrivatMsg(forNickName, msg);
                            continue;
                        }
                        System.out.println("Сервер получил сообщение для всех от " + nickName + ": " + msg);
                        controller.showInGUI(StartServer.getCurTime() + " - Сервер получил сообщение для всех от " + nickName + ": " + msg + "\n");
                        server.broadcastMsg(String.format("%s %s", nickName, msg), this);//добавляем перед msg nickname, чтобы все знали от кого сообщение
                    }
                } catch (SocketTimeoutException e){
                    System.out.println(e.getMessage());
                    sendMsg("/endtime Истекло время аутентификации на сервере");
                }
                catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("disconnect client: " + socket.getRemoteSocketAddress());
                    controller.showInGUI(StartServer.getCurTime() + " - disconnect client: " + socket.getRemoteSocketAddress() + "\n");
                    server.unsubscribe(this);
                    try {
                        socket.close();
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

    void sendMsg(String msg){
        try {
            msg = msg.trim();
            outputStreamNet.writeUTF(String.format("%s", msg));
            System.out.println("ClientHandler " + this.getNickName() + " отправил сообщение: \"" + msg + "\"");
            controller.showInGUI(StartServer.getCurTime() + " - ClientHandler " + this.getNickName() + " отправил сообщение: \"" + msg + "\"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
