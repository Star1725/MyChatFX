package server;

import java.sql.SQLException;

public interface AuthServiсe {
    /*
    * @return nickname если user есть
    * @return null если нет
    * */
    String getNickNameByLoginAndPassword(String login, String password);

    boolean registration(String login, String password, String nickName);
}
