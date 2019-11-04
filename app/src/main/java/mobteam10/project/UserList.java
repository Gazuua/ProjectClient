package mobteam10.project;

import android.widget.TextView;

/**
 * Created by JHG on 2017-10-29.
 */

// TextView로 된 유저 목록을 저장할 객체
public class UserList {

    private TextView nameView;
    private TextView scoreView;

    public UserList( TextView nameView, TextView scoreView)
    {
        this.nameView = nameView;
        this.scoreView = scoreView;
    }

    public TextView getNameView() {
        return nameView;
    }
    public TextView getScoreView() {
        return scoreView;
    }
}
