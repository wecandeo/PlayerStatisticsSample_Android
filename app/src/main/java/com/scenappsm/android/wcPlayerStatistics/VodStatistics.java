package com.scenappsm.android.wcPlayerStatistics;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

public class VodStatistics {

    Context context;
    WecandeoSdk wecandeoSdk;

    private static final String TAG = "VodStatistics";
    public static final String PLAY = "PLAY";
    public static final String PAUSE = "PAUSE";
    public static final String STOP = "STOP";
    public static final String RETRY = "RETRY";
    public static final String SEEK = "SEEK";

    private JsonObject videoInfo;
    private JsonObject playStatisticsInfo;
    JsonArray cuePointArray;

    private long duration; // 플레이어 총 재생시간
    private int startTime; // 플레이어 시작 시간
    private int currentTime; // 플레이어 현재 시간
    private int intervalTime = 10;
    private int section = 0;
    private static final int PERCENT = 10;

    boolean isRetry = false; // 재시작 여부
    boolean isInitPlay = true; // 영상 처음 재생 여부
    boolean isStopped = false;
    boolean isPaused = false;

    private Handler mHandler;
    private Runnable mRunnable;

    private static final int DELAY_TIME = 100;
    private static final int PERIOD_TIME = 1000;


    public VodStatistics(Context context){
        this.context = context;
    }

    public void setDuration(long duration){
        this.duration = duration;
    }

    public void setWecandeoSdk(WecandeoSdk wecandeoSdk){
        this.wecandeoSdk = wecandeoSdk;
    }

    // 영상 상세정보 조회
    public void fetchVideoDetail(String videoKey){
        String url = StatisticsUrlInfo.VIDEO_INFO_URL + videoKey;
        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                        JsonObject videoDetailJson =  jsonObject.get("VideoDetail").getAsJsonObject();
                        cuePointArray = videoDetailJson.get("CuePointList").getAsJsonArray();
                        fetchVideoStatsInfo(videoKey);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "fetchVideoDetail() is error : " + error.getLocalizedMessage());
            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    // 영상 통계정보 조회
    private void fetchVideoStatsInfo(String videoKey){
        String url = StatisticsUrlInfo.VIDEO_STATS_INFO_URL + videoKey;

        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                        videoInfo = (JsonObject) jsonObject.get("statsInfo");
                        videoInfo.remove("errorInfo");
                        String ref = "https://" + context.getPackageName();
                        videoInfo.addProperty("ref", Base64.encodeToString(ref.getBytes(), Base64.NO_WRAP));
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
                Log.d(TAG, "fetchVideoStatsInfo() is error : " + error.getLocalizedMessage());
            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    // 플레이어 로드 통계 전송
    private void playerLoadStatistics(){
        String keyword = "";
        try{
            keyword = URLEncoder.encode(videoInfo.toString(), "UTF-8");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        String url = StatisticsUrlInfo.VIDEO_PLAYS_URL + keyword;
        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "player load vodStatistics : " + url);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "playerLoadStatistics() is error : " + error.getLocalizedMessage());
            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    public void sendStatistics(String state){

        switch (state){
            case RETRY :
                if(playStatisticsInfo == null || isStopped){
                    isRetry = false;
                    sendStatistics(PLAY);
                }else{
                    isRetry = true;
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
                    String keyword = "";
                    try{
                        keyword = URLEncoder.encode(playStatisticsInfo.toString(), "UTF-8");
                    }catch (UnsupportedEncodingException e){
                        e.printStackTrace();
                    }
                    String url = StatisticsUrlInfo.VIDEO_PLAYS_URL + keyword;
                    CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d(TAG, "success, data : " + url);
                                    timeUpdate(playStatisticsInfo);
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "sendStatistics() is error : " + error.getLocalizedMessage());
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
                if(mHandler != null && !isStopped){
                    isStopped = true;
                    mHandler.removeCallbacks(mRunnable);
                    videoInfo.addProperty("e", "vt");
                    videoInfo.addProperty("dtt", duration);
                    videoInfo.addProperty("dst", startTime);
                    videoInfo.addProperty("det", currentTime);
                    sendLog(StatisticsUrlInfo.VIDEO_PLAYS_URL, videoInfo);
                    reset();
                }
                break;
            case PAUSE :
                if(mHandler != null && !isStopped){
                    isPaused = true;
                    mHandler.removeCallbacks(mRunnable);
                    videoInfo.addProperty("e", "vp");
                    videoInfo.addProperty("dtt", duration);
                    videoInfo.addProperty("dst", startTime);
                    videoInfo.addProperty("det", currentTime);
                    sendLog(StatisticsUrlInfo.VIDEO_PLAYS_URL, videoInfo);
                }
                break;
            case SEEK:
                if(mHandler != null && !isPaused){
                    videoInfo.remove("e");
                    videoInfo.addProperty("dtt", duration);
                    videoInfo.addProperty("dst", startTime);
                    videoInfo.addProperty("det", currentTime);
                    videoInfo.addProperty("e", "vk");
                    startTime = (int)TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition());
                    currentTime = (int)TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition());
                    sendLog(StatisticsUrlInfo.VIDEO_PLAYS_URL, videoInfo);
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
                sendLog(StatisticsUrlInfo.CUE_POINT_URL, ccObject);
            }
        }
    }

    private void timeUpdate(JsonObject jsonObject){
        mHandler = new Handler();
        mRunnable = new Runnable() {
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
                                sendLog(StatisticsUrlInfo.CUE_POINT_URL, cvObject);
                            }
                        }
                    }
                    if(currentTime - startTime >= intervalTime){
                        playsObject.addProperty("e", "vi");
                        playsObject.addProperty("dtt", duration);
                        playsObject.addProperty("dst", startTime);
                        playsObject.addProperty("det", currentTime);
                        sendLog(StatisticsUrlInfo.VIDEO_PLAYS_URL, playsObject);
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
                        sendLog(StatisticsUrlInfo.SECTIONS_URL, sectionInfo);
                    }
                }
                mHandler.postDelayed(this, PERIOD_TIME);
            }
        };
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, DELAY_TIME);
    }

    private void sendLog(String url, JsonObject resultObject){
        String keyword = "";
        try{
            keyword = URLEncoder.encode(resultObject.toString(), "UTF-8");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        String urlData = url + keyword;
        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, urlData,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "success, data : " + urlData);
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
                Log.d(TAG, "sendLog() is error : " + error.getLocalizedMessage());
            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    private void reset(){
        startTime = 0;
        section = 0;
    }

    public void onDestroy(){
        if(context != null && mHandler != null && !isStopped){
            sendStatistics(STOP);
        }
    }

}
