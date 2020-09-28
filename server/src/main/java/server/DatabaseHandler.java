package server;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class DatabaseHandler {

    private static final String CLIENTS_TABLE = "clients";
    private static final String CLIENTS_COLUMN_ID = "id_clients";
    private static final String CLIENTS_COLUMN_LOGIN = "login";
    private static final String CLIENTS_COLUMN_PASSWORD = "password";
    private static final String CLIENTS_COLUMN_NICKNAME = "nickname";

    private static final String MESSAGES_TABLE = "messages";
    private static final String MESSAGES_COLUMN_ID = "id_messages";
    private static final String MESSAGES_COLUMN_FLAG = "flag";
    private static final String MESSAGES_COLUMN_ID_SENDER = "id_sender";
    private static final String MESSAGES_COLUMN_ID_RECEIVER = "id_receiver";
    private static final String MESSAGES_COLUMN_DATE_RECEIPT = "date_of_receipt";
    private static final String MESSAGES_COLUMN_MSG = "msg";

    private static PreparedStatement psGetNickName;
    private static PreparedStatement psReg;
    private static PreparedStatement psChangeNickName;
    private static PreparedStatement psInsertClientMsg;
    private static PreparedStatement psUploadMsgForClient;

    private static Connection connection;
    private static Statement statement;

    public static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:myDB.db");
            System.out.println("class DatabaseHandler - connect with myDB");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void createTablesInDB(){
        connect();
        try {
            statement = connection.createStatement();
            statement.execute("CREATE TABLE if not exists " + CLIENTS_TABLE + " ( " +
                    CLIENTS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    CLIENTS_COLUMN_LOGIN + " TEXT UNIQUE, " +
                    CLIENTS_COLUMN_PASSWORD + " TEXT, " +
                    CLIENTS_COLUMN_NICKNAME + " TEXT UNIQUE);");
            statement.execute("CREATE TABLE if not exists " + MESSAGES_TABLE + " (" +
                    MESSAGES_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    MESSAGES_COLUMN_FLAG + " TEXT," +
                    MESSAGES_COLUMN_ID_SENDER + " INTEGER," +
                    MESSAGES_COLUMN_ID_RECEIVER + " INTEGER," +
                    MESSAGES_COLUMN_DATE_RECEIPT + " TEXT," +
                    MESSAGES_COLUMN_MSG + " TEXT);");
            System.out.println("class DatabaseHandler - created tables \"clients\" and \"messages\" in myDB");
            disconnect();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void preparedAllStatements(){
        connect();
        try {
            psReg = connection.prepareStatement("INSERT INTO " + CLIENTS_TABLE +
                    "(" + CLIENTS_COLUMN_LOGIN +
                    ", " + CLIENTS_COLUMN_PASSWORD +
                    ", " + CLIENTS_COLUMN_NICKNAME + ")" +
                    " VALUES (?, ?, ?);");
            psGetNickName = connection.prepareStatement("SELECT " + CLIENTS_COLUMN_NICKNAME + " FROM "+ CLIENTS_TABLE +
                    " WHERE "+ CLIENTS_COLUMN_LOGIN +" = ? AND "+ CLIENTS_COLUMN_PASSWORD +" = ?;");

            psInsertClientMsg = connection.prepareStatement("INSERT INTO " + MESSAGES_TABLE + " (" +
                    MESSAGES_COLUMN_FLAG + " , " +
                    MESSAGES_COLUMN_ID_SENDER + " , " +
                    MESSAGES_COLUMN_ID_RECEIVER + " , " +
                    MESSAGES_COLUMN_DATE_RECEIPT + " , " +
                    MESSAGES_COLUMN_MSG +
                    ") VALUES (?, " +
                    "( SELECT " + CLIENTS_COLUMN_ID + " FROM " + CLIENTS_TABLE + " WHERE " + CLIENTS_COLUMN_NICKNAME + " = ?), " +
                    "( SELECT " + CLIENTS_COLUMN_ID + " FROM " + CLIENTS_TABLE + " WHERE " + CLIENTS_COLUMN_NICKNAME + " = ?), " +
                    " ?, ?);");

            psUploadMsgForClient = connection.prepareStatement("SELECT\n" +
                    "flag,\n" +
                    "(SELECT nickname FROM clients WHERE id_clients = id_sender) as sender,\n" +
                    "(SELECT nickname FROM clients WHERE id_clients = id_receiver) as receiver,\n" +
                    "date_of_receipt,\n" +
                    "msg\n" +
                    "FROM messages\n" +
                    "WHERE id_sender = (SELECT id_clients FROM clients WHERE nickname = ?)\n" +
                    "OR id_receiver = (SELECT id_clients FROM clients WHERE nickname = ?)\n" +
                    "OR id_receiver = (SELECT id_clients FROM clients WHERE nickname = 'null');");

//            psUploadMsgForClient = connection.prepareStatement("SELECT " +
////                    MESSAGES_COLUMN_FLAG + ", " +
//                    "(SELECT " + CLIENTS_COLUMN_NICKNAME + " FROM " + CLIENTS_TABLE + " WHERE " + CLIENTS_COLUMN_ID + " = " + MESSAGES_COLUMN_ID_SENDER + " ), " +
//                    "(SELECT " + CLIENTS_COLUMN_NICKNAME + " FROM " + CLIENTS_TABLE + " WHERE " + CLIENTS_COLUMN_ID + " = " + MESSAGES_COLUMN_ID_RECEIVER + " ), " +
//                    MESSAGES_COLUMN_DATE_RECEIPT + ", " +
//                    MESSAGES_COLUMN_MSG +
//                    " FROM " + MESSAGES_TABLE +
//                    " WHERE " + MESSAGES_COLUMN_ID_SENDER + " = ( SELECT " + CLIENTS_COLUMN_ID + " FROM " + CLIENTS_TABLE + " WHERE " + CLIENTS_COLUMN_NICKNAME + " = ?) " +
//                    " OR " + MESSAGES_COLUMN_ID_RECEIVER + " = ( SELECT " + CLIENTS_COLUMN_ID + " FROM " + CLIENTS_TABLE + " WHERE " + CLIENTS_COLUMN_NICKNAME + " = ?) " +
//                    " OR " + MESSAGES_COLUMN_ID_RECEIVER + " = ( SELECT " + CLIENTS_COLUMN_ID + " FROM " + CLIENTS_TABLE + " WHERE " + CLIENTS_COLUMN_NICKNAME + " = 'null');");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static synchronized boolean registration(String login, String password, String nickName){
        try {
            psReg.setString(1, login);
            psReg.setString(2, password);
            psReg.setString(3, nickName);
            psReg.executeUpdate();
            return true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
    }

    public static String getNickNameByLoginAndPasswordFromDB(String login, String password){
        String nickname = null;
        try {
            psGetNickName.setString(1, login);
            psGetNickName.setString(2, password);
            try (ResultSet resultSet = psGetNickName.executeQuery();){
                while (resultSet.next()){
                    if(resultSet.isClosed()) return null;
                    else {
                        nickname = resultSet.getString(1);
                        System.out.println("class DatabaseHandler - полученны при аунтотификации следующие данные из БД - " + nickname);
                    }
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return nickname;
    }

    public synchronized static void insertClientsMsgInDB(String flag, String sender, String receiver, String date, String msg){
        try {
            System.out.println("class DatabaseHandler - добавляем в БД - флаг:" + flag + " sender:" + sender + " receiver:" + receiver + " date:" + date + " msg:" + msg);
            psInsertClientMsg.setString(1, flag);
            psInsertClientMsg.setString(2, sender);
            psInsertClientMsg.setString(3, receiver);
            psInsertClientMsg.setString(4, date);
            psInsertClientMsg.setString(5, msg);
            psInsertClientMsg.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void disconnect(){
        try {
            if (statement != null){statement.close();}
            if (psReg != null){psReg.close();}
            if (psGetNickName != null){psGetNickName.close();}
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            connection.close();
            System.out.println("class DatabaseHandler - disconnect with myDB");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static synchronized void uploadHistoryForClientHandler(String nickname, Server server){
        try {
            psUploadMsgForClient.setString(1, nickname);
            psUploadMsgForClient.setString(2, nickname);
            System.out.println("class DatabaseHandler - читаем историю сообщений для - " + nickname);
            try (ResultSet resultSet = psUploadMsgForClient.executeQuery()){
                while (resultSet.next()){
                    String flag = resultSet.getString(1);
                    String sender = resultSet.getString(2);
                    String receiver = resultSet.getString(3);
                    String date = resultSet.getString(4);
                    String msg = resultSet.getString(5);
                    server.sendPrivateMsg(nickname, String.format("%s %s %s %s %s", flag, sender, receiver, date, msg));
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}


