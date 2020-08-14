package com.scenappsm.android.wcPlayerStatistics;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.scenappsm.wecandeosdkplayer.WecandeoSdk;
import com.scenappsm.wecandeosdkplayer.WecandeoVideo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Statistics {

    Context context;
    WecandeoSdk wecandeoSdk;

    private String sectionsUrl;
    private String playsUrl;
    private String cuePointUrl;

    private static final String TAG = "Statistics";
    public static final String PLAY = "PLAY";
    public static final String PAUSE = "PAUSE";
    public static final String STOP = "STOP";
    public static final String RETRY = "RETRY";
    public static final String SEEK = "SEEK";

    private JSONObject videoInfo;
    private JSONObject playStatisticsInfo;

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

    JSONArray cuePointArray;


    public Statistics(Context context){
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

    public void sendCuePointStatistics(){
        String url = context.getResources().getString(R.string.videoInfoUrl) + context.getResources().getString(R.string.videoKey);
        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONObject jsonObject = new JSONObject(response);
                            JSONObject videoDetailJson =  jsonObject.getJSONObject("VideoDetail");
                            cuePointArray = videoDetailJson.getJSONArray("CuePointList");
                            getVideoStatsInfo();

                        }catch(JSONException e){
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    public void getVideoStatsInfo(){
        String url = context.getResources().getString(R.string.videoStatsInfoUrl) + context.getResources().getString(R.string.videoKey);
        Log.d(TAG, "getVideoInfo() - url : " + url);

        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONObject jsonObject = new JSONObject(response);
                            videoInfo = (JSONObject) jsonObject.get("statsInfo");
                            videoInfo.remove("errorInfo");
                            videoInfo.put("ref", context.getResources().getString(R.string.httpsString) + context.getPackageName());
                            videoInfo.put("e", "pl");
                            videoInfo.put("fv", "0.0.0");
                            for(int i = 0; i < cuePointArray.length(); i++){
                                cuePointArray.getJSONObject(i).put("plid", videoInfo.getInt("plid"));
                            }
                            playerLoadStatistics();
                        }catch(JSONException e){
                            e.printStackTrace();
                        }
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
                        Log.d(TAG, "player load statistics : " + url);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "requestVideoData is error!!! ");
            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    public void sendStatistics(String STATE){

        switch (STATE){
            case RETRY :
                isRetry = true;
                if(playStatisticsInfo == null || isStopped){
                    sendStatistics(PLAY);
                }else{
                    sendStatistics(STOP);
                }
                break;
            case PLAY :
                try{
                    playStatisticsInfo = new JSONObject(videoInfo.toString());
                    if(isInitPlay || isStopped){
                        isInitPlay = false;
                        isStopped = false;
                        isPaused = false;
                        playStatisticsInfo.put("e", "vs");
                        playStatisticsInfo.put("dtt", duration);
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
                }catch(JSONException e){
                    e.printStackTrace();
                }
                break;
            case STOP :
                try{
                    if(mTimer != null){
                        mTimer.cancel();
                        isStopped = true;
                        videoInfo.put("e", "vt");
                        videoInfo.put("dtt", duration);
                        videoInfo.put("dst", startTime);
                        videoInfo.put("det", currentTime);
                        sendLog(playsUrl, videoInfo);
                        reset();
                        mTimer = null;
                    }
                }catch(JSONException e){
                    e.printStackTrace();
                }
                break;
            case PAUSE :
                try{
                    if(mTimer != null){
                        isPaused = true;
                        mTimer.cancel();
                        videoInfo.put("e", "vp");
                        videoInfo.put("dtt", duration);
                        videoInfo.put("dst", startTime);
                        videoInfo.put("det", currentTime);
                        sendLog(playsUrl, videoInfo);
                    }
                }catch(JSONException e){
                    e.printStackTrace();
                }
                break;
            case SEEK:
                  if(mTimer != null && !isPaused){
                      try{
                          videoInfo.remove("e");
                          videoInfo.put("dtt", duration);
                          videoInfo.put("dst", startTime);
                          videoInfo.put("det", currentTime);
                          videoInfo.put("e", "vk");
                          startTime = (int)TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition());
                          currentTime = (int)TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition());
                          sendLog(playsUrl, videoInfo);
                      }catch(JSONException e){
                          e.printStackTrace();
                      }
                  }
                break;
        }
    }

    // 큐 포인트 클릭 통계 이벤트
    public void cuePointClick(int cuePointId){
        try{
            for(int i = 0; i < cuePointArray.length(); i++){
                if(cuePointId == cuePointArray.getJSONObject(i).getInt("cue_point_id")){
                    JSONObject ccObject = new JSONObject();
                    ccObject.put("vid", cuePointArray.getJSONObject(i).getInt("video_id"));
                    ccObject.put("gid", cuePointArray.getJSONObject(i).getInt("gid"));
                    ccObject.put("plid", cuePointArray.getJSONObject(i).getInt("plid"));
                    ccObject.put("pid", cuePointArray.getJSONObject(i).getInt("package_id"));
                    ccObject.put("cpid", cuePointArray.getJSONObject(i).getInt("cue_point_id"));
                    ccObject.put("e", "cc");
                    sendLog(cuePointUrl, ccObject);
                }
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void timeUpdate(JSONObject jsonObject){
        mTask = new TimerTask() {
            @Override
            public void run() {
                try{
                    if(wecandeoSdk.getPlayer() != null){
                        JSONObject playsObject = new JSONObject(jsonObject.toString());
                        if(TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition()) > duration){
                            currentTime = (int)duration;
                        }else{
                            currentTime = (int)TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getCurrentPosition());
                        }
                        if(cuePointArray != null){
                            for(int i = 0; i < cuePointArray.length(); i++){
                                if(currentTime == cuePointArray.getJSONObject(i).getInt("start_duration")){
                                    JSONObject cvObject = new JSONObject();
                                    cvObject.put("vid", cuePointArray.getJSONObject(i).getInt("video_id"));
                                    cvObject.put("gid", cuePointArray.getJSONObject(i).getInt("gid"));
                                    cvObject.put("plid", cuePointArray.getJSONObject(i).getInt("plid"));
                                    cvObject.put("pid", cuePointArray.getJSONObject(i).getInt("package_id"));
                                    cvObject.put("cpid", cuePointArray.getJSONObject(i).getInt("cue_point_id"));
                                    cvObject.put("e", "cv");
                                    sendLog(cuePointUrl, cvObject);
                                }
                            }
                        }
                        if(currentTime - startTime >= intervalTime){
                            playsObject.put("e", "vi");
                            playsObject.put("dtt", duration);
                            playsObject.put("dst", startTime);
                            playsObject.put("det", currentTime);
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
                            JSONObject sectionInfo = new JSONObject(jsonObject.toString());
                            sectionInfo.put("dtt", duration);
                            section = PERCENT * index;
                            sectionInfo.put("section", section);
                            try {
                                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                                String version = pInfo.versionName;
                                if(version.length() > 3){
                                    version = version.substring(0, 3);
                                }
                                sectionInfo.put("ver", "WCD_SDK_" + version);
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
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
        };
        mTimer = new Timer();
        mTimer.schedule(mTask, 100, 1000);
    }

    private void sendLog(String url, JSONObject resultObject){
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
