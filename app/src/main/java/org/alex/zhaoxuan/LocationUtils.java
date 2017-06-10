package org.alex.zhaoxuan;

import android.util.Log;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;

import java.util.Collection;

/**
 * Created by Administrator on 2017/5/19.
 */
public class LocationUtils {

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 通过经纬度获取距离(单位：米)
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return
     */
    public static double getDistance(double lat1, double lng1, double lat2,double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000d) / 10000d;
        return s;
    }

    public static int getZoomLevelByDistance(double distance){
        if(distance > 100000){//100km
            return 10;
        }else if(distance > 80000){//80km
            return 12;
        }else if(distance > 50000){//50km
            return 14;
        }else if(distance > 30000){//30km
            return 15;
        }else if(distance > 20000){//20km
            return 16;
        }else if(distance > 10000){//10km
            return 17;
        }else if(distance > 5000){//5km
            return 18;
        }else{//1-5km
            return 19;
        }
    }

    /**
     * 在一大堆目标点中得到他们的经纬度范围
     * @param targets
     * @return
     */
    public static double[] getMaxMinLatLng(Collection<RadarTarget> targets){
        double minLatitude = 90;
        double maxLatitude = -90;
        double minLongitud = 180;
        double maxLongitude = -180;
        for(RadarTarget t:targets){
            if(t.latitude < minLatitude)minLatitude = t.latitude;
            if(t.latitude > maxLatitude)maxLatitude = t.latitude;
            if(t.longitude < minLongitud)minLongitud = t.longitude;
            if(t.longitude > maxLongitude)maxLongitude = t.longitude;
        }
        //西北，东南
        return new double[]{minLatitude,minLongitud,maxLatitude,maxLongitude};
    }

    /**
     * 以自己为地图的中心计算显示大量目标点的经纬度范围
     * @param targets
     * @param myPosition
     * @return
     */
    public static float getZoomLevelForTargetsAndCenter(Collection<RadarTarget> targets, RadarTarget myPosition, AMap aMap){
        double maxLat = 0;//最远纬度的绝对值
        double maxlng = 0;//最远经度的绝对值
        for(RadarTarget p:targets) {
            double latDis = Math.abs(p.latitude - myPosition.latitude);
            double lngDis = Math.abs(p.longitude - myPosition.longitude);
            if (latDis > maxLat) maxLat = latDis;
            if (lngDis > maxlng) maxlng = lngDis;
        }
        if(maxLat < 0.01 && maxlng < 0.01) return 18;
        LatLng southwestLatLng = new LatLng(myPosition.latitude - maxLat, myPosition.longitude - maxlng);
        LatLng northeastLatLng = new LatLng(myPosition.latitude + maxLat, myPosition.longitude + maxlng);
        Log.i("AlexZoom", "经纬度限制：" + southwestLatLng + ",," + northeastLatLng);
        return aMap.getZoomToSpanLevel(southwestLatLng,northeastLatLng)-1;
    }


    private final static double EARTH_RADIUS = 6378138.0;
    private final static double PI = 3.14159265;
    private final static double Rc = 6378137;  // 赤道半径
    private final static double Rj = 6356725;  // 极半径

    /**
     * @param distance Unit:Km
     * @param angle    i.g. 45/135/225/315
     * @return result[0] latitude<br>result[1] longitude
     */
    public static double[] getGPSLocation(double latitude, double longitude, double distance, double angle) {
        double[] result = {0, 0};

        double m_Latitude;
        double m_RadLo, m_RadLa;
        double Ec;
        double Ed;

        m_Latitude = latitude;
        m_RadLo = longitude * PI / 180.0;
        m_RadLa = latitude * PI / 180.0;
        Ec = Rj + (Rc - Rj) * (90.0 - m_Latitude) / 90.0;
        Ed = Ec * Math.cos(m_RadLa);

        double dx = distance * Math.sin(angle * PI / 180.0);
        double dy = distance * Math.cos(angle * PI / 180.0);

        double BJD = (dx / Ed + m_RadLo) * 180.0 / PI;
        double BWD = (dy / Ec + m_RadLa) * 180.0 / PI;

        result[0] = BWD;
        result[1] = BJD;
        return result;
    }

}
