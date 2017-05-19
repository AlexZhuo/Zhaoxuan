package org.alex.zhaoxuan;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2017/5/19.
 */
public class AlxPosition {
    public int sensorType;
    public double latitude;
    public double longitude;
    public float accuracy;
    public long time;
    public float speed;
    public float bearing;
    public String deviceName;
    public int deviceID;
    public String city;
    public String jiedao;
    public String getDate(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if(time == 0)return "none";
        Date date = new Date(time);
        return df.format(date);
    }

    @Override
    public String toString() {
        return "AlxPosition{" +
                "sensorType=" + sensorType +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", time=" + time +
                ", speed=" + speed +
                ", bearing=" + bearing +
                ", deviceName='" + deviceName + '\'' +
                ", deviceID='" + deviceID + '\'' +
                ", city='" + city + '\'' +
                ", jiedao='" + jiedao + '\'' +
                '}';
    }
}
