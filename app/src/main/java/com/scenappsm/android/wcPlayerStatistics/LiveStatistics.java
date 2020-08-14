package com.scenappsm.android.wcPlayerStatistics;

import android.content.Context;

import com.scenappsm.wecandeosdkplayer.WecandeoSdk;

public class LiveStatistics {

    Context context;
    WecandeoSdk wecandeoSdk;

    private String infoUrl;
    private String statsInfoUrl;
    private String logUrl;

    public LiveStatistics(Context context){
        this.context = context;
        infoUrl = context.getResources().getString(R.string.liveInfoUrl);
        statsInfoUrl = context.getResources().getString(R.string.liveStatsInfoUrl);
        logUrl = context.getResources().getString(R.string.liveLogUrl);
    }

}
