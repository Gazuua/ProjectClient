package mobteam10.project;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by byunsangjin on 2017. 10. 2..
 */

// 삭제를 진행하는 php 도메인으로 요청을 보내는 클래스
public class DeleteRequest extends StringRequest{

    final static private String URL = "https://sjbyun93.cafe24.com/Delete.php";
    private Map<String, String> parameters;

    public DeleteRequest(String userID, Response.Listener<String> listener){
        super(Request.Method.POST, URL, listener, null);
        parameters = new HashMap<>();
        parameters.put("userID",userID);
    }

    @Override
    public Map <String, String> getParams(){
        return parameters;
    }
}
