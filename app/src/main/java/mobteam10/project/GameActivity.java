package mobteam10.project;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

// 게임이 이루어지는 게임 액티비티
// View를 다룰 수 있도록 SocketReceiver의 callback interface를 구현함
public class GameActivity extends AppCompatActivity implements SocketReceiver.ViewHandlerCallback {

    private SocketHandler socketHandler;
    private Handler handler;

    private ArrayList<UserList> userList;
    private TextView timerTextView;
    private TextView answerTextView;
    private EditText sendText;
    private Button sendButton;
    private ImageButton redButton;
    private ImageButton greenButton;
    private ImageButton blueButton;
    private ImageButton eraseButton;
    private TextView chatView;
    private ScrollView scrollView;

    private AlertDialog disconDialog;
    private AlertDialog exitDialog;

    private AudioManager audioManager;
    private MediaPlayer m_Sound_BackGround;
    private SoundPool soundPool;
    private int SOUND_START;
    private int SOUND_CORRECT;
    private int SOUND_TIMEWARNING;
    private int SOUND_TIMEOUT;

    private int STREAM_TIMEWARNING;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // 백그라운드 음악 재생
        m_Sound_BackGround = MediaPlayer.create
                (getApplicationContext(), R.raw.background);
        m_Sound_BackGround.setLooping(true);
        m_Sound_BackGround.setVolume(1f, 1f);
        m_Sound_BackGround.start();

        soundPool = new SoundPool(3, AudioManager.STREAM_ALARM, 0);

        SOUND_START = soundPool.load(this, R.raw.start, 0);
        SOUND_CORRECT = soundPool.load(this, R.raw.correct, 1);
        SOUND_TIMEOUT = soundPool.load(this, R.raw.timeout, 2);
        SOUND_TIMEWARNING = soundPool.load(this, R.raw.timewarning, 3);

        STREAM_TIMEWARNING = 0;

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // 화면을 항상 켜놓도록 설정
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 객체들 초기화하기
        handler = new Handler();
        socketHandler = SocketHandler.getClassInstance();

        // SocketReceiver 객체에 Callback instance 전달하기
        socketHandler.getCallbackInstance(this);

       // 뷰의 인스턴스화
        userList = new ArrayList<UserList>();
        userList.add(makeUserList(R.id.userNameText1, R.id.userScoreText1));
        userList.add(makeUserList(R.id.userNameText2, R.id.userScoreText2));
        userList.add(makeUserList(R.id.userNameText3, R.id.userScoreText3));
        userList.add(makeUserList(R.id.userNameText4, R.id.userScoreText4));

