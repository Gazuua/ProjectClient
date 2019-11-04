package mobteam10.project;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// 로그인 화면을 다루는 액티비티
public class LoginActivity extends AppCompatActivity {

    // 백그라운드에서 AsyncTask를 이용해 유저 리스트를 빠르게 업데이트하는 이너 클래스
    class BackgroundTask extends AsyncTask<Void, Void, String>
    {
        String target;

        // 생성자와 같은 개념으로 접속할 사이트 주소를 String으로 받아둠
        @Override
        protected void onPreExecute(){
            target = "https://sjbyun93.cafe24.com/List.php";
        }

        // 해당 Task가 백그라운드에서 무엇을 할 지 정의하는 메소드
        @Override
        protected String doInBackground(Void... voids){
            try{
                URL url = new URL(target);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String temp;
                StringBuilder stringBuilder = new StringBuilder();
                while((temp = bufferedReader.readLine())!=null){
                    stringBuilder.append(temp+"\n");
                }
                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();
                return stringBuilder.toString().trim(); // 리스트 데이터를 전부 읽어서 String으로 반환한다

            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void onProgressUpdate(Void... values){
            super.onProgressUpdate(values);
        }

        @Override
        public void onPostExecute(String result){
            Intent intent = new Intent(LoginActivity.this,ManagementActivity.class);
            intent.putExtra("userList",result);
            LoginActivity.this.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText idText = (EditText) findViewById(R.id.idText);
        final EditText passwordText = (EditText) findViewById(R.id.passwordText);
        final Button loginButton = (Button) findViewById(R.id.loginButton);
        final TextView registerButton = (TextView) findViewById(R.id.registerButton);

        // 회원가입 버튼을 누르면 회원가입 액티비티로..
        registerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
                LoginActivity.this.startActivity(registerIntent);
            }
        });

        // 로그인 버튼을 누르면 맞는 동작을 수행
        loginButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                final String userID = idText.getText().toString();
                final String userPassword = passwordText.getText().toString();
                if ( userID.length() < 1 || userPassword.length() < 1 ) return; // 공백 처리
                if ( userID.equals("admin") && userPassword.equals("admin") )
                {
                    new BackgroundTask().execute();
                    return;
                }

                // DB를 php파일로 액세스하여 결과값을 받아오는 리스너 정의
                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);

                            // 로그인 성공 시 닉네임을 넘겨주고 메인 액티비티로
                            if(success)
                            {
                                String userName = jsonResponse.getString("userName");
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("userName", userName);
                                LoginActivity.this.startActivity(intent);
                                finish();
                            }
                            // 로그인 실패 시 알림만 띄워줌
                            else
                            {
                                builder.setMessage("로그인에 실패하였습니다.")
                                        .setNegativeButton("다시시도", null)
                                        .create()
                                        .show();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                };

                // 요청을 생성하여 큐에 추가함으로써 처리해준다
                LoginRequest loginRequest = new LoginRequest(userID,userPassword,responseListener);
                RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
                queue.add(loginRequest);
            }
        });

    }
}
