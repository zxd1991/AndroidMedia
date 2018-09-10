package com.example.mediatest.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.example.mediatest.R;
import com.example.mediatest.decode.DecoderManager;
import com.example.mediatest.manager.AudioPlayManager;

import io.vov.vitamio.utils.Log;

/**
 * Created by zhangxd on 2018/9/5.
 */

public class PlayVideoActivity extends Activity {

    private static final String TAG = PlayVideoActivity.class.getSimpleName();

    private static final int INIT_MANAGER_MSG = 0x01;

    private static final int INIT_MANAGER_DELAY = 1 * 1000;

    public static SurfaceView surfaceView;

    private SurfaceHolder mSurfaceHolder;

    private Button mEndBtn;

    private boolean isPlayH264;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_mp4);
        surfaceView = findViewById(R.id.surfaceview);
        mEndBtn = findViewById(R.id.end);
        mEndBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DecoderManager.getInstance().close();
                if (!isPlayH264) {
                    AudioPlayManager.getInstance().close();
                }
                finish();
            }
        });
        initSurface();

    }

    private void initSurface() {
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mHandler.sendEmptyMessageDelayed(INIT_MANAGER_MSG, INIT_MANAGER_DELAY);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    private void initManager() {
        Intent intent = getIntent();
        isPlayH264 = intent.getBooleanExtra("isPlayH264", false);
        if (isPlayH264) {
            DecoderManager.getInstance().startH264Decode();
            return;
        }
        DecoderManager.getInstance().startMP4Decode();
        AudioPlayManager.getInstance().setContext(getApplicationContext());
        AudioPlayManager.getInstance().startThread();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy..");
    }

    public static Surface getSurface() {
        return surfaceView.getHolder().getSurface();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == INIT_MANAGER_MSG) {
                initManager();
            }
        }
    };

}
