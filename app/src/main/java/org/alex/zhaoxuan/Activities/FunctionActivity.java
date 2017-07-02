package org.alex.zhaoxuan.Activities;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.alex.zhaoxuan.BluetoothTools;
import org.alex.zhaoxuan.Fragments.RadarMapFragment;
import org.alex.zhaoxuan.LocationUtils;
import org.alex.zhaoxuan.RadarTarget;
import org.alex.zhaoxuan.R;

import com.ipcamera.demo.AddCameraFragment;
import com.ipcamera.demo.PlayFragment;
import com.smartwebee.android.blespp.BleSppActivity;



public class FunctionActivity extends AppCompatActivity implements RadarMapFragment.OnFragmentInteractionListener,View.OnClickListener,AddCameraFragment.OnFragmentInteractionListener,PlayFragment.OnFragmentInteractionListener{
    //下面这几项是要和MapFragment同步的
    private String ipAddress;//服务器IP地址
    public RadarTarget myPosition = new RadarTarget();//我的位置
    String indoor_lat;
    String indoor_lng;

    private FragmentManager mFragmentManager;//fragment管理者
    private Button bt_map,bt_video,bt_settings;

    private String Buffer = "";
    private int index = 0;
    private static String FrameHead = "AA";
    private static String FrameTail = "BB";

    FrameLayout fragmentsHolder;
    private RadarMapFragment mapFragment;
    private AddCameraFragment cameraFragment;
    private PlayFragment playFragment;


