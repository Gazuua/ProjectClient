package mobteam10.project;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

// 로그인 액티비티에서 넘어오면 소켓 서버로 접속을 하기 위한 액티비티
public class MainActivity extends AppCompatActivity {

    private SocketHandler socketHandler;    // 소켓 핸들러
    private Thread socketConnectThread;    // 소켓 작업을 할 스레드

    private ProgressDialog progressDialog; // 접속 중임을 알리는 ProgressDialog
    private Handler handler; // 소켓 상태 변화에 따라 UI 상태를 변경하기 위한 핸들러
    private AlertDialog failDialog;     // 서버 접속 실패 시 띄울 AlertDialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 핸들러 객체 인스턴스 할당
        handler = new Handler();

        // 소켓 핸들러 싱글톤 객체 받아오기
        socketHandler = SocketHandler.getClassInstance();

        // ProgressDialog 설정
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("서버에 접속 중입니다.\n잠시만 기다려 주세요.");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 소켓 서버 접속 실패 시 띄울 AlertDialog
        failDialog = new AlertDialog.Builder(this)
                .setTitle("접속 실패")
                .setMessage("서버 접속에 실패하였습니다.\r\n게임을 다시 시작해주세요.")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        socketHandler.finish();
                        finish();
                        System.exit(0);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .create();


        // 소켓 스레드로 할 작업 설정 및 시작
        socketConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean isConnected = socketHandler.connect();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if ( isConnected == true ) { // 접속 성공 시 바로 게임액티비티로 넘어가게 함
                            // 유저 데이터 등록
                            String name = getIntent().getStringExtra("userName");
                            socketHandler.getUserData().setName(name);

                            Intent intent = new Intent(getApplicationContext(), GameActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            // GameActivity 실행 후엔 해당 액티비티만 남아있도록 플래그를 준다
                            startActivity(intent);
                            progressDialog.dismiss();
                            progressDialog = null;
                            finish();
                        }
                        else failDialog.show();
                    }
                });
            }
        });
        socketConnectThread.start();
    }

    // 액티비티가 끝날 때 내부의 모든 객체를 정리하는 메소드
    @Override
    public void finish() {
        super.finish();

        if( socketConnectThread != null ) {
            if( socketConnectThread.isAlive() ) socketConnectThread.interrupt();
            socketConnectThread = null;
        }

        if( socketHandler != null )
            socketHandler = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( failDialog != null ) {
            failDialog.dismiss();
            failDialog = null;
        }

        if( handler != null ) handler = null;
    }
}
