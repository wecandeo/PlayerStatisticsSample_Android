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
import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.scenappsm.wecandeosdkplayer.SdkInterface;
import com.scenappsm.wecandeosdkplayer.WecandeoSdk;
import com.scenappsm.wecandeosdkplayer.WecandeoVideo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener, Player.EventListener, SdkInterface.onSdkListener{
    WecandeoSdk wecandeoSdk;
    WecandeoVideo wecandeoVideo;
    VodStatistics vodStatistics; // 통계 연동 객체

    private static final String TAG = "PlayerActivity";
    boolean isInitVideoInfo = true; // 플레이어가 준비 완료되면 true 이후 false (최초 한번만 실행)
    boolean isPlaying = false; // 재생 시작 여부

    // 풀스크린 여부
    private boolean isFullscreen = false;

    ConstraintLayout mainLayout;
    ConstraintLayout playerParent;
    PlayerView playerView;

    Button retryButton;
    Button stopButton;
    Button playButton;
    Button pauseButton;
    Button muteButton;
    Button volumeUpButton;
    Button volumeDownButton;
    Button rewindButton;
    Button forwardButton;
    Button fullScreenButton;
    Spinner resizeSpinner;
    TextView actionText;
    TextView debugText;

    List<Button> buttonList = new ArrayList<>();

    boolean isDrm = false; // DRM 여부
    String videoKey; // 영상의 videoKey 값
    String gId; // DRM 영상인 경우에만 사용, 발급받은 gId 값
    String packageId; // DRM 영상인 경우에만 사용, 발급받은 packageId 값
    String videoId; // DRM 영상인 경우에만 사용, 발급받은 videoId 값
    String secretKey; // DRM 영상인 경우에만 사용, 발급받은 secretKey 값


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        initViews();
        if(videoKey != null && !videoKey.equals("")){
            initWecandeoSetting();
        }
    }

    private void initViews(){
        mainLayout = findViewById(R.id.main_layout);
        playerParent = findViewById(R.id.player_parent);
        playerView = findViewById(R.id.player_view);
        retryButton = findViewById(R.id.retry_button);
        retryButton.setOnClickListener(this);
        stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(this);
        playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(this);
        pauseButton = findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(this);
        muteButton = findViewById(R.id.mute_button);
        muteButton.setOnClickListener(this);
        volumeUpButton = findViewById(R.id.volume_up_button);
        volumeUpButton.setOnClickListener(this);
        volumeDownButton = findViewById(R.id.volume_down_button);
        volumeDownButton.setOnClickListener(this);
        rewindButton = findViewById(R.id.rewind_button);
        rewindButton.setOnClickListener(this);
        forwardButton = findViewById(R.id.forward_button);
        forwardButton.setOnClickListener(this);
        fullScreenButton = findViewById(R.id.full_screen_button);
        fullScreenButton.setOnClickListener(this);
        actionText = findViewById(R.id.action_text);
        debugText = findViewById(R.id.debug_text);
        buttonList.add(stopButton);
        buttonList.add(playButton);
        buttonList.add(pauseButton);
        resizeSpinner = findViewById(R.id.resize_spinner);
        ArrayAdapter<CharSequence> resizeSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.resize_array, android.R.layout.simple_spinner_item);
        resizeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resizeSpinner.setAdapter(resizeSpinnerAdapter);
        resizeSpinner.setOnItemSelectedListener(this);
        actionText = findViewById(R.id.action_text);
        debugText = findViewById(R.id.debug_text);
        resizeSpinner.bringToFront();
        playerView.setOnTouchListener(new View.OnTouchListener() {
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
        wecandeoVideo.setDrm(isDrm);
        if(isDrm){
            // DRM 영상인 경우, 발급된 videoId, videoKey, gId, scretKey, packageId 셋팅 이후, 통계 연동 객체 생성
            wecandeoVideo.setVideoKey(videoKey);
            wecandeoVideo.setgId(gId);
            wecandeoVideo.setPackageId(packageId);
            wecandeoVideo.setVideoId(videoId);
            wecandeoVideo.setSecretKey(secretKey);
            wecandeoSdk.setWecandeoVideo(wecandeoVideo);
            wecandeoSdk.setPlayerView(playerView);
            wecandeoSdk.setDebugTextView(debugText);
            //기본 컨트롤 뷰사용
            wecandeoSdk.setUseController(false);
            // 통계 연동
            vodStatistics = new VodStatistics(this);
        }else{
            /*
             * DRM 영상이 아닌 경우, 발급된 videoKey 값을 이용하여 영상 상세정보를 조회한 뒤,
             * videoUrl 값을 가져와서 Player 를 구성합니다.
             * Player 구성 이후, 통계 연동 객체를 생성합니다.
             * */
            String url = StatisticsUrlInfo.VIDEO_INFO_URL + videoKey;
            CustomStringRequest request = new CustomStringRequest(getApplicationContext(), Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                            JsonObject videoDetailObject = jsonObject.get("VideoDetail").getAsJsonObject();
                            String videoUrl = videoDetailObject.get("videoUrl").getAsString();
                            wecandeoVideo.setVideoKey(videoUrl);
                            wecandeoSdk.setWecandeoVideo(wecandeoVideo);
                            wecandeoSdk.setPlayerView(playerView);
                            wecandeoSdk.setDebugTextView(debugText);
                            //기본 컨트롤 뷰사용
                            wecandeoSdk.setUseController(false);
                            // 통계 연동
                            vodStatistics = new VodStatistics(getApplicationContext());
                            wecandeoSdk.onStart();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "initWecandeoSetting() is error : " + error.getLocalizedMessage());
                }
            });
            RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        if(wecandeoSdk != null)
            wecandeoSdk.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(wecandeoSdk != null)
            wecandeoSdk.onResume();
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
    public void onDestroy(){
        if(vodStatistics != null)
            vodStatistics.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed(){
        if(isFullscreen){
            closeFullscreenDialog();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View view){
        if(view instanceof Button){
            String name = ((Button)view).getText().toString();
            actionText.setText(getResources().getString(R.string.actionText) + name);
        }
        switch (view.getId()){
            case R.id.retry_button :
                disableButton(retryButton);
                wecandeoSdk.retry();
                vodStatistics.sendStatistics(VodStatistics.RETRY);
                isPlaying = true;
                break;
            case R.id.stop_button :
                disableButton(stopButton);
                vodStatistics.sendStatistics(VodStatistics.STOP);
                wecandeoSdk.stop();
                isPlaying = false;
                break;
            case R.id.play_button :
                disableButton(playButton);
                wecandeoSdk.play();
                vodStatistics.sendStatistics(VodStatistics.PLAY);
                isPlaying = true;
                break;
            case R.id.pause_button :
                disableButton(pauseButton);
                wecandeoSdk.pause();
                vodStatistics.sendStatistics(VodStatistics.PAUSE);
                break;
            case R.id.mute_button :
                wecandeoSdk.setMute(wecandeoSdk.isMute() ? false : true);
                break;
            case R.id.volume_up_button :
                wecandeoSdk.volumePlus();
                break;
            case R.id.volume_down_button :
                wecandeoSdk.volumeMinus();
                break;
            case R.id.rewind_button :
                if(isPlaying){
                    wecandeoSdk.rewind();
                    vodStatistics.sendStatistics(VodStatistics.SEEK);
                }
                break;
            case R.id.forward_button :
                if(isPlaying){
                    wecandeoSdk.fastForward();
                    vodStatistics.sendStatistics(VodStatistics.SEEK);
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

    private void disableButton(Button button){
        if(button == retryButton){
            for(Button item : buttonList){
                if(item == playButton)
                    item.setEnabled(false);
                else
                    item.setEnabled(true);
            }
        }else{
            for(Button item : buttonList){
                if(item == button)
                    item.setEnabled(false);
                else
                    item.setEnabled(true);
            }
        }
    }

    private void openFullscreenDialog(){
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        playerParent.setLayoutParams(params);
        isFullscreen = true;
    }

    private void closeFullscreenDialog(){
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        playerParent.setLayoutParams(params);
        isFullscreen = false;
    }

    @Override
    public void onSdkError(SimpleExoPlayer simpleExoPlayer, int i) {}

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}


    @Override
    public void onPlaybackStateChanged(int playbackState) {
        // 영상이 로드되고 준비가 되면 통계 연동 셋팅
        if(playbackState == Player.STATE_READY && wecandeoSdk.getPlayer() != null && isInitVideoInfo){
            isInitVideoInfo = false;
            vodStatistics.setWecandeoSdk(wecandeoSdk);
            vodStatistics.setDuration(TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getDuration()));
            vodStatistics.fetchVideoDetail(videoKey);
        }

        // 영상이 완료되었을 때
        if(playbackState == Player.STATE_ENDED && wecandeoSdk.getPlayer() != null){
            wecandeoSdk.complete();
            vodStatistics.sendStatistics(VodStatistics.STOP);
            if(!playButton.isEnabled()){
                playButton.setEnabled(true);
            }
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {}

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}
}

