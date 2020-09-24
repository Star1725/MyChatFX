package server;

import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthServiсe {

    private class UserData{
        String login;
        String password;
        String nickName;

        public UserData(String login, String password, String nickName) {
            this.login = login;
            this.password = password;
            this.nickName = nickName;
        }
    }

    List<UserData> usersDataList;

    public SimpleAuthService() {
        usersDataList = new ArrayList<>();
        for (int i = 1; i < 11 ; i++) {
            usersDataList.add(new UserData("login" + i, "login" + i, "nick" + i));
        }
        usersDataList.add(new UserData("qwe", "qwe", "qwe"));
        usersDataList.add(new UserData("asd", "asd", "asd"));
        usersDataList.add(new UserData("zxc", "zxc", "zxc"));
    }

    @Override
    public Object[] getNickNameByLoginAndPassword(String login, String password) {
        for (UserData userData : usersDataList) {
            if (userData.login.equals(login) && userData.password.equals(password)){
                return new Object[]{userData.nickName};
            }
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickName) {
        for (UserData userData : usersDataList) {
            if (userData.login.equals(login) || userData.nickName.equals(nickName)){
                return false;
            }
        }
        usersDataList.add(new UserData(login, password, nickName));
        return true;
    }
}
