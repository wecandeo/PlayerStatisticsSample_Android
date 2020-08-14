package com.scenappsm.android.wcPlayerStatistics;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.scenappsm.wecandeosdkplayer.SdkInterface;
import com.scenappsm.wecandeosdkplayer.WecandeoSdk;
import com.scenappsm.wecandeosdkplayer.WecandeoVideo;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class LiveActivity extends AppCompatActivity implements View.OnClickListener, ExoPlayer.EventListener, SdkInterface.onSdkListener{

    ConstraintLayout mainLayout;
    ConstraintLayout liveParent;
    SimpleExoPlayerView simpleExoPlayerView;

    Button stopButton;
    Button playButton;

    WecandeoSdk wecandeoSdk;
    WecandeoVideo wecandeoVideo;

    LiveStatistics liveStatistics;

    // 풀스크린 여부
    private boolean isFullscreen = false;

    private static final String TAG = "LiveActivity";
    private static final int PLAY_ENABLE = 3; // 즉시 플레이 가능
    private static final int PLAY_COMPLETE = 4; // 플레이 완료
    boolean isInitVideoInfo = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        initViews();
        initWecanedoSetting();
    }

    private void initViews(){
        mainLayout = findViewById(R.id.main_layout);
        liveParent = findViewById(R.id.player_parent);
        simpleExoPlayerView = findViewById(R.id.player_view);
        stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(this);
        playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(this);
    }

    private void initWecanedoSetting(){
        wecandeoSdk = new WecandeoSdk(this);
        wecandeoSdk.setSdkListener(this);
        wecandeoSdk.addPlayerListener(this);
        wecandeoVideo = new WecandeoVideo();
        wecandeoVideo.setDrm(false);
        initVideoInfo();
    }

    private void initVideoInfo(){
        String url = getResources().getString(R.string.videoInfoUrl) + getResources().getString(R.string.videoKey);
        CustomStringRequest request = new CustomStringRequest(getApplicationContext(), Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                        JsonObject videoDetailObject = jsonObject.get("VideoDetail").getAsJsonObject();
                        String videoKey = videoDetailObject.get("videoUrl").getAsString();
                        wecandeoVideo.setVideoKey(videoKey);
                        wecandeoSdk.setWecandeoVideo(wecandeoVideo);
                        wecandeoSdk.setSimpleExoPlayerView(simpleExoPlayerView);
                        //기본 컨트롤 뷰사용
                        wecandeoSdk.setUseController(true);
                        // 통계 연동
                        liveStatistics = new LiveStatistics(getApplicationContext());
                        wecandeoSdk.onStart();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){

        }
    }

    private void openFullscreenDialog(){
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        liveParent.setLayoutParams(params);
        isFullscreen = true;
    }

    private void closeFullscreenDialog(){
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        liveParent.setLayoutParams(params);
        isFullscreen = false;
    }

    @Override
    public void onPause(){
        super.onPause();
        wecandeoSdk.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        wecandeoSdk.onStop();
    }

    @Override
    public void onSdkError(SimpleExoPlayer simpleExoPlayer, int i) {}

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}

    @Override
    public void onLoadingChanged(boolean isLoading) {}


    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if(playbackState == PLAY_ENABLE && wecandeoSdk.getPlayer() != null && isInitVideoInfo){
            isInitVideoInfo = false;

        }

        // 영상이 완료되었을 때
        if(playWhenReady && playbackState == PLAY_COMPLETE && wecandeoSdk.getPlayer() != null){

        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {}

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {}

    @Override
    public void onPositionDiscontinuity() {}
}
