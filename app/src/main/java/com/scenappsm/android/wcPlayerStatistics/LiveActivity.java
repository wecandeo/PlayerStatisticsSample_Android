package com.scenappsm.android.wcPlayerStatistics;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class LiveActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener, ExoPlayer.EventListener, SdkInterface.onSdkListener{

    ConstraintLayout mainLayout;
    ConstraintLayout liveParent;
    SimpleExoPlayerView simpleExoPlayerView;

    Button stopButton;
    Button playButton;
    Button fullscreenButton;
    Spinner resizeSpinner;

    WecandeoSdk wecandeoSdk;
    WecandeoVideo wecandeoVideo;

    LiveStatistics liveStatistics;

    private boolean isFullscreen = false; // 풀스크린 여부
    private boolean isPlayEnabled = false; // Live 재생 가능 여부

    private static final String TAG = "LiveActivity";
    private static final int PLAY_COMPLETE = 4; // 플레이 완료

    String liveKey; // 발급 받은 liveKey 값을 입력

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        initViews();
        if(liveKey != null && !liveKey.equals("")){
            initWecandeoSetting();
        }
    }

    @Override
    public void onDestroy(){
        if(liveStatistics != null)
            liveStatistics.sendStatistics(LiveStatistics.STOP);
        super.onDestroy();
    }

    private void initViews(){
        mainLayout = findViewById(R.id.main_layout);
        liveParent = findViewById(R.id.live_parent);
        simpleExoPlayerView = findViewById(R.id.live_view);
        stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(this);
        playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(this);
        fullscreenButton =findViewById(R.id.full_screen_button);
        fullscreenButton.setOnClickListener(this);
        resizeSpinner = findViewById(R.id.resize_spinner);
        ArrayAdapter<CharSequence> resizeSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.resize_array, android.R.layout.simple_spinner_item);
        resizeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resizeSpinner.setAdapter(resizeSpinnerAdapter);
        resizeSpinner.setOnItemSelectedListener(this);
        resizeSpinner.bringToFront();
        simpleExoPlayerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(resizeSpinner.getVisibility() == View.VISIBLE){
                    resizeSpinner.setVisibility(View.INVISIBLE);
                }else{
                    resizeSpinner.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });
    }

    private void initWecandeoSetting(){
        wecandeoSdk = new WecandeoSdk(this);
        wecandeoSdk.setSdkListener(this);
        wecandeoSdk.addPlayerListener(this);
        wecandeoVideo = new WecandeoVideo();
        initVideoInfo();
    }

    // videoKey 를 이용하여 영상 상세정보 조회하여 videoUrl 로 Player 구성
    private void initVideoInfo(){
        String url = StatisticsUrlInfo.LIVE_INFO_URL + liveKey;
        CustomStringRequest request = new CustomStringRequest(getApplicationContext(), Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                        JsonObject videoDetailObject = jsonObject.get("VideoDetail").getAsJsonObject();
                        JsonObject errorInfo = videoDetailObject.get("errorInfo").getAsJsonObject();
                        if(!errorInfo.get("errorCode").getAsString().equals("None")){
                            isPlayEnabled = false;
                            ViewGroup.LayoutParams params = simpleExoPlayerView.getLayoutParams();
                            params.height = 800;
                            simpleExoPlayerView.setLayoutParams(params);
                            Toast.makeText(getApplicationContext(),"아직 방송시간이 아닙니다.",Toast.LENGTH_LONG).show();
                        }else{
                            // 현재 LIVE 방송 중일 경우 video setting
                            isPlayEnabled = true;
                            String videoKey = videoDetailObject.get("videoUrl").getAsString();
                            wecandeoVideo.setDrm(false);
                            wecandeoVideo.setVideoKey(videoKey);
                            wecandeoSdk.setWecandeoVideo(wecandeoVideo);
                            wecandeoSdk.setSimpleExoPlayerView(simpleExoPlayerView);
                            //기본 컨트롤 뷰사용
                            wecandeoSdk.setUseController(false);
                            // 통계 연동
                            liveStatistics = new LiveStatistics(getApplicationContext(), liveKey);
                            wecandeoSdk.onStart();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "initVideoInfo() is error : " + error.getLocalizedMessage());
            }
        });
        RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.play_button :
                if(isPlayEnabled){
                    wecandeoSdk.play();
                    liveStatistics.sendStatistics(LiveStatistics.PLAY);
                }
                break;
            case R.id.stop_button :
                if(isPlayEnabled){
                    wecandeoSdk.stop();
                    liveStatistics.sendStatistics(LiveStatistics.STOP);
                }
                break;
            case R.id.full_screen_button :
                if(isFullscreen)
                    closeFullscreenDialog();
                else
                    openFullscreenDialog();
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(wecandeoSdk != null)
            wecandeoSdk.setResizeMode(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onBackPressed(){
        if(isFullscreen){
            closeFullscreenDialog();
        }else{
            super.onBackPressed();
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
        if(wecandeoSdk != null)
            wecandeoSdk.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(wecandeoSdk != null)
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

        // 영상이 완료되었을 때
        if(playWhenReady && playbackState == PLAY_COMPLETE && wecandeoSdk.getPlayer() != null){
            wecandeoSdk.complete();
            liveStatistics.sendStatistics(LiveStatistics.STOP);
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
