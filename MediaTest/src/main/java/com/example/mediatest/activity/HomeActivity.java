package com.example.mediatest.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.example.mediatest.R;

import io.vov.vitamio.utils.Log;

/**
 * Created by zhangxd on 2018/9/5.
 */

public class HomeActivity extends Activity implements View.OnClickListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

    private Button mPlayMp4;

    private Button mPlayH264;

    private Button mPushStream;

    private Button mPlayStream;

    private Button mRecordMp4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initView();
        requestPermissions();
    }

    private void initView() {
        mRecordMp4 = findViewById(R.id.record_mp4);
        mPlayMp4 = findViewById(R.id.play_mp4);
        mPlayH264 = findViewById(R.id.play_h264);
        mPushStream = findViewById(R.id.push_stream);
        mPlayStream = findViewById(R.id.play_net_stream);
        mRecordMp4.setOnClickListener(this);
        mPlayMp4.setOnClickListener(this);
        mPlayH264.setOnClickListener(this);
        mPushStream.setOnClickListener(this);
        mPlayStream.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record_mp4: {
                Intent intent = new Intent();
                intent.setClass(HomeActivity.this, RecordActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.play_mp4: {
                Intent intent = new Intent();
                intent.setClass(HomeActivity.this, PlayVideoActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.play_h264: {
                Intent intent = new Intent();
                intent.putExtra("isPlayH264", true);
                intent.setClass(HomeActivity.this, PlayVideoActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.push_stream: {
                Intent intent = new Intent();
                intent.putExtra("isPushStream", true);
                intent.setClass(HomeActivity.this, RecordActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.play_net_stream: {
                Intent intent = new Intent();
                intent.setClass(HomeActivity.this, RtmpPlayActivity.class);
                startActivity(intent);
            }
            break;
            default:
                break;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, 0);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 0) {
            return;
        }
        for (int i = 0; i < grantResults.length; i++) {
            String requestResult = permissions[i];
            int result = grantResults[i];
            if (result != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, " onRequestPermissionsResult: " + requestResult);
                finish();
            } else {
                Log.d(TAG, " onRequestPermissionsResult: " + requestResult);
            }
        }
    }
}
