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
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler {
    private static final Logger logger= Logger.getLogger(StartServer.class.getName());
    private static final int TIMEOUT_CLOSE_CONNECT = 15000;
    private Server server;
    private Socket socket;

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
            inputStreamNet = new DataInputStream(socket.getInputStream());
            outputStreamNet = new DataOutputStream(socket.getOutputStream());
            logger.log(Level.INFO,"Start Thread ClientHandler");
            Thread threadReadMsgFromNet = new Thread(() -> {
                try {
                    //аутентификация
                    while (true){
                        String data = inputStreamNet.readUTF();
                        logger.log(Level.INFO,"Сервер получил данные аунтотификации " + data);
                        if (data.startsWith("/auth")){
                            logger.log(Level.INFO,"Установка времени timeout");
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
                                    logger.log(Level.INFO,"Клиент " + nickName + " подключился");
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
                            logger.log(Level.INFO,"сброс времени timeout");
                            socket.setSoTimeout(0);
                        }
                    }

///////////////////////загрузка истории из БД
                    //DatabaseHandler.uploadHistoryForClientHandler(this.nickName, server);


                    //работа
                    while (true){
                        logger.log(Level.INFO,"Цикл работы");
                        String msg = inputStreamNet.readUTF();
                        String[] globalToken = msg.split("\\s", 3);
                        String dateGetMsgFromClient = String.format("%s %s", globalToken[0], globalToken[1]);
                        msg = globalToken[2];
                        if (msg.startsWith("/end")){
                            outputStreamNet.writeUTF(msg);
                            break;
                        }
                        //личное сообщение от кого-то
                        if (msg.startsWith("/w")){
                            String[] token = msg.split("\\s", 3);
                            String forNickName = token[1];
                            msg = String.format("%s %s %s", token[0], this.nickName, token[2]);
                            server.sendPrivateMsg(forNickName, msg);
                            //запись личного сообщения в БД
                            DatabaseHandler.insertClientsMsgInDB("/his", this.nickName, forNickName, dateGetMsgFromClient, token[2]);
                            continue;
                        }
                        //сообщение для всех
                        server.broadcastMsg(msg, this);
                        //запись общего сообщения в БД
                        DatabaseHandler.insertClientsMsgInDB("/his", this.nickName, "null", dateGetMsgFromClient, msg);
                    }
                } catch (SocketTimeoutException e){
                    logger.log(Level.INFO,e.getMessage());
                    sendMsg("/endtime Истекло время аутентификации на сервере");
                }
                catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    logger.log(Level.INFO,"disconnect client: " + socket.getRemoteSocketAddress());
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
            logger.log(Level.INFO,"ClientHandler " + this.getNickName() + " отправил сообщение: \"" + msg + "\"");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
