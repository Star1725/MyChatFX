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

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ResourceBundle;

public class ChatController implements Initializable{

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

    public ReadWriteNetHandler getReadWriteNetHandler() {
        return readWriteNetHandler;
    }

    public void setReadWriteNetHandler(ReadWriteNetHandler readWriteNetHandler) {
        this.readWriteNetHandler = readWriteNetHandler;
    }

    private ReadWriteNetHandler readWriteNetHandler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //скрытие списка контактов
        splitPaneMainWindow.getItems().remove(0);
        vBoxForFieldChat.setVisible(true);
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
    }
    //метод для получения сообщения от readWriteNetHandler
    public void getMsg(String msg){
        createGUIMessageForChat(false, msg);
    }

    private void createGUIMessageForChat(boolean isMyMsg, String msg){
        if (!msg.isEmpty()) {
            String privateMsgFor = "";
            String sendName;
            //моё сообщение
            if (isMyMsg){
                sendName = "Вы";
                if (msg.startsWith("/w")){//моё личное сообщение для
                    String[] token = msg.split("\\s", 3);
                    msg = token[2];
                    privateMsgFor = "(личное для " + token[1] + ")";
                    System.out.println("createMessage" + privateMsgFor + " - " + msg );
                }
                //личное сообщение из чата
            } else if (msg.startsWith("/w")){
                String[] token = msg.split("\\s", 3);
                msg = token[2];
                System.out.println("private createMessage - " + msg );
                sendName = token[1];
                privateMsgFor = "(личное)";
            } else {//общее сообщение из чата
                String[] token = msg.split("\\s", 2);
                msg = token[1];
                System.out.println("createMessage - " + msg );
                sendName = token[0];
            }

            Label labelNameAndTime = new Label( sendName + " в " + getCurTime(FLAG_TIME) + " " + privateMsgFor);
            Label labelMes = new Label(msg.trim());
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
            ((Stage) vBoxForFieldChat.getScene().getWindow()).setTitle(TITLE + " для " + nickName);
            //отображение списка контактов
            if (splitPaneMainWindow.getItems().size() == 1){
                splitPaneMainWindow.getItems().add(0, anchPanelListContacts);
            }
            splitPaneMainWindow.setDividerPosition(0, 0.3);
        });
    }

    public void updatedListViewContacts(String[] token){
        Platform.runLater(() -> {
            //System.out.println("GUI Thread is start");
            listContacts.getItems().clear();
            //System.out.println("Очистили список");
            for (int i = 1; i < token.length; i++) {
                System.out.println(token[i]);
                if (token[i].equals(nickName)){
                    token[i] = String.format("%s (%s)", "Вы",token[i]);
                }
                listContacts.getItems().add(token[i]);
            }
            System.out.println("Вывели новый список");
        });
    }
}
