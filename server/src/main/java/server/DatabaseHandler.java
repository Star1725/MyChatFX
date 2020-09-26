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
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void preparedAllStatements(){
        connect();
        try {
            psReg = connection.prepareStatement("INSERT INTO " + CLIENTS_TABLE +
                    "(" + CLIENTS_COLUMN_LOGIN + ", " + CLIENTS_COLUMN_PASSWORD + ", " + CLIENTS_COLUMN_NICKNAME + ")" +
                    " VALUES (?, ?, ?);");
            psGetNickName = connection.prepareStatement("SELECT " + CLIENTS_COLUMN_NICKNAME + " FROM "+ CLIENTS_TABLE +
                    " WHERE "+ CLIENTS_COLUMN_LOGIN +" = ? AND "+ CLIENTS_COLUMN_PASSWORD +" = ?;");
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


    public synchronized static void insertClientsMsgInDB(String flag, int idSender, int idReceiver, String date, String msg){
        connect();
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO " + MESSAGES_TABLE + " (" +
                MESSAGES_COLUMN_FLAG + " , " +
                MESSAGES_COLUMN_ID_SENDER + " , " +
                MESSAGES_COLUMN_ID_RECEIVER + " , " +
                MESSAGES_COLUMN_DATE_RECEIPT + " , " +
                MESSAGES_COLUMN_MSG +
                ") VALUES (?, ?, ?, ?, ?);");){
            ps.setString(1, flag);
            ps.setInt(2, idSender);
            ps.setInt(3, idReceiver);
            ps.setString(4, date);
            ps.setString(5, msg);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        disconnect();
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


    public static synchronized void uploadHistoryForClientHandler(ClientHandler clientHandler){
        connect();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + MESSAGES_TABLE +
                " INNER JON " + CLIENTS_TABLE +
                " ON " + MESSAGES_TABLE + "." + MESSAGES_COLUMN_ID_SENDER + " AS Sender " + " = " + CLIENTS_TABLE + "." + CLIENTS_COLUMN_ID +
                " OR " + MESSAGES_TABLE + "." + MESSAGES_COLUMN_ID_RECEIVER + " AS Receiver " + " = " + CLIENTS_TABLE + "." + CLIENTS_COLUMN_ID +
                " WHERE " + MESSAGES_COLUMN_ID_SENDER + " = ? " +
                " OR " + MESSAGES_COLUMN_ID_RECEIVER + " = ? " +
                " OR " + MESSAGES_COLUMN_ID_RECEIVER + " = ?;")){
//            ps.setInt(1, id);
//            ps.setInt(1, id);
            ps.setInt(1, 0);
            try (ResultSet resultSet = ps.executeQuery()){
                while (resultSet.next()){
                    System.out.print(resultSet.getInt(1) + " ");
                    System.out.print(resultSet.getString(2) + " ");
                    System.out.print(resultSet.getInt(3) + " ");
                    System.out.print(resultSet.getInt(4) + " ");
                    System.out.print(resultSet.getString(5) + " ");
                    System.out.print(resultSet.getString(6) + " ");
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            disconnect();
        }
    }
}


