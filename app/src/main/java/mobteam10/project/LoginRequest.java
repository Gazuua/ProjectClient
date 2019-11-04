package mobteam10.project;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by byunsangjin on 2017. 10. 2..
 */

// 로그인 작업을 하는 php 도메인에 요청을 보내는 클래스
public class LoginRequest extends StringRequest{
    final static private String URL = "https://sjbyun93.cafe24.com/Login.php";
    private Map<String, String> parameters;

    public LoginRequest(String userID, String userPassword, Response.Listener<String> listener) {
        super(Method.POST, URL, listener, null);
        parameters = new HashMap<>();
        parameters.put("userID",userID);
        parameters.put("userPassword",userPassword);
        Log.d("LOGIN", userID + " / " + userPassword);
    }

    @Override
    public Map<String, String> getParams(){
        return parameters;
    }
}
