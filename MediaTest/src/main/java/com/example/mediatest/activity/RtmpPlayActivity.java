package com.example.mediatest.activity;

/**
 * Created by zhangxd on 2018/9/5.
 */

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.example.mediatest.R;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.utils.Log;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class RtmpPlayActivity extends Activity {

    private static final String TAG = RtmpPlayActivity.class.getSimpleName();

    private VideoView mVideoView;

    private Button mButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtmp_play);
        if (!LibsChecker.checkVitamioLibs(this)) {
            Log.d(TAG, "onCreate checkVitamioLibs failed");
            return;
        }
        initView();
        initData();
    }

    private void initView() {
        mVideoView = findViewById(R.id.video_view);
        mVideoView.setVideoPath("rtmp://live.hkstv.hk.lxdns.com/live/hks");
        mButton = findViewById(R.id.end);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initData() {
        mVideoView.setMediaController(new MediaController(this));
        mVideoView.requestFocus();
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setPlaybackSpeed(1.0f);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
