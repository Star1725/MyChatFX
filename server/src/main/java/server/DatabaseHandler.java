package server;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class DatabaseHandler implements AuthServiсe{

    private static final String CLIENTS_TABLE = "clients";
    private static final String CLIENTS_COLUMN_ID = "id_clients";
    private static final String CLIENTS_COLUMN_LOGIN = "login";
    private static final String CLIENTS_COLUMN_PASSWORD = "password";
    private static final String CLIENTS_COLUMN_NICKNAME = "nickname";

    private static final String MESSAGES_TABLE = "messages";
    private static final String MESSAGES_COLUMN_ID = "id_messages";
    private static final String MESSAGES_COLUMN_FLAG = "flag";
    private static final String MESSAGES_COLUMN_ID_SENDER = "id sender";
    private static final String MESSAGES_COLUMN_ID_RECEIVER = "id receiver";
    private static final String MESSAGES_COLUMN_MSG = "msg";

    private String nickName;

    public Controller getController() {
        return controller;
    }
    private final Controller controller;

    public DatabaseHandler(Controller controller) {
        this.controller = controller;
        createdFileDB(controller);
    }

    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement preparedStatement;

    public static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:myDB.db");
            statement = connection.createStatement();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void createTablesInDB(){
        try {
            statement.execute("CREATE TABLE if not exists " + CLIENTS_TABLE + " ( " +
                    CLIENTS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    CLIENTS_COLUMN_LOGIN + " TEXT, " +
                    CLIENTS_COLUMN_PASSWORD + " TEXT, " +
                    CLIENTS_COLUMN_NICKNAME + " TEXT);");
            statement.execute("CREATE TABLE if not exists " + MESSAGES_TABLE + " (" +
                    MESSAGES_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    MESSAGES_COLUMN_FLAG + " TEXT," +
                    MESSAGES_COLUMN_ID_SENDER + " INTEGER," +
                    MESSAGES_COLUMN_ID_RECEIVER + " INTEGER," +
                    MESSAGES_COLUMN_MSG + " TEXT);");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void insertClientsDAtaInDB(String login, String password, String nickname) throws SQLException {
        preparedStatement = connection.prepareStatement("INSERT INTO clients (login, password, nickname) VALUES (?, ?, ?);");
        preparedStatement.setString(1, login);
        preparedStatement.setString(2, password);
        preparedStatement.setString(3, nickname);
        preparedStatement.executeUpdate();
    }

    public static int checkClientsDataForReg(String login, String nickname) throws SQLException {
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(login), COUNT(nickname) FROM clients WHERE login = '" + login + "' OR nickname = '" + nickname + "';");
        resultSet.next();
        System.out.println("** Колво совпадений при регистрации - " + resultSet.getInt(1) + resultSet.getInt(2));
        return (resultSet.getInt(1) + resultSet.getInt(2));
    }

    public static String checkClientsDataInDBForAuth(String login,String password) throws SQLException {
        ResultSet resultSet = statement.executeQuery("SELECT nickname FROM clients WHERE login = '" + login + "' AND password = '" + password + "';");
        resultSet.next();
        if(resultSet.isClosed()) return null;
        else {
            System.out.println("** Nickname полученный при аунтотификации - " + resultSet.getString(1));
            return resultSet.getString(1);
        }
    }

    public static void disconnect(){
        try {
            if (statement != null){statement.close();}
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            if (preparedStatement != null){preparedStatement.close();}
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void createdFileDB(Controller controller) {
        File newFile = new File("myDB.db");
        boolean created = false;
        // создадим новый файл
        try {
            created = newFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(created){
            logInConsoleAndGUI(controller, "** файл базы данных создан");
            connect();
            logInConsoleAndGUI(controller, "** connect with myDB");
            createTablesInDB();
            logInConsoleAndGUI(controller, "** created table \"clients\" in myDB");
            disconnect();
            logInConsoleAndGUI(controller, "** disconnect with myDB");
        } else {
            logInConsoleAndGUI(controller, "** файл myDB уже был создан");
        }
    }

    @Override
    public synchronized String getNickNameByLoginAndPassword(String login, String password){
        connect();
        logInConsoleAndGUI(controller, "** connect with myDB");
        try {
            nickName = checkClientsDataInDBForAuth(login, password);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        disconnect();
        logInConsoleAndGUI(controller, "** disconnect with myDB");
        return nickName;
    }

    @Override
    public synchronized boolean registration(String login, String password, String nickName){
        connect();
        logInConsoleAndGUI(controller, "** connect with myDB");
        try {
            if (checkClientsDataForReg(login, nickName) != 0){
                disconnect();
                logInConsoleAndGUI(controller, "** disconnect with myDB");
                return false;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            insertClientsDAtaInDB(login, password, nickName);
            disconnect();
            logInConsoleAndGUI(controller, "** disconnect with myDB");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return true;
    }

    public synchronized void uploadHistoryForClientHandler(ClientHandler clientHandler){
        ResultSet resultSet = null;
        try {
            resultSet = statement.executeQuery("SELECT * FROM " + MESSAGES_TABLE +
                    " INNER JON " + CLIENTS_TABLE +
                    " ON " + MESSAGES_TABLE + "." + MESSAGES_COLUMN_ID_SENDER + " = " + CLIENTS_TABLE + "." + CLIENTS_COLUMN_ID +
                    " INNER JON " + CLIENTS_TABLE +
                    " ON " + MESSAGES_TABLE + "." + MESSAGES_COLUMN_ID_RECEIVER + " = " + CLIENTS_TABLE + "." + CLIENTS_COLUMN_ID +
                    " WHERE " + CLIENTS_COLUMN_NICKNAME + " = " + clientHandler.getNickName() + ";");
            while (resultSet.next()){

            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    private void logInConsoleAndGUI(Controller controller, String info) {
        System.out.println(info);
        controller.showInGUI(info + "\n");
    }
}


