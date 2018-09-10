package com.example.mediatest.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.mediatest.R;
import com.example.mediatest.encode.EncoderManager;
import com.example.mediatest.manager.AudioRecordManager;
import com.example.mediatest.manager.MediaMuxerManager;
import com.example.mediatest.manager.RtmpPushManager;

import android.hardware.Camera.Size;
import android.widget.TextView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.vov.vitamio.utils.Log;

/**
 * Created by zhangxd on 2018/9/5.
 */

public class RecordActivity extends Activity implements View.OnClickListener, android.hardware.Camera.PreviewCallback {

    private static final String TAG = RecordActivity.class.getSimpleName();

    public static LinkedList<byte[]> mYUVQueue = new LinkedList<>();

    private static final int START_ENCODE = 0x01;

    private static final int START_ENCODE_MSG_DELAY = 2000;

    private static final int MSG_SHOW_TIME = 0x06;

    private static final int TIME_LIMIT = 59;

    private static final int TIME_FORMAT = 10;

    private static final int MAX_SIZE = 100;

    public static SurfaceHolder surfaceHolder;

    private SurfaceView surfaceView;

    protected Camera mCamera;

    private Camera.Parameters parameters;

    private Button mButtonEnd;

    private LinearLayout mPushStreamLayout;

    private EditText mPushEditText;

    private Button mStreamDoneBtn;

    private static Size mSize;

    private int mSecond;

    private int mMinute;

    private int mHour;

    private TextView mTimeRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        initView();
        initSurfaceHolder();
        initManager();
    }

    private void initView() {
        surfaceView = findViewById(R.id.surfaceview);
        mButtonEnd = findViewById(R.id.end);
        mButtonEnd.setOnClickListener(this);
        mPushStreamLayout = findViewById(R.id.linearlayout);
        mPushEditText = findViewById(R.id.push_strem_address);
        mStreamDoneBtn = findViewById(R.id.push_stream_done);
        mTimeRun = findViewById(R.id.record_time);
        mStreamDoneBtn.setOnClickListener(this);
    }

    private void initManager() {
        MediaMuxerManager.getInstance().init();
        Intent intent = getIntent();
        boolean pushStream = intent.getBooleanExtra("isPushStream", false);
        if (pushStream) {
            mPushStreamLayout.setVisibility(View.VISIBLE);
        }
    }

    private void initSurfaceHolder() {
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera();
                setTextTime();
                mHandler.sendEmptyMessageDelayed(START_ENCODE, START_ENCODE_MSG_DELAY);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mCamera.startPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                destroyCamera();
            }
        });
    }


    private void initCamera() {
        mCamera = Camera.open(1);
        mCamera.setPreviewCallback(this);
        setCameraDisplayOrientation();
        if (parameters == null) {
            parameters = mCamera.getParameters();
        }
        parameters.setPreviewFormat(ImageFormat.NV21);
        List<Size> mSizeList = parameters.getSupportedPreviewSizes();
        mSize = mSizeList.get(0);
        parameters.setPreviewSize(mSize.width, mSize.height);
        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mYUVQueue.size() > MAX_SIZE) {
            mYUVQueue.poll();
        }
        mYUVQueue.add(data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.end: {
                finish();
                break;
            }
            case R.id.push_stream_done: {
                RtmpPushManager.getInstance().init(mPushEditText.getText().toString());
                mPushStreamLayout.setVisibility(View.GONE);
                EncoderManager.getInstance().setIsPushStream(true);
                AudioRecordManager.getInstance().setIsPushStream(true);
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyCamera();
    }


    private void setCameraDisplayOrientation() {
        Display mDisplay = getWindowManager().getDefaultDisplay();
        int orientation = mDisplay.getOrientation();
        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(1, info);
        int degrees = 0;
        switch (orientation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;

        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {// back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
        Log.d(TAG, " orientation: " + orientation);
    }

    public static Size getSize() {
        return mSize;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == START_ENCODE) {
                EncoderManager.getInstance().startEncode();
                AudioRecordManager.getInstance().startRecordThread();
            } else if (msg.what == MSG_SHOW_TIME) {
                setTextTime();
            }

        }
    };

    private void setTextTime() {
        mSecond++;
        if (mSecond > TIME_LIMIT) {
            mSecond = 0;
            mMinute++;
            if (mMinute > TIME_LIMIT) {
                mMinute = 0;
                mHour++;
            }
        }
        StringBuilder builder = new StringBuilder();
        if (mHour < TIME_FORMAT) {
            builder.append(0);
        }
        builder.append(mHour).append(":");
        if (mMinute < TIME_FORMAT) {
            builder.append(0);
        }
        builder.append(mMinute).append(":");
        if (mSecond < TIME_FORMAT) {
            builder.append(0);
        }
        builder.append(mSecond);
        mTimeRun.setText(builder.toString());
        mHandler.sendEmptyMessageDelayed(MSG_SHOW_TIME, 1000);
    }

    public static LinkedList getYUVQueue() {
        if (mYUVQueue.isEmpty()) {
            EncoderManager.getInstance().close();
            AudioRecordManager.getInstance().close();
        }
        return mYUVQueue;
    }
}
