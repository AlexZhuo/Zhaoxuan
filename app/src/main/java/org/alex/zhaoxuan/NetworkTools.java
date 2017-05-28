package org.alex.zhaoxuan;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;

import java.text.DecimalFormat;
import java.util.Collection;

/**
 * Created by Administrator on 2017/5/20.
 */
public class NetworkTools {
    /**
     * 将手机设备的位置信息上传到服务器
     * @param ipAddress
     * @param myPosition
     * @param tag
     * @param callback
     */
    public static void sendPhoneLocationToServer(String ipAddress, RadarTarget myPosition, Object tag, StringCallback callback){
        OkGo.get("http://"+ipAddress+"/ZhaoxuanServer/RegistClient")     // 请求方式和请求url
                .tag(tag)                       // 请求的 tag, 主要用于取消对应的请求
                .cacheKey("device")            // 设置当前请求的缓存key,建议每个不同功能的请求设置一个
                .params("latitude", myPosition.latitude)
                .params("longitude", myPosition.longitude)
                .params("accuracy", myPosition.accuracy)
                .params("targetId", myPosition.targetId)
                .params("targetName", myPosition.targetName)
                .params("city", myPosition.city)
                .params("speed", myPosition.speed)
                .params("jiedao", myPosition.jiedao)
                .params("bearing", myPosition.bearing)
                .params("altitude",myPosition.altitude)
                .execute(callback);
    }

    /**
     * 将雷达发来的目标信息上传到服务器
     * @param ipAddress
     * @param targets
     * @param tag
     * @param callback
     */
    public static void sendTargetLocationToServer(String ipAddress,Collection<RadarTarget> targets, Object tag,int senderId,StringCallback callback){
        OkGo.post("http://"+ipAddress+"/ZhaoxuanServer/RegistClient")     // 请求方式和请求url
                .tag(tag)                       // 请求的 tag, 主要用于取消对应的请求
                .cacheKey("targets")            // 设置当前请求的缓存key,建议每个不同功能的请求设置一个
                .params("senderId",senderId)
                .params("data", JSON.toJSONString(targets))
                .execute(callback);
    }

    public static String genSnippet(RadarTarget p,double myLat,double mylon){
        if(p == null) return "无信息";
        double distance = LocationUtils.getDistance(p.latitude,p.longitude,myLat,mylon);
        Log.i("Alex","距离是："+distance);
        distance = Math.round(distance);
        return  "经度:"+p.longitude+"\n纬度:"+p.latitude+
                "\n速度："+new DecimalFormat("0.000").format(p.speed*3.6)+" km/h"+
                "\n海拔："+p.altitude+" m"+
                "\n街道："+p.jiedao+
                "\n精确度："+p.accuracy+" m"+
                "\n距离我："+ (distance>2000?distance/1000+" km":distance+" m")+
                "\n上次更新："+(Math.round((System.currentTimeMillis() - p.time)/1000))+" 秒前";
    }
}