        timerTextView = (TextView) findViewById(R.id.timerTextView);
        answerTextView = (TextView) findViewById(R.id.answerTextView);
        sendText = (EditText) findViewById(R.id.chatEditText);
        sendButton = (Button) findViewById(R.id.chatSendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = sendText.getText().toString();
                if( str.equals("") ) return;
                if ( !socketHandler.isDrawer() )
                    socketHandler.sendChatPacket(str);
                sendText.setText("");
            }
        });

        redButton = (ImageButton) findViewById(R.id.redPaint);
        redButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                socketHandler.setNowColor(DrawingView.COLOR_RED);
            }
        });
        greenButton = (ImageButton) findViewById(R.id.greenPaint);
        greenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                socketHandler.setNowColor(DrawingView.COLOR_GREEN);
            }
        });
        blueButton = (ImageButton) findViewById(R.id.bluePaint);
        blueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                socketHandler.setNowColor(DrawingView.COLOR_BLUE);
            }
        });
        eraseButton = (ImageButton) findViewById(R.id.eraseButton);
        eraseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(socketHandler.isDrawer()) socketHandler.getDrawPacketAndPut(null);
                if(!socketHandler.isGaming()) socketHandler.setErase(true);
                socketHandler.sendDrawPacket(null);
            }
        });

        chatView = (TextView) findViewById(R.id.chatView);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        // 서버의 문제로 소켓 연결이 끊어질 경우 띄울 AlertDialog
        disconDialog = new AlertDialog.Builder(this)
                .setCancelable(false).setTitle("접속 끊김")
                .setMessage("서버의 요청 혹은 오류로 인하여 접속이 끊어졌습니다. 게임을 종료합니다.")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // 소켓 핸들러 및 스레드 정리 -> 액티비티 종료 -> 프로세스 종료
                        SocketHandler.getClassInstance().finish();
                        finish();
                        System.exit(0);
                    }
                }).create();

        // 유저가 직접 종료를 요청할 경우 띄울 AlertDialog
        exitDialog = new AlertDialog.Builder(this)
                .setTitle("종료")
                .setMessage("정말로 게임을 종료하시겠습니까?")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // 소켓 핸들러 및 스레드 정리 -> 액티비티 종료 -> 프로세스 종료
                        SocketHandler.getClassInstance().finish();
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton("취소", null)
                .create();
    }



    // 뒤로 버튼을 누르면 종료 여부 묻기
    @Override
    public void onBackPressed()
    {
        exitDialog.show();
    }

    private final String[] strColor= {
            "#FF0000", "#47C83E", "#0000FF", "#5F00FF"
    };

    // 텍스트 뷰에 채팅 메세지를 표시해 주는 메소드
    public void showChatMessage(String msg, int index, int namelen) {
        final String str = msg;
        final int i = index;
        final int len = namelen;

        handler.post(new Runnable() {
            @Override
            public void run() {
                SpannableStringBuilder builder = new SpannableStringBuilder(str + "\n");
                builder.setSpan(new ForegroundColorSpan(Color.parseColor(strColor[i])),
                        0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                chatView.append(builder);
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    public void showToastMessage(String msg)
    {
        final String str = msg;

        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void playSound(int flag)
    {
        // 무음, 진동 모드일 경우 소리가 안 나도록 하는 코드
        if( audioManager.getRingerMode() != 2 ) return;

        switch(flag)
        {
            case 0:
                soundPool.play(SOUND_START, 0.9f, 0.9f, 1, 0, 1);
                break;
            case 1:
                soundPool.play(SOUND_CORRECT, 0.9f, 0.9f, 2, 0, 1);
                break;
            case 2:
                soundPool.play(SOUND_TIMEOUT, 0.9f, 0.9f, 3, 0, 1);
                break;
            case 3:
                if ( STREAM_TIMEWARNING == 0 )
                    STREAM_TIMEWARNING = soundPool.play(SOUND_TIMEWARNING,
                            0.9f, 0.9f, 4, -1, 1);
                break;
        }
    }

    public void stopSound()
    {
        if ( STREAM_TIMEWARNING != 0 )
            soundPool.stop(STREAM_TIMEWARNING);
        STREAM_TIMEWARNING = 0;
    }

    public void setTimer(int milis)
    {
        String str = new String();
        int sec = milis / 1000;
        int min = 0;

        if (sec >= 60) {
            min = sec / 60;
            sec -= min * 60;
        }

        if (min < 10) str += "0";
        str += min + ":";
        if (sec < 10) str += "0";
        str += sec;

        int clr;

        if (min >= 1) clr = getResources().getColor(R.color.colorBlue);
        else if (sec > 20) clr = getResources().getColor(R.color.colorYellow);
        else clr = getResources().getColor(R.color.colorRed);

        final String time = str;
        final int color = clr;

        handler.post(new Runnable() {
            @Override
            public void run() {
                timerTextView.setTextColor(color);
                timerTextView.setText(time);
            }
        });
    }

    public void showAnswerText(final String answer, int flag)
    {
        final String a = answer;
        final int f = flag;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (f == 1 || socketHandler.isDrawer()) answerTextView.setText("정답 : " + a);
                else {
                    answerTextView.setText("정답 : " );
                    for( int i=0; i<a.length(); i++ )
                        answerTextView.append("X");
                }
            }
        });
    }

    public void setScore(int index)
    {
        final int i = index;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if ( i == -1 ) {
                    for(UserList views : userList) {
                        TextView scoreView = views.getScoreView();
                        String val = scoreView.getText().toString();
                        int score = 0;
                        if (!val.equals(""))
                            score = Integer.valueOf(val);
                        Log.d("String", ""+score);
                        if ( views.getNameView().getText().toString() != "" )
                            scoreView.setText("" + (score - 50));
                    }
                }
                else {
                    TextView scoreView = userList.get(i).getScoreView();
                    int score = Integer.valueOf(scoreView.getText().toString());
                    Log.d("String", ""+score);
                    scoreView.setText("" + (score + 100));
                }
            }
        });
    }

    // 유저 리스트 뷰 추가하는 메소드
    public UserList makeUserList( int nameViewID, int scoreViewID)
    {
        TextView username = (TextView) findViewById(nameViewID);
        TextView userscore = (TextView) findViewById(scoreViewID);

        return new UserList(username, userscore);
    }

    // 유저 리스트에 유저를 표시해 주는 메소드
    public void showUserListView(String name, int index)
    {
        final String myname = name;
        final int myindex = index;

        handler.post(new Runnable() {
            @Override
            public void run() {
                UserList presentView = userList.get(myindex);
                // 이름 표시
                presentView.getNameView().setText(myname);
                // 점수 표시
                if ( presentView.getScoreView().getText().equals("") )
                    presentView.getScoreView().setText("0");
            }
        });
    }

    // 유저 리스트에서 유저 정보를 없애는 메소드
    public void deleteUserListView(int index)
    {
        final int myindex = index;

        handler.post(new Runnable() {
            @Override
            public void run() {
                UserList presentView = userList.get(myindex);
                // 이름 표시 해제
                presentView.getNameView().setText("");
                // 점수 표시 해제
                presentView.getScoreView().setText("");
            }
        });
    }

    // 종료 알림 상자를 보여주는 메소드
    public void showExitDialog() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                exitDialog.show();
            }
        });
    }

    // 연결 끊김 알림 상자를 보여주는 메소드
    public void showDisconDialog() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                disconDialog.show();
            }
        });
    }

    // 액티비티가 끝날 때 내부의 모든 객체를 정리하는 메소드
    @Override
    public void finish() {
        super.finish();

        if ( userList != null ) {
            for ( int i=0; i<userList.size(); i++ )
                userList.remove(0);
        }
        if ( sendText != null ) sendText = null;
        if ( sendButton != null ) sendButton = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (m_Sound_BackGround != null)
            m_Sound_BackGround.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (m_Sound_BackGround != null)
            m_Sound_BackGround.pause();
    }

    // 종료를 위한 대화 상자와 핸들러는 finish() 메소드가 모두 끝나고 가장 마지막에 액티비티와 함께 사라짐
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if ( socketHandler != null ) socketHandler = null;

        if ( disconDialog != null ) {
            disconDialog.dismiss();
            disconDialog = null;
        };
        if ( exitDialog != null ) {
            exitDialog.dismiss();
            exitDialog = null;
        };

        if( handler != null )
            handler = null;
    }

}
