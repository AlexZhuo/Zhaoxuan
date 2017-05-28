package org.alex.zhaoxuan;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2017/5/19.
 */
public class RadarTarget {
    public int sensorType;
    public double latitude;
    public double longitude;
    public float accuracy;
    public long time;
    public float speed;
    public float bearing;
    public String targetName;
    public int targetId;
    public String city;
    public String jiedao;
    public double altitude;
    public String getDate(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if(time == 0)return "none";
        Date date = new Date(time);
        return df.format(date);
    }

    @Override
    public String toString() {
        return "RadarTarget{" +
                "sensorType=" + sensorType +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", time=" + time +
                ", speed=" + speed +
                ", bearing=" + bearing +
                ", targetName='" + targetName + '\'' +
                ", targetId=" + targetId +
                ", city='" + city + '\'' +
                ", jiedao='" + jiedao + '\'' +
                ", altitude=" + altitude +
                '}';
    }
}
