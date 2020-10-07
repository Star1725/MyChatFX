package client.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

public class ChatController implements Initializable{

    private String privateMsgFor = "";
    private String sendName;
    private String receivName;
    private String dateMsg;
    private String msgForChat;
    private File history;

    public void setLogin(String login) {
        if (history == null || !this.login.equals(login)) {
            history = new File(String.format("client/%s.txt", login));
            boolean iscreated = false;
            //попытка создадния нового файла локальной истории
            try {
                this.login = login;
                System.out.println("class StartServer - попытка создания файла для пользователя - " + this.login);
                iscreated = history.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (iscreated)
                System.out.println("class StartServer - файл истории создан");
            else
                System.out.println("class StartServer - файл истории уже был создан");
        }
        //загрузка локальной истории
        getLocalHistory();
    }

    private String login;

    private void getLocalHistory() {
        try (BufferedReader reader = new BufferedReader(new FileReader(history.getPath()))){
            String str;
            LinkedList<String> temp = new LinkedList<>();
            while ((str = reader.readLine()) != null){
                if (temp.size() < 5){
                    temp.add(str);
                } else {
                    temp.removeFirst();
                    temp.add(str);
                }
            }
            for (String s : temp) {
                createGUIMessageForChat(false, s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String TITLE = "Флудилка";
    public ListView<String> listContacts;
    public AnchorPane anchPaneChatField;
    public SplitPane splitPaneMainWindow;

    public AnchorPane anchPanelListContacts;

    private static final int FLAG_TIME = 0;
    private static final int FLAG_DATE = 1;


    public void clickListClients(MouseEvent mouseEvent) {
        //различные нажатия на мышь
        //mouseEvent.
        String resiveMsg = listContacts.getSelectionModel().getSelectedItem();
        textFieldForSend.setText("/w " + resiveMsg + " ");
    }

    @FXML
    public ImageView imageViewPut;
    public ImageView imageViewEmoji;
    public ImageView imageViewSend;
    public TextField textFieldForSend;
    public VBox vBoxForFieldChat;

    public String getNickName() {
        return nickName;
    }

    private void setNickName(String nickName) {
        this.nickName = nickName;
    }

    private String nickName;

    public void setReadWriteNetHandler(ReadWriteNetHandler readWriteNetHandler) {
        this.readWriteNetHandler = readWriteNetHandler;
    }

    private ReadWriteNetHandler readWriteNetHandler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //скрытие чата контактов
        splitPaneMainWindow.setVisible(false);
    }

    @FXML
    public void onAction(javafx.event.ActionEvent actionEvent){
        sendAndCreateMsg();
    }
    @FXML
    public void onClickedForSend(MouseEvent mouseEvent) {
        sendAndCreateMsg();
    }

    private void sendAndCreateMsg() {
        String msg = textFieldForSend.getText().trim();
        readWriteNetHandler.sendMsg(String.format("%s %s", getCurTime(FLAG_DATE), msg));
        createGUIMessageForChat(true, msg);
        insertMsgInLocalFile(true, msg);
    }
    //метод для получения сообщения от readWriteNetHandler
    public void getMsg(String msg){
        createGUIMessageForChat(false, msg);
        insertMsgInLocalFile(false, msg);
    }

    private void createGUIMessageForChat(boolean isMyMsg, String msg){
        if (!msg.isEmpty()) {
            //мои сообщения
            if (isMyMsg) {
                System.out.println("class ChatController - мои сообщения для чата");
                if (msg.startsWith("/end")) return;
                //личное для кого-то
                if (msg.startsWith("/w")) {//моё личное сообщение для
                    String[] token = msg.split("\\s", 3);
                    String receiver = token[1];
                    msg = token[2];
                    createdMsgFromMe(receiver, String.format("в %s", getCurTime(FLAG_TIME)), msg);
                    //общее сообщение для чата
                } else {
                    createdMsgFromMe("forAll", String.format("в %s", getCurTime(FLAG_TIME)), msg);
                }
            } //сообщения из локальной истории
            else if (msg.startsWith("/local")){
                System.out.println("class ChatController - мои сообщения из файла локальной истории");
                String[] locToken = msg.split("\\s", 5);
                //мои сообщения
                if (locToken[1].equals("true")){
                    isMyMsg = true;
                    if (locToken[4].startsWith("/w")){
                        String[] strings = locToken[4].split("\\s", 3);
                        createdMsgFromMe(strings[1], String.format("%s в %s", locToken[2], locToken[3]), strings[2]);
                    } else {
                        createdMsgFromMe("forAll", String.format("%s в %s", locToken[2], locToken[3]), locToken[4]);
                    }
                    //сообщения из чата
                } else if (locToken[1].equals("false")){
                    if (locToken[4].startsWith("/w")){
                        String[] strings = locToken[4].split("\\s", 3);
                        createdMsgForMe(strings[1], this.nickName, String.format("%s в %s", locToken[2], locToken[3]), strings[2]);
                    } else {
                        String[] strings = locToken[4].split("\\s", 2);
                        createdMsgForMe(strings[0], "forAll", String.format("%s в %s", locToken[2], locToken[3]), strings[1]);
                    }
                }

            }//сообщение из чата, загруженное из БД
            else if (msg.startsWith("/his")){
                    System.out.println("class ChatController - сообщения, загруженные из истории БД");
                    String[] hisToken = msg.split("\\s", 6);
                    sendName = hisToken[1];
                    receivName = hisToken[2];
                    String date = String.format("%s в %s", hisToken[3], hisToken[4]);
                    msg = hisToken[5];
                    //моё сообщение
                    if (sendName.equals(this.nickName)) {
                        isMyMsg = createdMsgFromMe(receivName, date, msg);
                    }
                    //сообщение из чата
                    else {
                        createdMsgForMe(sendName, receivName, date, msg);
                    }
            //личное сообщение из чата
            } else if (msg.startsWith("/w")){
                    System.out.println("class ChatController - личное сообщение из чата");
                    String[] token = msg.split("\\s", 3);
                    sendName = token[1];
                    msg = token[2];
                    createdMsgForMe(sendName, this.nickName, getCurTime(FLAG_TIME), msg);
            //сообщение из чата
            } else {
                    System.out.println("class ChatController - сообщение из чата");
                    String[] token = msg.split("\\s", 2);
                    System.out.println(token[0] + " " + token[1]);
                    createdMsgForMe(token[0], "null", getCurTime(FLAG_TIME), token[1]);
            }

            Label labelNameAndTime = new Label( String.format("%s %s %s", sendName , dateMsg, privateMsgFor));
            Label labelMes = new Label(msgForChat.trim());
            labelMes.setWrapText(true);
            labelMes.setBackground(new Background(new BackgroundFill(javafx.scene.paint.Color.WHITE, new CornerRadii(10),
                    null)));
            labelMes.setPadding(new Insets(8, 8, 8, 8));
            labelMes.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(10),
                    BorderWidths.DEFAULT)));

            VBox vBoxMsg = new VBox();
            vBoxMsg.getChildren().add(labelNameAndTime);
            vBoxMsg.getChildren().add(labelMes);
            if (isMyMsg){
                vBoxMsg.setAlignment(Pos.TOP_LEFT);
            } else {
                vBoxMsg.setAlignment(Pos.TOP_RIGHT);
            }
            Platform.runLater(() -> {
                vBoxForFieldChat.getChildren().add(vBoxMsg);
                vBoxForFieldChat.setPadding(new Insets(8, 8, 8, 8));
                vBoxForFieldChat.setSpacing(8);
                textFieldForSend.requestFocus();
                textFieldForSend.clear();
            });
        }
    }

    private void insertMsgInLocalFile(boolean isMyMsg, String msg) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(history.getPath(), true))){
            System.out.println("class ChatController - пишем в локальный файл истории");
            if (msg.startsWith("/end")) return;
            writer.write(String.format("%s %b %s %s\n", "/local", isMyMsg, getCurTime(FLAG_DATE), msg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createdMsgForMe(String sendName,String receiver, String date, String msg) {
        this.sendName = sendName;
        dateMsg = date;
        msgForChat = msg;
        privateMsgFor = "";
        if (receiver.equals(this.nickName)){
            privateMsgFor = "(личное)";
        }
    }

    private boolean createdMsgFromMe(String receivName, String date, String msg) {
        sendName = "Вы";
        msgForChat = msg;
        dateMsg = date;
        privateMsgFor = "";
        if (!receivName.equals("forAll")){
            privateMsgFor = "(личное для " + receivName + ")";
        }
        return true;
    }

    private String getCurTime(int flagTimeOrDate) {
        Calendar calendar = Calendar.getInstance();
        DateFormat dateFormat = null;
        if (flagTimeOrDate == FLAG_TIME){
            dateFormat = new SimpleDateFormat("HH:mm");
        } else if (flagTimeOrDate == FLAG_DATE){
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
        assert dateFormat != null;
        return dateFormat.format(calendar.getTime());
    }

    public void setTitle(String nickName){
        setNickName(nickName);
        Platform.runLater(() -> {
            splitPaneMainWindow.setVisible(true);
            //отображение списка контактов
            if (splitPaneMainWindow.getItems().size() == 1){
                splitPaneMainWindow.getItems().add(0, anchPanelListContacts);
            }
            splitPaneMainWindow.setDividerPosition(0, 0.3);
            ((Stage) vBoxForFieldChat.getScene().getWindow()).setTitle(TITLE + " для " + nickName);
        });
    }

    public void updatedListViewContacts(String[] token){
        Platform.runLater(() -> {
            listContacts.getItems().clear();
            for (int i = 1; i < token.length; i++) {
                if (token[i].equals(nickName)){
                    token[i] = String.format("%s (%s)", "Вы",token[i]);
                }
                listContacts.getItems().add(token[i]);
            }
        });
    }
}