    //==========判断是否已经接通了蓝牙=================
    public boolean mConnected = false;
    BluetoothTools bluetoothTools;
    View bt_status;
    public RadarTarget t;//即将用于上传的目标
    Menu menu;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(com.smartwebee.android.blespp.R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(com.smartwebee.android.blespp.R.id.menu_connect).setVisible(false);
            menu.findItem(com.smartwebee.android.blespp.R.id.menu_disconnect).setVisible(true);
            menu.findItem(com.smartwebee.android.blespp.R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(com.smartwebee.android.blespp.R.id.menu_connect).setVisible(true);
            menu.findItem(com.smartwebee.android.blespp.R.id.menu_disconnect).setVisible(false);
            menu.findItem(com.smartwebee.android.blespp.R.id.menu_refresh).setActionView(
                    com.smartwebee.android.blespp.R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    //这段代码应该被使用？？因为他有连接蓝牙的必要过程
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                if(bluetoothTools != null)bluetoothTools.mBluetoothLeService.connect(bluetoothTools.mDeviceAddress);
                if(findViewById(R.id.bt_showBT).getVisibility() == View.GONE){
                    Toast.makeText(this,"没有配对",Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(this,"正在连接雷达",Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.menu_disconnect:
                if(bluetoothTools != null)bluetoothTools.mBluetoothLeService.disconnect();
                Toast.makeText(this,"断开雷达连接",Toast.LENGTH_LONG).show();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            finish();
            return false;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function);
        ipAddress = getIntent().getStringExtra("ip");
        indoor_lat = getIntent().getStringExtra("indoor_lat");
        indoor_lng = getIntent().getStringExtra("indoor_lng");
        bt_map = (Button)findViewById(R.id.bt_map);
        bt_video = (Button)findViewById(R.id.bt_video);
        bt_settings = (Button)findViewById(R.id.bt_settings);
        bt_map.setOnClickListener(this);
        bt_video.setOnClickListener(this);
        bt_settings.setOnClickListener(this);

        fragmentsHolder = (FrameLayout) findViewById(R.id.fragmentsHolder);

        mFragmentManager = getSupportFragmentManager();//获取到fragment的管理对象
        FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
        mapFragment = RadarMapFragment.newInstance("","",ipAddress,indoor_lat,indoor_lng);
        mFragmentTransaction.add(R.id.fragmentsHolder,mapFragment).commit();

        //设置蓝牙相关
        final Intent intent = getIntent();
        String mDeviceName = intent.getStringExtra(BleSppActivity.EXTRAS_DEVICE_NAME);
        String mDeviceAddress = intent.getStringExtra(BleSppActivity.EXTRAS_DEVICE_ADDRESS);
        Log.i("Alex","收到的address是"+mDeviceAddress);
        if(mDeviceAddress == null){
            findViewById(R.id.bt_status).setVisibility(View.GONE);
            findViewById(R.id.bt_showBT).setVisibility(View.GONE);
            getSupportActionBar().hide();
            return;
        }
        getSupportActionBar().setTitle(mDeviceName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bluetoothTools = new BluetoothTools(
                this,
                mDeviceAddress,
                (TextView) findViewById(com.smartwebee.android.blespp.R.id.data_read_text),
                (TextView) findViewById(com.smartwebee.android.blespp.R.id.byte_received_text),
                (TextView) findViewById(com.smartwebee.android.blespp.R.id.data_received_format),
                (EditText) findViewById(com.smartwebee.android.blespp.R.id.data_edit_box),
                (TextView) findViewById(com.smartwebee.android.blespp.R.id.byte_send_text),
                (TextView) findViewById(com.smartwebee.android.blespp.R.id.data_sended_format),
                (TextView) findViewById(com.smartwebee.android.blespp.R.id.notify_speed_text),
                (Button) findViewById(com.smartwebee.android.blespp.R.id.send_data_btn),
                (Button) findViewById(com.smartwebee.android.blespp.R.id.clean_data_btn),
                (Button) findViewById(com.smartwebee.android.blespp.R.id.clean_text_btn),
                new BluetoothTools.BTReceiver() {
                    @Override
                    public void onReceive(String s) {
                        Buffer += s;
                        String standardFrame = "";
                        try {
                            if (Buffer.indexOf(FrameTail) == -1) {
                                return;
                            } else if (Buffer.indexOf(FrameHead) > Buffer.indexOf(FrameTail) || Buffer.indexOf(FrameTail) - Buffer.indexOf(FrameHead) > 28) {
                                index = Buffer.indexOf(FrameHead) + FrameHead.length();
                                if (index < Buffer.length() && index != -1) {
                                    Buffer = Buffer.substring(index);
                                }
                            } else {
                                standardFrame = Buffer.substring(Buffer.indexOf(FrameHead), Buffer.indexOf(FrameTail) + FrameTail.length());
                                Buffer = Buffer.substring(Buffer.indexOf(FrameTail) + FrameTail.length());
                            }
                            if (TextUtils.isEmpty(standardFrame) || !standardFrame.startsWith(FrameHead) || !standardFrame.endsWith(FrameTail)) {
                                //Toast.makeText(LocationMapActivity.this, "数据帧格式错误", Toast.LENGTH_LONG).show();
                                standardFrame = null;
                                return;
                            }
                            Log.i("Alex", Buffer);
                            Log.i("Alex", "掐头去尾后的值" + standardFrame);
                            standardFrame = standardFrame.substring(FrameHead.length(), standardFrame.length()-FrameTail.length());
                        } catch (Exception e) {
                            Log.i("Error", e.getMessage());
                        }
                        String[] results = standardFrame.split(",");

                        if (results.length < 3) {
                            Toast.makeText(FunctionActivity.this, "数据帧数据错误", Toast.LENGTH_LONG).show();
                            return;
                        }
                        try {
                            double angle = new Double(results[0]);
                            double distance = new Double(results[1]);
                            float speed = new Float(results[2]);

                            double[] latlng = LocationUtils.getGPSLocation(myPosition.latitude, myPosition.longitude, distance, angle);
                            t = new RadarTarget();
                            t.latitude = latlng[0];
                            t.longitude = latlng[1];
                            t.speed = speed / 3.6f;
                            t.targetId = 778899;
                            t.targetName = "目标位置";
                            t.time = System.currentTimeMillis();
                            Log.i("Alex", "雷达传来的目标是" + t);

                        } catch (Exception e) {
                            Toast.makeText(FunctionActivity.this, "数据帧乱码", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        bt_status = findViewById(R.id.bt_status);
        bt_status.setVisibility(View.GONE);
        findViewById(R.id.bt_showBT).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bt_status.getVisibility() == View.GONE){
                    bt_status.setVisibility(View.VISIBLE);
                    return;
                }else {
                    bt_status.setVisibility(View.GONE);
                }

            }
        });


    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bluetoothTools != null)bluetoothTools.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(bluetoothTools != null)bluetoothTools.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(bluetoothTools != null)bluetoothTools.onPause();
    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onClick(View v) {
        FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
        switch (v.getId()){
            case R.id.bt_map:
                if(cameraFragment != null)mFragmentTransaction.hide(cameraFragment);
                if(playFragment != null)mFragmentTransaction.hide(playFragment);
                mFragmentTransaction.show(mapFragment).commit();
                break;
            case R.id.bt_video:
                if(cameraFragment == null){
                    cameraFragment = AddCameraFragment.newInstance("","");
                    mFragmentTransaction.hide(mapFragment);
                    if(playFragment != null)mFragmentTransaction.hide(playFragment);
                    mFragmentTransaction.add(R.id.fragmentsHolder,cameraFragment).commit();
                }else {
                    mFragmentTransaction.hide(mapFragment);
                    if(playFragment != null)mFragmentTransaction.hide(playFragment);
                    mFragmentTransaction.show(cameraFragment).commit();
                }
                break;
            case R.id.bt_settings:
                if(playFragment == null){
                    playFragment = PlayFragment.newInstance("","");
                    mFragmentTransaction.hide(mapFragment);
                    mFragmentTransaction.hide(cameraFragment);
                    mFragmentTransaction.add(R.id.fragmentsHolder,playFragment).commit();
                }else {
                    mFragmentTransaction.hide(mapFragment);
                    mFragmentTransaction.hide(cameraFragment);
                    mFragmentTransaction.show(playFragment).commit();
                }
                break;
        }
    }
}
