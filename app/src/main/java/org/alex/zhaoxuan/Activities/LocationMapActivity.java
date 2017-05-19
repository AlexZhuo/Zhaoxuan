package org.alex.zhaoxuan.Activities;

import android.content.Context;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.alex.zhaoxuan.AlxPosition;
import org.alex.zhaoxuan.R;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;


public class LocationMapActivity extends AppCompatActivity {

    public int userDeviceID;
    //===============已下是地图SDK================
    AMap aMap;
    MapView mapView;
    //===============以下三个声明是定位SDK的=================
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    public AMapLocationListener mLocationListener = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_map);
        //======================以下是地图SDK功能====================
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        aMap = mapView.getMap();
        aMap.setTrafficEnabled(false);// 显示实时交通状况
        //地图模式可选类型：MAP_TYPE_NORMAL,MAP_TYPE_SATELLITE,MAP_TYPE_NIGHT
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);// 卫星地图模式
        MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        //==========================以下是定位SDK功能=========================
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        mLocationListener = new AMapLocationListener(){
            @Override
            public void onLocationChanged(AMapLocation amapLocation) {
                if (amapLocation != null) {
                    if (amapLocation.getErrorCode() == 0) {
                        //可在其中解析amapLocation获取相应内容。
                        AlxPosition position = new AlxPosition();
                        position.sensorType = amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                        position.latitude = amapLocation.getLatitude();//获取纬度
                        position.longitude = amapLocation.getLongitude();//获取经度
                        position.accuracy = amapLocation.getAccuracy();//获取精度信息
                        position.speed = amapLocation.getSpeed();
                        //获取定位时间
                        position.time = amapLocation.getTime();
                        position.deviceName = Build.BRAND+" "+Build.MODEL;
                        position.city = amapLocation.getCity();
                        position.jiedao = amapLocation.getDistrict()+" "+amapLocation.getStreet();
                        position.deviceID = userDeviceID;
                        Toast.makeText(LocationMapActivity.this,position.toString(),Toast.LENGTH_LONG).show();
                        //参数依次是：视角调整区域的中心点坐标、希望调整到的缩放级别、俯仰角0°~45°（垂直与地图时为0）、偏航角 0~360° (正北方为0)
                        if(amapLocation.getLocationType() != 2){
                            CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(amapLocation.getLatitude(),amapLocation.getLongitude()),15,0,0));
                            aMap.moveCamera(mCameraUpdate);
                        }
                    }else {
                        //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                        Log.e("AmapError","location Error, ErrCode:"
                                + amapLocation.getErrorCode() + ", errInfo:"
                                + amapLocation.getErrorInfo());
                    }
                }
            }
        };
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);

        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        mLocationOption.setInterval(2000);
        //设置是否强制刷新WIFI，默认为true，强制刷新。
        mLocationOption.setWifiScan(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(10000);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();


    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            TelephonyManager tm = (TelephonyManager) LocationMapActivity.this.getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId = tm.getDeviceId();
            String sim = tm.getSimSerialNumber();
            String imsi = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            userDeviceID = (deviceId+sim+imsi).hashCode();
        }catch (Exception e){

        }
        //=============设置marker============
        LatLng latLng = new LatLng(36,119.397972);
        final Marker marker = aMap.addMarker(new MarkerOptions().position(latLng).title("北京").snippet("哈哈哈"));
        // 定义 Marker 点击事件监听
        AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
            // marker 对象被点击时回调的接口
            // 返回 true 则表示接口已响应事件，否则返回false
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                return true;
            }
        };
// 绑定 Marker 被点击事件
        aMap.setOnMarkerClickListener(markerClickListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationClient.stopLocation();//停止定位后，本地定位服务并不会被销毁
        mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }
}
