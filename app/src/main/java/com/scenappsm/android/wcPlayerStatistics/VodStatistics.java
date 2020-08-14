package com.scenappsm.android.wcPlayerStatistics;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.scenappsm.wecandeosdkplayer.WecandeoSdk;
import com.scenappsm.wecandeosdkplayer.WecandeoVideo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class VodStatistics {

    Context context;
    WecandeoSdk wecandeoSdk;

    private String sectionsUrl;
    private String playsUrl;
    private String cuePointUrl;

    private static final String TAG = "VodStatistics";
    public static final String PLAY = "PLAY";
    public static final String PAUSE = "PAUSE";
    public static final String STOP = "STOP";
    public static final String RETRY = "RETRY";
    public static final String SEEK = "SEEK";

    private JsonObject videoInfo;
    private JsonObject playStatisticsInfo;
    JsonArray cuePointArray;

    private long duration;
    private int startTime;
    private int currentTime;
    private int intervalTime = 10;
    private int section = 0;
    private static final int PERCENT = 10;

    boolean isRetry = false;
    boolean isInitPlay = true;
    boolean isStopped = false;
    boolean isPaused = false;

    private TimerTask mTask;
    private Timer mTimer;


    public VodStatistics(Context context){
        this.context = context;
        playsUrl = context.getResources().getString(R.string.videoPlaysUrl);
        sectionsUrl = context.getResources().getString(R.string.videoSectionUrl);
        cuePointUrl = context.getResources().getString(R.string.cuePointUrl);
    }

    public void setDuration(long duration){
        this.duration = duration;
    }

    public void setWecandeoSdk(WecandeoSdk wecandeoSdk){
        this.wecandeoSdk = wecandeoSdk;
    }

    public void fetchVideoDetail(String videoKey){
        String url = context.getResources().getString(R.string.videoInfoUrl) + videoKey;
        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                        JsonObject videoDetailJson =  jsonObject.get("VideoDetail").getAsJsonObject();
                        cuePointArray = videoDetailJson.get("CuePointList").getAsJsonArray();
                        fetchVideoStatistics(videoKey);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    private void fetchVideoStatistics(String videoKey){
        String url = context.getResources().getString(R.string.videoStatsInfoUrl) + videoKey;
        Log.d(TAG, "getVideoInfo() - url : " + url);

        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                        videoInfo = (JsonObject) jsonObject.get("statsInfo");
                        videoInfo.remove("errorInfo");
                        String ref = context.getResources().getString(R.string.httpsString) + context.getPackageName();
                        videoInfo.addProperty("ref", Base64.encodeToString(ref.getBytes(), 0));
                        videoInfo.addProperty("e", "pl");
                        videoInfo.addProperty("fv", "0.0.0");
                        for(int i = 0; i < cuePointArray.size(); i++){
                            cuePointArray.get(i).getAsJsonObject().addProperty("plid", videoInfo.get("plid").getAsInt());
                        }
                        playerLoadStatistics();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    private void playerLoadStatistics(){
        String url = context.getResources().getString(R.string.videoPlaysUrl) + videoInfo.toString();
        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "player load vodStatistics : " + url);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "requestVideoData is error!!! ");
            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    public void sendStatistics(String state){

        switch (state){
            case RETRY :
                isRetry = true;
                if(playStatisticsInfo == null || isStopped){
                    sendStatistics(PLAY);
                }else{
                    sendStatistics(STOP);
                }
                break;
            case PLAY :
                playStatisticsInfo = JsonParser.parseString(videoInfo.toString()).getAsJsonObject();
                if(isInitPlay || isStopped){
                    isInitPlay = false;
                    isStopped = false;
                    isPaused = false;
                    playStatisticsInfo.addProperty("e", "vs");
                    playStatisticsInfo.addProperty("dtt", duration);
                    playStatisticsInfo.remove("dst");
                    playStatisticsInfo.remove("det");
                    startTime = 0;
                    currentTime = 0;
                    String url = context.getResources().getString(R.string.videoPlaysUrl) + playStatisticsInfo.toString();
                    CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d(TAG, "success, urlData : " + url);
                                    timeUpdate(playStatisticsInfo);
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "sendStatistics is error!!! " + error.getLocalizedMessage());
                        }
                    });
                    RequestSingleton.getInstance(context).addToRequestQueue(request);
                }else{
                    isPaused = false;
                    startTime = currentTime;
                    timeUpdate(playStatisticsInfo);
                }
                break;
            case STOP :
                if(mTimer != null){
                    mTimer.cancel();
                    isStopped = true;
                    videoInfo.addProperty("e", "vt");
                    videoInfo.addProperty("dtt", duration);
                    videoInfo.addProperty("dst", startTime);
                    videoInfo.addProperty("det", currentTime);
                    sendLog(playsUrl, videoInfo);
                    reset();
                    mTimer = null;
                }
                break;
            case PAUSE :
                if(mTimer != null){
                    isPaused = true;
                    mTimer.cancel();
                    videoInfo.addProperty("e", "vp");
                    videoInfo.addProperty("dtt", duration);
                    videoInfo.addProperty("dst", startTime);
                    videoInfo.addProperty("det", currentTime);
                    sendLog(playsUrl, videoInfo);
                }
                break;
            case SEEK:
                if(mTimer != null && !isPaused){
                    videoInfo.remove("e");
                    videoInfo.addProperty("dtt", duration);
                    videoInfo.addProperty("dst", startTime);
                    videoInfo.addProperty("det", currentTime);
                    videoInfo.addProperty("e", "vk");
                    startTime = (int)TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition());
                    currentTime = (int)TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition());
                    sendLog(playsUrl, videoInfo);
                }
                break;
        }
    }

    // 큐 포인트 클릭 통계 이벤트
    public void cuePointClick(int cuePointId){
        for(int i = 0; i < cuePointArray.size(); i++){
            if(cuePointId == cuePointArray.get(i).getAsJsonObject().get("cue_point_id").getAsInt()){
                JsonObject ccObject = new JsonObject();
                ccObject.addProperty("vid", cuePointArray.get(i).getAsJsonObject().get("video_id").getAsInt());
                ccObject.addProperty("gid", cuePointArray.get(i).getAsJsonObject().get("gid").getAsInt());
                ccObject.addProperty("plid", cuePointArray.get(i).getAsJsonObject().get("plid").getAsInt());
                ccObject.addProperty("pid", cuePointArray.get(i).getAsJsonObject().get("package_id").getAsInt());
                ccObject.addProperty("cpid",cuePointArray.get(i).getAsJsonObject().get("cue_point_id").getAsInt());
                ccObject.addProperty("e", "cc");
                sendLog(cuePointUrl, ccObject);
            }
        }
    }

    private void timeUpdate(JsonObject jsonObject){
        mTask = new TimerTask() {
            @Override
            public void run() {
                    if(wecandeoSdk.getPlayer() != null){
                        JsonObject playsObject = JsonParser.parseString(jsonObject.toString()).getAsJsonObject();
                        if(TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition()) > duration){
                            currentTime = (int)duration;
                        }else{
                            currentTime = (int)TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition());
                        }
                        if(cuePointArray != null){
                            for(int i = 0; i < cuePointArray.size(); i++){
                                if(currentTime == cuePointArray.get(i).getAsJsonObject().get("start_duration").getAsInt()){
                                    JsonObject cvObject = new JsonObject();
                                    cvObject.addProperty("vid", cuePointArray.get(i).getAsJsonObject().get("video_id").getAsInt());
                                    cvObject.addProperty("gid", cuePointArray.get(i).getAsJsonObject().get("gid").getAsInt());
                                    cvObject.addProperty("plid", cuePointArray.get(i).getAsJsonObject().get("plid").getAsInt());
                                    cvObject.addProperty("pid", cuePointArray.get(i).getAsJsonObject().get("package_id").getAsInt());
                                    cvObject.addProperty("cpid", cuePointArray.get(i).getAsJsonObject().get("cue_point_id").getAsInt());
                                    cvObject.addProperty("e", "cv");
                                    sendLog(cuePointUrl, cvObject);
                                }
                            }
                        }
                        if(currentTime - startTime >= intervalTime){
                            playsObject.addProperty("e", "vi");
                            playsObject.addProperty("dtt", duration);
                            playsObject.addProperty("dst", startTime);
                            playsObject.addProperty("det", currentTime);
                            sendLog(playsUrl, playsObject);
                            startTime = currentTime;
                        }

                        if(currentTime < 60)
                            intervalTime = 5;
                        else
                            intervalTime = 10;

                        int percentByTime = (int)(((double)TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition()) / (double)duration) * 100);
                        int index = (percentByTime / PERCENT) + 1;
                        if(percentByTime >= 100)
                            index = 100 / PERCENT;
                        if(section != PERCENT * index){
                            JsonObject sectionInfo = JsonParser.parseString(jsonObject.toString()).getAsJsonObject();
                            sectionInfo.addProperty("dtt", duration);
                            section = PERCENT * index;
                            sectionInfo.addProperty("section", section);
                            try {
                                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                                String version = pInfo.versionName;
                                if(version.length() > 3){
                                    version = version.substring(0, 3);
                                }
                                sectionInfo.addProperty("ver", "WCD_SDK_" + version);
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                            sectionInfo.remove("dst");
                            sectionInfo.remove("det");
                            sectionInfo.remove("ref");
                            sectionInfo.remove("e");
                            sectionInfo.remove("fv");
                            sendLog(sectionsUrl, sectionInfo);
                        }
                    }
            }
        };
        mTimer = new Timer();
        mTimer.schedule(mTask, 100, 1000);
    }

    private void sendLog(String url, JsonObject resultObject){
        String urlData = url + resultObject.toString();
        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, urlData,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "success, urlData : " + urlData);
                        if(isRetry && isStopped){
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendStatistics(PLAY);
                                    isRetry = false;
                                }
                            }, 200);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "sendStatistics is error!!! ");
            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    private void reset(){
        startTime = 0;
        section = 0;
    }

    public void onDestroy(){
        if(mTimer != null && !isStopped){
            sendStatistics(STOP);
        }
    }

}
