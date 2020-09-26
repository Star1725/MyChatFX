package server;

public class DatabaseAuthService implements AuthServiсe{
    @Override
    public String getNickNameByLoginAndPassword(String login, String password) {
        return DatabaseHandler.getNickNameByLoginAndPasswordFromDB(login, password);
    }

    @Override
    public boolean registration(String login, String password, String nickName) {
        return DatabaseHandler.registration(login, password, nickName);
    }
}
