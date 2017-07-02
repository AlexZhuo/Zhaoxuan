package org.alex.zhaoxuan.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.lzy.okgo.callback.StringCallback;


import org.alex.zhaoxuan.Activities.FunctionActivity;
import org.alex.zhaoxuan.LocationUtils;
import org.alex.zhaoxuan.NetworkTools;
import org.alex.zhaoxuan.R;
import org.alex.zhaoxuan.RadarTarget;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RadarMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RadarMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RadarMapFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    String indoor_lat;
    String indoor_lng;
    private String ipAddress;//服务器IP地址

    private OnFragmentInteractionListener mListener;

    private long lastUpdateTime;//上次更新周围的时间

    private float mapZoomLevel = 18;//初始的镜头范围
    private HashMap<Integer,Marker> markerMap = new HashMap<>();
    public int userDeviceID;
    final RadarTarget myPosition = new RadarTarget();//我的位置
    private boolean indoorMode;
    TextView myLatitude;
    TextView myLongitude;
    TextView mySpeed;
    TextView myAccuracy;
    boolean movedToCenter = false;//判断是否已经从北京

    //===============以下是地图SDK================
    AMap aMap;
    MapView mapView;
    private UiSettings mUiSettings;//定义一个UiSettings对象
    //===============以下三个声明是定位SDK的=================
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    public AMapLocationListener mLocationListener = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    public RadarMapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RadarMapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RadarMapFragment newInstance(String param1, String param2, String ip,String indoor_lat,String indoor_lng) {
        RadarMapFragment fragment = new RadarMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        args.putString("indoor_lat",indoor_lat);
        args.putString("indoor_lng",indoor_lng);
        args.putString("ip",ip);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            ipAddress = getArguments().getString("ip");
            Log.i("Alex","传过来的IP"+ipAddress);
            indoor_lat = getArguments().getString("indoor_lat");
            indoor_lng = getArguments().getString("indoor_lng");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_radar_map, container, false);
        myLatitude = (TextView)view.findViewById(R.id.myLatitude);
        myLongitude = (TextView)view.findViewById(R.id.myLongitude);
        mySpeed = (TextView)view.findViewById(R.id.mySpeed);
        myAccuracy = (TextView)view.findViewById(R.id.myAccuracy);

        if(!TextUtils.isEmpty(indoor_lat) && !TextUtils.isEmpty(indoor_lng)){
            //开启室内模式
            indoorMode = true;
            myPosition.accuracy = 1.0f;
            myPosition.latitude = new Double(indoor_lat);
            myPosition.longitude = new Double(indoor_lng);
        }
        Log.i("Alex","传过来的IP"+ipAddress);
        //======================以下是地图SDK功能====================
        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        aMap = mapView.getMap();
        mUiSettings = aMap.getUiSettings();//实例化UiSettings类对象
        aMap.setTrafficEnabled(false);// 显示实时交通状况
        //地图模式可选类型：MAP_TYPE_NORMAL,MAP_TYPE_SATELLITE,MAP_TYPE_NIGHT
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);// 卫星地图模式
        mUiSettings.setCompassEnabled(true);
        mUiSettings.setScaleControlsEnabled(true);//控制比例尺控件是否显示
        MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        if(indoorMode){
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);//连续定位、蓝点不会移动到地图中心点，并且蓝点会跟随设备移动。
            myLocationStyle.showMyLocation(false);//室内模式不显示定位蓝点
            Log.i("Alex","准备移动camera"+myPosition.latitude+"   "+myPosition.longitude);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(myPosition.latitude,myPosition.longitude),mapZoomLevel,0,0));
                    aMap.moveCamera(mCameraUpdate);
                }
            },2500);
            MarkerOptions markerOption = new MarkerOptions();
            markerOption.position(new LatLng(myPosition.latitude,myPosition.longitude));
            markerOption.title("雷达位置").snippet("经度："+myPosition.longitude+"\n纬度："+myPosition.latitude);
            markerOption.draggable(true);//设置Marker可拖动
            markerOption.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.circle)));
            // 将Marker设置为贴地显示，可以双指下拉地图查看效果
            markerOption.setFlat(true);//设置marker平贴地图效果
            aMap.addMarker(markerOption);
            aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {

                }

                @Override
                public void onMarkerDrag(Marker marker) {
                    if("雷达位置".equals(marker.getTitle())){
                        myPosition.latitude = marker.getPosition().latitude;
                        myPosition.longitude = marker.getPosition().longitude;
                        marker.setSnippet("经度："+myPosition.longitude+"\n纬度："+myPosition.latitude);
                        myLatitude.setText("经度："+myPosition.longitude);
                        myLongitude.setText("纬度："+myPosition.latitude);
                    }
                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    if("雷达位置".equals(marker.getTitle())){
                        SharedPreferences sp = getActivity().getSharedPreferences("config", getActivity().MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("latitude", myPosition.latitude+"");
                        editor.putString("longitude", myPosition.longitude+"");
                        editor.commit();
                    }
                }
            });
            aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {
                    Log.i("Alex","我擦我擦"+location.getLatitude());
                    location.setLatitude(myPosition.latitude);
                    location.setLongitude(myPosition.longitude);
                }
            });
        }else {
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);//连续定位、蓝点不会移动到地图中心点，定位点依照设备方向旋转，并且蓝点会跟随设备移动。
        }
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        // 定义 Marker 点击事件监听
        AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
            // marker 对象被点击时回调的接口
            // 返回 true 则表示接口已响应事件，否则返回false
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(marker.isInfoWindowShown())marker.hideInfoWindow();
                else marker.showInfoWindow();
                return true;
            }
        };
        // 绑定 Marker 被点击事件
        aMap.setOnMarkerClickListener(markerClickListener);
        //==========================以下是定位SDK功能=========================
        //初始化定位
        mLocationClient = new AMapLocationClient(getActivity().getApplicationContext());
        mLocationListener = new AMapLocationListener(){
            @Override
            public void onLocationChanged(AMapLocation amapLocation) {
                if (!indoorMode && amapLocation == null){
                    Toast.makeText(getActivity(),"手机定位失败2",Toast.LENGTH_LONG).show();
                    return;
                }
                if (amapLocation.getErrorCode() == 0 || indoorMode) {
                    //可在其中解析amapLocation获取相应内容。
                    if(amapLocation != null)myPosition.sensorType = amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                    if(!indoorMode) {//非室内模式使用GPS信息
                        myPosition.latitude = amapLocation.getLatitude();//获取纬度
                        myPosition.longitude = amapLocation.getLongitude();//获取经度
                        myPosition.accuracy = amapLocation.getAccuracy();//获取精度信息
                    }
                    if(amapLocation != null)myPosition.speed = amapLocation.getSpeed();
                    //获取定位时间
                    if(amapLocation != null) myPosition.time = amapLocation.getTime();
                    myPosition.targetName = Build.BRAND+" "+Build.MODEL;
                    if(amapLocation != null)myPosition.city = amapLocation.getCity();
                    if(amapLocation != null)myPosition.jiedao = amapLocation.getDistrict()+" "+amapLocation.getStreet();
                    myPosition.targetId = userDeviceID;
                    ((FunctionActivity)getActivity()).myPosition = myPosition;
                    myLatitude.setText("经度："+myPosition.longitude);
                    myLongitude.setText("纬度："+myPosition.latitude);
                    mySpeed.setText("速度："+new DecimalFormat("0.000").format(myPosition.speed*3.6)+" km/h");
                    myAccuracy.setText("精确度："+myPosition.accuracy+"m");
                    //参数依次是：视角调整区域的中心点坐标、希望调整到的缩放级别、俯仰角0°~45°（垂直与地图时为0）、偏航角 0~360° (正北方为0)
                    if(amapLocation.getLocationType() != 2 && !movedToCenter){
//                            CameraPosition cameraPosition = aMap.getCameraPosition();
//                            if(myPosition.latitude-cameraPosition.target.latitude < 1 && myPosition.longitude - cameraPosition.target.latitude < 1)return;
                        CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(amapLocation.getLatitude(),amapLocation.getLongitude()),mapZoomLevel,0,0));
                        aMap.moveCamera(mCameraUpdate);
                        movedToCenter = true;
                    }
                    //==================向服务器发送手机位置并接收目标的位置=================
//                        updatePhonePosition();
                    ArrayList<RadarTarget> targets = new ArrayList<>();
                    RadarTarget t = ((FunctionActivity)getActivity()).t;
                    if(t != null)targets.add(t);
                    updateRadarTarget(targets);
                    Log.i("Alex","发送到服务器的目标是"+t.toString());
                }else {
                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError","location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                    Toast.makeText(getActivity(),"手机定位失败",Toast.LENGTH_LONG).show();
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

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId = tm.getDeviceId();
            String sim = tm.getSimSerialNumber();
            String imsi = ((TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            userDeviceID = (deviceId+sim+imsi).hashCode();
        }catch (Exception e){

        }
        //启动定位
        mLocationClient.startLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mapView.onDestroy();
        mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
    }

    @Override
    public void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapView.onResume();
        Log.i("Alex","是否恢复了定位"+mLocationClient.isStarted());
        mLocationClient.startLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    public static class ReciveMessage{
        public long timestamp;
        public ArrayList<RadarTarget> locationList;
    }



    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * 更新附近手机的位置并显示在地图上
     */
    public void updatePhonePosition(){
        NetworkTools.sendPhoneLocationToServer(ipAddress,myPosition,getActivity(),
                new StringCallback() {
                    @Override
                    public void onSuccess(String s, Call call, Response response) {
                        // s 即为所需要的结果
                        Log.i("Alex","访问成功"+s);
                        ReciveMessage msg = JSON.parseObject(s,ReciveMessage.class);
                        if(msg == null)return;
                        if(msg.timestamp <= lastUpdateTime)return;
                        boolean hasNewTarget = false;//判断有没有新的坐标，需要放大地图
                        lastUpdateTime = msg.timestamp;
                        HashMap<Integer,Marker> newMap = new HashMap<Integer, Marker>();
                        for(RadarTarget p:msg.locationList){

                            //旧的坐标
                            if(markerMap.containsKey(p.targetId)){
                                Marker m = markerMap.get(p.targetId);
                                m.setPosition(new LatLng(p.latitude,p.longitude));
                                m.setSnippet(NetworkTools.genSnippet(p));
                                //吧送来的marker放到新的map李去
                                newMap.put(p.targetId,m);
                                markerMap.remove(p.targetId);
                                continue;
                            }
                            //新的坐标
                            hasNewTarget = true;
                            //=============设置marker============
                            LatLng latLng = new LatLng(p.latitude,p.longitude);
                            Marker newMarker = aMap.addMarker(new MarkerOptions().position(latLng).title(p.targetName).snippet(NetworkTools.genSnippet(p)));
                            newMap.put(p.targetId,newMarker);
                        }
                        for(int i:markerMap.keySet()){
                            markerMap.get(i).setVisible(false);
                            markerMap.get(i).setPosition(new LatLng(-90,0));
                            markerMap.get(i).remove();
                            Log.i("Alex","去掉一个设备："+i);
                            markerMap.remove(i);
                        }

                        markerMap = newMap;
                        float zoomLevel = LocationUtils.getZoomLevelForTargetsAndCenter(msg.locationList,myPosition,aMap);
                        mapZoomLevel = aMap.getCameraPosition().zoom;
                        if(zoomLevel < mapZoomLevel && hasNewTarget) {//需要扩大视野
                            Log.i("AlexZoom", "修改zoom Level：" + zoomLevel);
//                                                aMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
                            //中心点坐标、希望调整到的缩放级别、俯仰角0°~45°（垂直与地图时为0）、偏航角 0~360° (正北方为0)
                            aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(myPosition.latitude,myPosition.longitude),zoomLevel,0,0)));
                            mapZoomLevel = zoomLevel;
                        }
                    }

                    @Override
                    public void onError(Call call, Response response, Exception e) {
                        super.onError(call, response, e);
                        Log.i("Alex","访问失败",e);
                        Toast.makeText(getActivity(),"访问服务器失败",Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * 在收到雷达发来的目标信息后执行该方法，把雷达发现的目标发到服务器并进行显示
     */
    public void updateRadarTarget(final Collection<RadarTarget> targets){
        NetworkTools.sendTargetLocationToServer(ipAddress,targets,getActivity(),2,//让所有客户端的target id都相同，那么就不会收到手机的位置了
                new StringCallback() {
                    @Override
                    public void onSuccess(String s, Call call, Response response) {
                        // s 即为所需要的结果
                        Log.i("Alex","访问成功"+s);
                        ReciveMessage msg = JSON.parseObject(s,ReciveMessage.class);
                        if(msg == null)return;
                        if(msg.timestamp <= lastUpdateTime)return;
                        boolean hasNewTarget = false;//判断有没有新的坐标，需要放大地图
                        lastUpdateTime = msg.timestamp;
                        HashMap<Integer,Marker> newMap = new HashMap<Integer, Marker>();
                        for(RadarTarget p:msg.locationList){
                            //旧的坐标
                            if(markerMap.containsKey(p.targetId)){
                                Marker m = markerMap.get(p.targetId);
                                m.setPosition(new LatLng(p.latitude,p.longitude));
                                m.setSnippet(NetworkTools.genSnippet(p));
                                //吧送来的marker放到新的map李去
                                newMap.put(p.targetId,m);
                                markerMap.remove(p.targetId);
                                continue;
                            }
                            //新的坐标
                            hasNewTarget = true;
                            //=============设置marker============
                            LatLng latLng = new LatLng(p.latitude,p.longitude);
                            Marker newMarker = aMap.addMarker(new MarkerOptions().position(latLng).title(p.targetName).snippet(NetworkTools.genSnippet(p)));
                            newMap.put(p.targetId,newMarker);
                        }
                        for(int i:markerMap.keySet()){
                            markerMap.get(i).setVisible(false);
                            markerMap.get(i).setPosition(new LatLng(-90,0));
                            markerMap.get(i).remove();
                            Log.i("Alex","去掉一个设备："+i);
                            markerMap.remove(i);
                        }

                        markerMap = newMap;
                        if(msg.locationList != null && msg.locationList.size() != 0) {
                            if(msg.locationList.size() == 1 )msg.locationList.add(myPosition);
                            double[] latlngs = LocationUtils.getMaxMinLatLng(msg.locationList);
                            if (latlngs == null || latlngs.length == 0) return;
                            LatLng southwestLatLng = new LatLng(latlngs[0], latlngs[1]);
                            LatLng northeastLatLng = new LatLng(latlngs[2], latlngs[3]);
                            Log.i("Alex", "经纬度限制：" + southwestLatLng + "," + northeastLatLng);
                            double cameraLatitude = (latlngs[0] + latlngs[2]) / 2;
                            double cameraLongitude = (latlngs[1] + latlngs[3]) / 2;//镜头为所有标记点的中心
                            float zoomLevel = aMap.getZoomToSpanLevel(southwestLatLng, northeastLatLng)-1;
                            Log.i("Alex","zoom level 是"+zoomLevel);
                            mapZoomLevel = aMap.getCameraPosition().zoom;
                            if (zoomLevel < mapZoomLevel && hasNewTarget) {//需要扩大视野
                                Log.i("Alex", "修改zoom Level：" + zoomLevel);
//                                                aMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
                                //中心点坐标、希望调整到的缩放级别、俯仰角0°~45°（垂直与地图时为0）、偏航角 0~360° (正北方为0)
                                aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(cameraLatitude, cameraLongitude), zoomLevel, 0, 0)));
                                mapZoomLevel = zoomLevel;
                            }
                        }

                    }

                    @Override
                    public void onError(Call call, Response response, Exception e) {
                        super.onError(call, response, e);
                        Log.i("Alex","访问失败",e);
                        Toast.makeText(getActivity(),"访问服务器失败",Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
