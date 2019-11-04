package mobteam10.project;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by byunsangjin on 2017. 10. 2..
 */

public class RegisterRequest extends StringRequest{
    final static private String URL = "https://sjbyun93.cafe24.com/Register.php";
    private Map<String, String> parameters;

    public RegisterRequest(String userID, String userPassword, String userName, String userAge, Response.Listener<String> listener) {
        super(Method.POST, URL, listener, null);
        parameters = new HashMap<>();
        parameters.put("userID",userID);
        parameters.put("userPassword",userPassword);
        parameters.put("userName",userName);
        parameters.put("userAge",userAge + "");
    }

    @Override
    public Map<String, String> getParams(){
        return parameters;
    }
}
