package com.scenappsm.android.wcPlayerStatistics;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button vodButton;
    private Button liveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews(){
        vodButton = findViewById(R.id.vod_button);
        vodButton.setOnClickListener(this);
        liveButton = findViewById(R.id.live_button);
        liveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.vod_button :
                startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                break;
            case R.id.live_button :
                startActivity(new Intent(MainActivity.this, LiveActivity.class));
                break;
        }
    }
}
