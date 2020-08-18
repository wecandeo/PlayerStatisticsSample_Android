package com.scenappsm.android.wcPlayerStatistics;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.scenappsm.wecandeosdkplayer.WecandeoSdk;

import java.sql.Time;
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
                if(mTimer != null){
                    mTimer.cancel();
                    mTimer = null;
                    liveInfo.addProperty("e", "lt");
                    JsonObject endJson = JsonParser.parseString(liveInfo.toString()).getAsJsonObject();
                    sendLog(endJson);
                }
        }
    }

    private void fetchLiveStatsInfo(String key){
        String url = StatisticsUrlInfo.LIVE_STATS_INFO_URL + key;
        CustomStringRequest request = new CustomStringRequest(context, Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                        liveInfo = (JsonObject) jsonObject.get("statsInfo");
                        liveInfo.remove("errorInfo");
                        liveInfo.addProperty("e", "ls");
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
        mTimer.schedule(mTask, 500, 5000);
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
