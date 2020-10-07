package server;

import javafx.application.Platform;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger= Logger.getLogger(StartServer.class.getName());
    private List<ClientHandler> clients;

    public AuthServiсe getAuthService() {
        return authServiсe;
    }
    private AuthServiсe authServiсe;

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    private ServerSocket serverSocket;

    private Controller controller;

    public Thread getThread() {
        return thread;
    }

    private Thread thread;

    private boolean isCreateDB = false;

    public Server(Controller controller) {

        this.controller = controller;
        clients = new Vector<>();
        getAndShowCountClients(controller, clients.size());
        //authServiсe = new SimpleAuthService();
        authServiсe = new DatabaseAuthService();
        DatabaseHandler.preparedAllStatements();

        thread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(controller.getPort());
                logger.log(Level.INFO,"Server start\n" +
                        "serverSocket: getInetAddress - " + serverSocket.getInetAddress() + "\n" +
                        "              getReuseAddress - " + serverSocket.getReuseAddress() + "\n" +
                        "              getLocalSocketAddress - " + serverSocket.getLocalSocketAddress() + "\n" +
                        "              getLocalPort - " + serverSocket.getLocalPort());
                while (!Thread.currentThread().isInterrupted()){
                    Platform.runLater(() -> controller.circleStartServer.setFill(Color.GREEN));
                    logger.log(Level.INFO,"ждем клиентов!!!\n");
                    Socket socket = serverSocket.accept();
                    logger.log(Level.INFO,"connect client: " + socket.getRemoteSocketAddress() + "\n" +
                            "socket: getInetAddress - " + socket.getInetAddress() + "\n" +
                            "        getReuseAddress - " + socket.getReuseAddress() + "\n" +
                            "        getLocalAddress - " + socket.getLocalAddress() + "\n" +
                            "        getLocalPort - " + socket.getLocalPort() + "\n" +
                            "        getRemoteSocketAddress - " + socket.getRemoteSocketAddress() + "\n" +
                            "        getLocalSocketAddress - " + socket.getLocalSocketAddress() + "\n" +
                            "        getPort - " + socket.getPort() + "\n" +
                            "        getTcpNoDelay - " + socket.getTcpNoDelay());
                new ClientHandler(this, socket, controller);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                controller.circleStartServer.setFill(Color.RED);
            } finally {
                try {
                    serverSocket.close();
                    controller.circleStartServer.setFill(Color.RED);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void broadcastMsg(String msgFromNickName, ClientHandler clientHandler){
        for (ClientHandler client : clients) {
            if (!client.equals(clientHandler)){
                client.sendMsg(String.format("%s %s", clientHandler.getNickName(), msgFromNickName));
            }
        }
    }

    public void sendPrivateMsg(String forNickName, String msg) {
        for (ClientHandler client : clients) {
            if (client.getNickName().equals(forNickName)){
                client.sendMsg(msg);
            }
        }
    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        getAndShowCountClients(this.controller, clients.size());
        broadcastListClients();
    }

    public void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
        getAndShowCountClients(this.controller, clients.size());
        broadcastListClients();
    }

    public boolean isAuthenticated(String log){
        for (ClientHandler client : clients) {
            if (client.getLogin().equals(log)){
                return true;
            }
        }
        return false;
    }

    private void broadcastListClients(){
        StringBuilder sb = new StringBuilder("/clientlist ");
        for (ClientHandler client : clients) {
            sb.append(client.getNickName()).append(" ");
        }
        String msg = sb.toString();
        for (ClientHandler client : clients) {
            client.sendMsg(msg);
        }
    }

    private void getAndShowCountClients(Controller controller, int count) {
        Platform.runLater(() -> controller.labelCountOfClients.setText(String.format(controller.COUNT_CLIENTS + "%d", count)));
    }

    public void broadcastMsgEnd(String msg) {
        for (ClientHandler client : clients) {
            client.sendMsg(msg);
        }
    }

    private String getCurTime() {
        Calendar calendar = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return dateFormat.format(calendar.getTime());
    }
}
