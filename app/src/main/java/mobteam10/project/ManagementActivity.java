package mobteam10.project;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// admin 계정으로 로그인 했을 경우 DB에 있는 유저들의 정보를 열람 및 삭제할 수 있는 액티비티
public class ManagementActivity extends AppCompatActivity {

    private ListView listView;
    private UserListAdapter adapter;
    private List<User> userList;
    private List<User> saveList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_management);
        Intent intent = getIntent();

        listView = (ListView) findViewById(R.id.listView);
        userList = new ArrayList<User>();
        saveList = new ArrayList<User>();
        adapter = new UserListAdapter(getApplicationContext(),userList, this, saveList);
        listView.setAdapter(adapter);

        try{
            JSONObject jsonObject = new JSONObject(intent.getStringExtra("userList"));
            JSONArray jsonAraay = jsonObject.getJSONArray("response");
            int count =0;
            String userID, userPassword, userName, userAge;
            while(count < jsonAraay.length())
            {
                JSONObject object = jsonAraay.getJSONObject(count);
                userID = object.getString("userID");
                userPassword= object.getString("userPassword");
                userName = object.getString("userName");
                userAge = object.getString("userAge");
                User user = new User(userID, userPassword, userName, userAge);
                if(!userID.equals("admin")) {
                    userList.add(user);
                    saveList.add(user);
                }
                count++;

            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        final EditText search = (EditText)findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                searchUser(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }

    public void searchUser(String search){
        userList.clear();
        for(int i=0; i< saveList.size(); i++){
            if(saveList.get(i).getUserID().contains(search)){
                userList.add(saveList.get(i));
            }
        }
        adapter.notifyDataSetChanged();
    }
}
