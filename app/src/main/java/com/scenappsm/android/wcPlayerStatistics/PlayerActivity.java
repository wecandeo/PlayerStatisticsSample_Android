package com.scenappsm.android.wcPlayerStatistics;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.scenappsm.wecandeosdkplayer.SdkInterface;
import com.scenappsm.wecandeosdkplayer.WecandeoSdk;
import com.scenappsm.wecandeosdkplayer.WecandeoVideo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener, ExoPlayer.EventListener, SdkInterface.onSdkListener{
    WecandeoSdk wecandeoSdk;
    WecandeoVideo wecandeoVideo;
    boolean isDrm = true;

    Statistics statistics;

    private static final String TAG = "PlayerActivity";
    private static final int PLAY_ENABLE = 3; // 즉시 플레이 가능
    private static final int PLAY_COMPLETE = 4; // 플레이 완료
    boolean isInitVideoInfo = true;

    boolean isPlaying = false;

    // 풀스크린 여부
    private boolean isFullscreen = false;

    ConstraintLayout mainLayout;
    ConstraintLayout playerParent;
    SimpleExoPlayerView simpleExoPlayerView;

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
    TextView actionText;
    TextView debugText;

    List<Button> buttonList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        initViews();
        initWecandeoSetting();
    }

    private void initViews(){
        mainLayout = findViewById(R.id.main_layout);
        playerParent = findViewById(R.id.player_parent);
        simpleExoPlayerView = findViewById(R.id.player_view);
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
    }

    private void initWecandeoSetting(){

        wecandeoSdk = new WecandeoSdk(this);
        wecandeoSdk.setSdkListener(this);
        wecandeoSdk.addPlayerListener(this);

        wecandeoVideo = new WecandeoVideo();
        wecandeoVideo.setVideoKey(getResources().getString(R.string.videoKey));
        wecandeoVideo.setgId(getResources().getString(R.string.gId));
        wecandeoVideo.setPackageId(getResources().getString(R.string.packageId));
        wecandeoVideo.setVideoId(getResources().getString(R.string.videoId));
        wecandeoVideo.setSecretKey(getResources().getString(R.string.secretKey));
        wecandeoVideo.setDrm(isDrm);
        wecandeoSdk.setWecandeoVideo(wecandeoVideo);
        wecandeoSdk.setSimpleExoPlayerView(simpleExoPlayerView);
        wecandeoSdk.setDebugTextView(debugText);

        //기본 컨트롤 뷰사용
        wecandeoSdk.setUseController(true);

        // 통계 연동
        statistics = new Statistics(this);
    }

    @Override
    public void onStart(){
        super.onStart();
        wecandeoSdk.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        wecandeoSdk.onResume();
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
    public void onDestroy(){
        statistics.onDestroy();
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
                statistics.sendStatistics(Statistics.RETRY);
                isPlaying = true;
                break;
            case R.id.stop_button :
                disableButton(stopButton);
                wecandeoSdk.stop();
                statistics.sendStatistics(Statistics.STOP);
                isPlaying = false;
                break;
            case R.id.play_button :
                disableButton(playButton);
                wecandeoSdk.play();
                statistics.sendStatistics(Statistics.PLAY);
                isPlaying = true;
                break;
            case R.id.pause_button :
                disableButton(pauseButton);
                wecandeoSdk.pause();
                statistics.sendStatistics(Statistics.PAUSE);
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
                    statistics.sendStatistics(Statistics.SEEK);
                }
                break;
            case R.id.forward_button :
                if(isPlaying){
                    wecandeoSdk.fastForward();
                    statistics.sendStatistics(Statistics.SEEK);
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

    private void disableButton(Button button){
        if(button == retryButton){
            for(Button item : buttonList){
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
    public void onLoadingChanged(boolean isLoading) {}


    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        // 영상이 로드되고 처음 준비 시
        if(playbackState == PLAY_ENABLE && wecandeoSdk.getPlayer() != null && isInitVideoInfo){
            isInitVideoInfo = false;
            statistics.setWecandeoSdk(wecandeoSdk);
            statistics.setDuration(TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getDuration()));
            // 큐포인트 정보, 비디오 정보
            statistics.sendCuePointStatistics();
        }

        // 영상이 완료되었을 때
        if(playWhenReady && playbackState == PLAY_COMPLETE && wecandeoSdk.getPlayer() != null){
            wecandeoSdk.complete();
            statistics.sendStatistics(Statistics.STOP);
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

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {}

    @Override
    public void onPositionDiscontinuity() {}
}
