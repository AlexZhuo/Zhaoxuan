package org.alex.zhaoxuan;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import com.smartwebee.android.blespp.BluetoothLeService;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2017/6/10.
 */
public class BluetoothTools implements View.OnClickListener{
    static long recv_cnt = 0;
    public String mDeviceAddress;
    public BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    public boolean mConnected = false;

    private long recvBytes=0;
    private long lastSecondBytes=0;
    private long sendBytes;
    private StringBuilder mData = new StringBuilder();;

    int sendIndex = 0;
    int sendDataLen=0;
    byte[] sendBuf;

    private TextView mDataRecvText;
    private TextView mRecvBytes;
    private TextView mDataRecvFormat;
    private EditText mEditBox;
    private TextView mSendBytes;
    private TextView mDataSendFormat;
    private TextView mNotify_speed_text;
    private String mFrame;
    Button mSendBtn;
    Button mCleanBtn;
    Button mCleanTextBtn;

    //测速
    private Timer timer;
    private TimerTask task;

    private Activity context;

    private BTReceiver receiver;

    public BluetoothTools(
            Activity activity,
            String mDeviceAddress,
            TextView mDataRecvText,
            TextView mRecvBytes,
            TextView mDataRecvFormat,
            EditText mEditBox,
            TextView mSendBytes,
            TextView mDataSendFormat,
            final TextView mNotify_speed_text,
            Button mSendBtn,
            Button mCleanBtn,
            Button mCleanTextBtn,
            final BTReceiver receiver
    ){
            this.context = activity;
            this.mDeviceAddress = mDeviceAddress;
            this.mDataRecvText = mDataRecvText;
            this.mRecvBytes = mRecvBytes;
            this.mDataRecvFormat = mDataRecvFormat;
            this.mEditBox = mEditBox;
            this.mSendBytes = mSendBytes;
            this.mDataSendFormat = mDataSendFormat;
            this.mNotify_speed_text = mNotify_speed_text;
            this.mSendBtn = mSendBtn;
            this.mCleanBtn = mCleanBtn;
            this.mCleanTextBtn = mCleanTextBtn;
            this.receiver = receiver;

        mDataRecvFormat.setOnClickListener(this);
        mDataSendFormat.setOnClickListener(this);
        mRecvBytes.setOnClickListener(this);
        mSendBytes.setOnClickListener(this);

        mCleanBtn.setOnClickListener(this);
        mSendBtn.setOnClickListener(this);
        mCleanTextBtn.setOnClickListener(this);
        mDataRecvText.setMovementMethod(ScrollingMovementMethod.getInstance());

        //测试用的一块代码
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                receiver.onReceive("AA120,115.5,17BB");
//            }
//        },10000);

        final int SPEED = 1;
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SPEED:
                        lastSecondBytes = recvBytes - lastSecondBytes;
                        mNotify_speed_text.setText(String.valueOf(lastSecondBytes)+ " B/s");
                        lastSecondBytes = recvBytes;
                        break;
                }
            }
        };

        task = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = SPEED;
                message.obj = System.currentTimeMillis();
                handler.sendMessage(message);
            }
        };

        timer = new Timer();
        // 参数：
        // 1000，延时1秒后执行。
        // 1000，每隔2秒执行1次task。
        timer.schedule(task, 1000, 1000);

        Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
        context.bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.i("Alex","service 建立");
            if (!mBluetoothLeService.initialize()) {
                Log.e("Alex", "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
           if( mBluetoothLeService.connect(mDeviceAddress)){
               Log.i("Alex","感觉应该还不错");
           }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            Log.i("Alex","service 断开");
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_SUCCESSFUL);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_NO_DISCOVERED);
        return intentFilter;
    }

    //获取输入框十六进制格式
    private static String getHexString(String text) {
        String s = text.toString();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (('0' <= c && c <= '9') || ('a' <= c && c <= 'f') ||
                    ('A' <= c && c <= 'F')) {
                sb.append(c);
            }
        }
        if ((sb.length() % 2) != 0) {
            sb.deleteCharAt(sb.length());
        }
        return sb.toString();
    }

    ////将字符换转换为字节数组
    private static byte[] stringToBytes(String s) {
        byte[] buf = new byte[s.length() / 2];
        for (int i = 0; i < buf.length; i++) {
            try {
                buf[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return buf;
    }
    ////将ascii码转换为字符串
    public static String asciiToString(byte[] bytes) {
        char[] buf = new char[bytes.length];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (char) bytes[i];
            sb.append(buf[i]);
        }
        return sb.toString();
    }
    ////将字节数组转换为字符串
    public static String bytesToString(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];

            sb.append(hexChars[i * 2]);
            sb.append(hexChars[i * 2 + 1]);
            sb.append(' ');
        }
        return sb.toString();
    }


    private void displayData(byte[] buf,BTReceiver r) {
        recvBytes += buf.length;
        recv_cnt += buf.length;

        if (recv_cnt>=1024)
        {
            recv_cnt = 0;
            mData.delete(0,mData.length()/2); //UI界面只保留512个字节，免得APP卡顿
        }

        if (mDataRecvFormat.getText().equals("Ascii")) {
            String s =asciiToString(buf);
            mData.append(s);
            mFrame = s;
            System.out.print("这就是数据");
        } else {
            String s = bytesToString(buf);
            mData.append(s);
            mFrame = s;
        }
        /////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////
        mDataRecvText.setText(mData.toString());//mdata是所收到的数据
        mRecvBytes.setText(recvBytes+"");
        /////////////////////////////////////////////////////////////////////////
        /*
        主要工作：把mdata写到一个新的类中并打包。调用这个数据；
         */
        ///////////////////////////////////////////////////////////////////////////
        r.onReceive(mFrame);
    }

    public interface BTReceiver{
        void onReceive(String s);
    }

    private void updateConnectionState(final int resourceId) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // mConnectionState.setText(resourceId);
            }
        });
    }

    private void getSendBuf(){
        sendIndex = 0;
        if (mDataSendFormat.getText().equals(context.getResources().getString(com.smartwebee.android.blespp.R.string.data_format_default))) {
            sendBuf = mEditBox.getText().toString().trim().getBytes();
        } else {
            sendBuf = stringToBytes(getHexString(mEditBox.getText().toString()));
        }
        sendDataLen = sendBuf.length;
    }
    private void onSendBtnClicked() {
        if (sendDataLen>20) {
            sendBytes += 20;
            final byte[] buf = new byte[20];
            // System.arraycopy(buffer, 0, tmpBuf, 0, writeLength);
            for (int i=0;i<20;i++)
            {
                buf[i] = sendBuf[sendIndex+i];
            }
            sendIndex+=20;
            mBluetoothLeService.writeData(buf);
            sendDataLen -= 20;
        }
        else {
            sendBytes += sendDataLen;
            final byte[] buf = new byte[sendDataLen];
            for (int i=0;i<sendDataLen;i++)
            {
                buf[i] = sendBuf[sendIndex+i];
            }
            mBluetoothLeService.writeData(buf);
            sendDataLen = 0;
            sendIndex = 0;
        }
    }
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i("Alex","收到了广播"+action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(com.smartwebee.android.blespp.R.string.disconnected);
//                invalidateOptionsMenu();
                mBluetoothLeService.connect(mDeviceAddress);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //特征值找到才代表连接成功
                mConnected = true;
//                invalidateOptionsMenu();
                updateConnectionState(com.smartwebee.android.blespp.R.string.connected);
            }else if (BluetoothLeService.ACTION_GATT_SERVICES_NO_DISCOVERED.equals(action)){
                mBluetoothLeService.connect(mDeviceAddress);
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
//                final StringBuilder stringBuilder = new StringBuilder();
//                 for(byte byteChar : data)
//                      stringBuilder.append(String.format("%02X ", byteChar));
//                Log.v("log",stringBuilder.toString());
                displayData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA),receiver);
            }else if (BluetoothLeService.ACTION_WRITE_SUCCESSFUL.equals(action)) {
                mSendBytes.setText(sendBytes + " ");
                if (sendDataLen>0)
                {
                    Log.v("log","Write OK,Send again");
                    onSendBtnClicked();
                }
                else {
                    Log.v("log","Write Finish");
                }
            }

        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.data_received_format:
                if (mDataRecvFormat.getText().equals(context.getResources().getString(R.string.data_format_default))) {
                    convertText(mDataRecvFormat, R.string.data_format_hex);
                } else {
                  convertText(mDataRecvFormat,R.string.data_format_default);
                }
                break;

            case R.id.data_sended_format:
                if (mDataSendFormat.getText().equals(context.getResources().getString(R.string.data_format_default)))  {
                    convertText(mDataSendFormat, R.string.data_format_hex);
                } else {
                    convertText(mDataSendFormat, R.string.data_format_default);
                }
                break;

            case R.id.byte_received_text:
                recvBytes = 0;
                lastSecondBytes=0;
                convertText(mRecvBytes, R.string.zero);
                break;

            case R.id.byte_send_text:
                sendBytes = 0;
                convertText(mSendBytes, R.string.zero);
                break;

            case R.id.send_data_btn:
                getSendBuf();
                onSendBtnClicked();
                break;

            case R.id.clean_data_btn:
                mData.delete(0, mData.length());
                mDataRecvText.setText(mData.toString());
                break;

            case R.id.clean_text_btn:
                mEditBox.setText("");
                break;

            default:
                break;
        }
    }

    //动态效果
    public static void convertText(final TextView textView, final int convertTextId) {
        final Animation scaleIn = AnimationUtils.loadAnimation(textView.getContext(),
                com.smartwebee.android.blespp.R.anim.text_scale_in);
        Animation scaleOut = AnimationUtils.loadAnimation(textView.getContext(),
                com.smartwebee.android.blespp.R.anim.text_scale_out);
        scaleOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                textView.setText(convertTextId);
                textView.startAnimation(scaleIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        textView.startAnimation(scaleOut);
    }

    public void onResume() {
        context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d("Alex", "Connect request result=" + result);

        }
    }
    public void onPause() {
        context.unregisterReceiver(mGattUpdateReceiver);
    }

    public void onDestroy() {
        context.unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


}
