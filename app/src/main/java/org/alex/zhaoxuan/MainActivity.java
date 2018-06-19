package org.alex.zhaoxuan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;

import org.alex.zhaoxuan.Activities.LocationMapActivity;

import okhttp3.Call;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btn_test,btn_map;
    EditText ipAddress,port,name;
    TextView tv_status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipAddress = (EditText)findViewById(R.id.ipAddress);
        btn_test = (Button)findViewById(R.id.btn_test);
        btn_map = (Button)findViewById(R.id.btn_map);
        tv_status = (TextView)findViewById(R.id.tv_status);
        port = (EditText)findViewById(R.id.et_port);
        name = (EditText)findViewById(R.id.et_name);
        btn_map.setClickable(false);
        btn_map.setEnabled(false);
        btn_map.setOnClickListener(this);
        btn_test.setOnClickListener(this);

        SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
        ipAddress.setText(sp.getString("ipAddress","211.159.164.135"));
        port.setText(sp.getString("port","8080"));
        name.setText(sp.getString("name", Build.BRAND+" "+Build.MODEL));

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_test:
                tv_status.setText("正在连接服务器...");
                final long start_time = System.currentTimeMillis();
                btn_test.setText("请稍后");
                btn_test.setClickable(false);
                btn_test.setEnabled(false);
                OkGo.get("http://"+ipAddress.getText()+":"+port.getText()+"/ZhaoxuanServer/")     // 请求方式和请求url
                        .tag(this)                       // 请求的 tag, 主要用于取消对应的请求
                        .cacheKey("default_key")            // 设置当前请求的缓存key,建议每个不同功能的请求设置一个
                        .execute(new StringCallback() {
                            @Override
                            public void onSuccess(String s, Call call, Response response) {
                                // s 即为所需要的结果
                                btn_test.setText("重新测试");
                                btn_test.setClickable(true);
                                btn_test.setEnabled(true);
                                Log.i("Alex","字符串是::"+s+"长度是"+s.length());
                                if(!s.contains("ZhaoxuanServer") ){
                                    tv_status.setText("服务器不可用");
                                    return;
                                }
                                long timespend = System.currentTimeMillis() - start_time;
                                tv_status.setText("服务器正常,延时"+timespend+" ms");

                                if(timespend < 5000){
                                    btn_map.setClickable(true);
                                    btn_map.setEnabled(true);
                                }

                            }

                            @Override
                            public void onError(Call call, Response response, Exception e) {
                                super.onError(call, response, e);
                                btn_test.setText("重新测试");
                                tv_status.setText("服务器不可用");
                                btn_test.setClickable(true);
                                btn_test.setEnabled(true);
                            }
                        });
                break;
            case R.id.btn_map:
                SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("name", name.getText().toString());
                editor.putString("port", port.getText().toString());
                editor.putString("ipAddress",ipAddress.getText().toString());
                editor.commit();
                Intent intent = new Intent(MainActivity.this, LocationMapActivity.class);
                intent.putExtra("ip",ipAddress.getText().toString()+":"+port.getText().toString());
                intent.putExtra("name",name.getText().toString());
                startActivity(intent);
                finish();
                break;
        }
    }
}
