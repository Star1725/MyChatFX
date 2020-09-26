package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ClientHandler {
    private static final int TIMEOUT_CLOSE_CONNECT = 5000;
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

    public ClientHandler(Server server, Socket socket, Controller controller) {
        try {
            this.server = server;
            this.socket = socket;
            this.controller = controller;
            inputStreamNet = new DataInputStream(socket.getInputStream());
            outputStreamNet = new DataOutputStream(socket.getOutputStream());
            System.out.println("Start Thread ClientHandler");
            System.out.println(StartServer.getCurTime() + "class ClientHandler - Start Thread ClientHandler");
            Thread threadReadMsgFromNet = new Thread(() -> {
                try {
                    //аутентификация
                    while (true){
                        System.out.println(StartServer.getCurTime() + "class ClientHandler - Цикл аунтетификации");
                        String data = inputStreamNet.readUTF();
                        System.out.println(StartServer.getCurTime() + "class ClientHandler - Сервер получил данные аунтотификации " + data);
                        if (data.startsWith("/auth")){
                            System.out.println(StartServer.getCurTime() + "class ClientHandler - Установка времени timeout");
                            socket.setSoTimeout(TIMEOUT_CLOSE_CONNECT);
                            sendMsg(String.format("%s %s", "/timeout_on", TIMEOUT_CLOSE_CONNECT));
                            String[] token = data.split("\\s");
                            if (token.length < 3){
                                continue;
                            }
                            String dataAuth = server
                                    .getAuthService()
                                    .getNickNameByLoginAndPassword(token[1], token[2]);
                            String newNickName = dataAuth;
                            login = token[1];
                            if (newNickName != null){
                                if (!server.isAuthenticated(login)){
                                    nickName = newNickName;
                                    sendMsg(String.format("%s %s", "/authok", newNickName));
                                    server.subscribe(this);
                                    System.out.println(StartServer.getCurTime() + "class ClientHandler - Клиент " + nickName + " подключился");
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
                            boolean b = server.getAuthService().registration(token[1], token[2], token[3]);
                            if (b){
                                sendMsg("/regok Регистрация успешна");
                            } else {
                                sendMsg("/regno Регистрация не прошла");
                            }
                        }
                        if (data.startsWith("/timeout_off")){
                            System.out.println(StartServer.getCurTime() + "class ClientHandler - сброс времени timeout");
                            socket.setSoTimeout(0);
                        }
                    }
                    //загрузка истории из БД
                    //DatabaseHandler.uploadHistoryForClientHandler(this);
                    //работа
                    while (true){
                        System.out.println(StartServer.getCurTime() + "class ClientHandler - Цикл работы");
                        String msg = inputStreamNet.readUTF();
                        String[] globalToken = msg.split("\\s", 3);
                        String dateGetMsgFromClient = String.format("%s %s", globalToken[0], globalToken[1]);
                        msg = globalToken[2];
                        if (msg.startsWith("/end")){
                            System.out.println(StartServer.getCurTime() + "class ClientHandler - Сервер получил служебное сообщение /end от " + this.getNickName());
                            outputStreamNet.writeUTF(msg);
                            break;
                        }
                        if (msg.startsWith("/w")){
                            System.out.println(StartServer.getCurTime() + "class ClientHandler - Сервер получил служебное сообщение \"" + msg + "\" от " + this.getNickName());
                            String[] token = msg.split("\\s", 3);
                            String forNickName = token[1];
                            msg = String.format("%s %s %s", token[0], this.nickName, token[2]);
                            System.out.println(StartServer.getCurTime() + "class ClientHandler - Сервер получил сообщение для " + forNickName + " от " + this.nickName + ": " + msg + " в " + token[2]);
                            server.sendPrivatMsg(forNickName, msg);
                            //запись сообщения в БД
                            //DatabaseHandler.insertClientsMsgInDB(token[0], this.id, server.getIdForNickname(forNickName), dateGetMsgFromClient, token[2]);
                            continue;
                        }

                        System.out.println(StartServer.getCurTime() + "class ClientHandler - Сервер получил сообщение для всех от " + this.nickName + ": " + msg);
                        //добавляем перед msg nickname, чтобы все знали от кого сообщение
                        server.broadcastMsg(msg, this);
                        //запись сообщения в БД
                        //DatabaseHandler.insertClientsMsgInDB("", this.id, 0, dateGetMsgFromClient, msg);
                    }
                } catch (SocketTimeoutException e){
                    System.out.println(" class ClientHandler - " + e.getMessage());
                    sendMsg("/endtime Истекло время аутентификации на сервере");
                }
                catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println(StartServer.getCurTime() + "class ClientHandler - disconnect client: " + socket.getRemoteSocketAddress());
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
            System.out.println(StartServer.getCurTime() + "class ClientHandler - ClientHandler " + this.getNickName() + " отправил сообщение: \"" + msg + "\"");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
