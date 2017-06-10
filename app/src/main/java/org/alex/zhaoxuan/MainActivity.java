package org.alex.zhaoxuan;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.smartwebee.android.blespp.BleSppActivity;
import com.smartwebee.android.blespp.DeviceScanActivity;

import org.alex.zhaoxuan.Activities.LocationMapActivity;

import okhttp3.Call;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btn_test,btn_map;
    EditText ipAddress;
    TextView tv_status,tv_btName,tv_btAddress;
    private final static int REQ_CODE_SCAN = 10000;
    boolean internet_valable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipAddress = (EditText)findViewById(R.id.ipAddress);
        btn_test = (Button)findViewById(R.id.btn_test);
        btn_map = (Button)findViewById(R.id.btn_map);
        tv_status = (TextView)findViewById(R.id.tv_status);
        tv_btName = (TextView)findViewById(R.id.tv_btName);
        tv_btAddress = (TextView)findViewById(R.id.tv_btAddress);
        btn_map.setClickable(false);
        btn_map.setEnabled(false);
        btn_map.setOnClickListener(this);
        btn_test.setOnClickListener(this);
        tv_btName.setOnClickListener(this);
        tv_btAddress.setOnClickListener(this);
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
                OkGo.get("http://"+ipAddress.getText()+"/ZhaoxuanServer/")     // 请求方式和请求url
                        .tag(this)                       // 请求的 tag, 主要用于取消对应的请求
                        .cacheKey("default_key")            // 设置当前请求的缓存key,建议每个不同功能的请求设置一个
                        .execute(new StringCallback() {
                            @Override
                            public void onSuccess(String s, Call call, Response response) {
                                // s 即为所需要的结果
                                btn_test.setText("重新测试");
                                btn_test.setClickable(true);
                                btn_test.setEnabled(true);
                                if(s.length() == 0){
                                    tv_status.setText("服务器不可用");
                                    return;
                                }
                                long timespend = System.currentTimeMillis() - start_time;
                                tv_status.setText("服务器正常,延时"+timespend+" ms");
                                internet_valable = timespend < 5000?true:false;
                                if(internet_valable){
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
                Intent intent = new Intent(MainActivity.this, LocationMapActivity.class);
                intent.putExtra("ip",ipAddress.getText().toString());
                if(tv_btName.getText().toString().startsWith("蓝牙")) {
                    intent.putExtra(BleSppActivity.EXTRAS_DEVICE_NAME, tv_btName.getText().toString().substring(5));
                    intent.putExtra(BleSppActivity.EXTRAS_DEVICE_ADDRESS, tv_btAddress.getText().toString().substring(5));
                }
                startActivity(intent);
                finish();
                break;
            case R.id.tv_btName:
            case R.id.tv_btAddress://进入选择蓝牙设备界面
                startActivityForResult(new Intent(this, DeviceScanActivity.class),REQ_CODE_SCAN);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQ_CODE_SCAN:
                if(resultCode != RESULT_OK || data == null ||data.getExtras() == null){
                    Toast.makeText(this,"选择蓝牙设备失败，请重试",Toast.LENGTH_LONG).show();
                    break;
                }
                String deviceName =data.getStringExtra(BleSppActivity.EXTRAS_DEVICE_NAME);
                String deviceAddress =data.getStringExtra(BleSppActivity.EXTRAS_DEVICE_ADDRESS);
                if(TextUtils.isEmpty(deviceName) || TextUtils.isEmpty(deviceAddress)){
                    Toast.makeText(this,"选择蓝牙设备失败222，请重试",Toast.LENGTH_LONG).show();
                    break;
                }
                tv_btName.setText("蓝牙名称："+deviceName);
                tv_btAddress.setText("蓝牙地址："+deviceAddress);
                if(internet_valable){
                    btn_map.setClickable(true);
                    btn_map.setEnabled(true);
                }
                break;
        }
    }
}
