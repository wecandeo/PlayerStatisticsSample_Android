package com.scenappsm.android.wcPlayerStatistics;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Timer;
import java.util.TimerTask;

public class LiveStatistics {

    Context context;
    private TimerTask mTask;
    private Timer mTimer;

    private static final String TAG = "LiveStatistics";
    public static final String PLAY = "PLAY";
    public static final String STOP = "STOP";

    private JsonObject liveInfo;
    String videoKey;
    boolean isPlaying = false;

    private static final int DELAY_TIME = 500;
    private static final int INTERVAL_TIME = 5000;

    public LiveStatistics(Context context, String key){
        this.context = context;
        videoKey = key;
    }

    public void sendStatistics(String state){
        switch (state){
            case PLAY :
                fetchLiveStatsInfo(videoKey);
                break;
            case STOP :
                if(context != null && mTimer != null){
                    mTimer.cancel();
                    mTimer = null;
                    isPlaying = false;
                    liveInfo.addProperty("e", "lt");
                    JsonObject endJson = JsonParser.parseString(liveInfo.toString()).getAsJsonObject();
                    sendLog(endJson);
                }
        }
    }

    // 라이브 통계정보 조회
    private void fetchLiveStatsInfo(String key){
        isPlaying = true;
        String url = StatisticsUrlInfo.LIVE_STATS_INFO_URL + key;
        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                        liveInfo = (JsonObject) jsonObject.get("statsInfo");
                        liveInfo.remove("errorInfo");
                        liveInfo.addProperty("fv", "0.0.0");
                        liveInfo.addProperty("e", "ls");
                        String ref = "https://" + context.getPackageName();
                        liveInfo.addProperty("ref", Base64.encodeToString(ref.getBytes(), Base64.NO_WRAP));
                        JsonObject startJson = JsonParser.parseString(liveInfo.toString()).getAsJsonObject();
                        sendLog(startJson);
                        timeUpdate();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "error : " + error);
            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }

    private void timeUpdate(){
        mTask = new TimerTask() {
            @Override
            public void run() {
                liveInfo.addProperty("e", "li");
                sendLog(liveInfo);
            }
        };
        mTimer = new Timer();
        mTimer.schedule(mTask, DELAY_TIME, INTERVAL_TIME);
    }

    private void sendLog(JsonObject jsonObject){
        String url = StatisticsUrlInfo.LIVE_LOG_URL + jsonObject.toString();
        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "success : " + url);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "error : " + error);
            }
        });
        RequestSingleton.getInstance(context).addToRequestQueue(request);
    }



}
