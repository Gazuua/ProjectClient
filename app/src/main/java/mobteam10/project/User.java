package mobteam10.project;

/**
 * Created by byunsangjin on 2017. 10. 2..
 */

// DB와 통신할 때 쓰는 유저 정보 객체
public class User {
    String userID;
    String userPassword;
    String userName;
    String userAge;

    public User(String userID, String userPassword, String userName, String userAge) {
        this.userID = userID;
        this.userPassword = userPassword;
        this.userName = userName;
        this.userAge = userAge;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAge() {
        return userAge;
    }

    public void setUserAge(String userAge) {
        this.userAge = userAge;
    }
}
