package mobteam10.project;

import java.io.Serializable;

/**
 * Created by JHG on 2017-10-03.
 */

// PacketMessage에 들어갈 유저 정보 객체. 방 내에서의 인덱스와 이름만으로 구성되어 있다.
public class UserData implements Serializable {

    private static final long serialVersionUID = 8501L;

    private int index;
    private String name;

    // default constructor
    public UserData(){
        name = "PID" + android.os.Process.myPid();
        index = -1;
    }

    public UserData(int index, String name)
    {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }
    public String getName() {
        return name;
    }

    public void setIndex(int index) {
        this.index = index;
    }
    public void setName(String name) {
        this.name = name;
    }
}
