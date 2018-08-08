package com.example.mediatest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, android.hardware.Camera.PreviewCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static LinkedList<byte[]> YUVQueue = new LinkedList<>();

    public static SurfaceHolder surfaceHolder;

    public static SurfaceHolder surfaceHolder2;

    private SurfaceView surfaceView;

    private SurfaceView surfaceViewOut;

    protected Camera mCamera;

    private Camera.Parameters parameters;

    private Encoder mEncoder = Encoder.getInstance();

    private Decoder mDecoder = Decoder.getInstance();

    private Button mGenerate_mp4;

    private Button mPlay_MP4;

    private Button mPlay_H264;

    private Button mEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initSurfaceHolder();
        initSurfaceHolder2();
        DecodeH264File.getInstance().init();
    }

    private void initView() {
        surfaceView = findViewById(R.id.surface);
        mGenerate_mp4 = findViewById(R.id.generate_mp4);
        mPlay_MP4 = findViewById(R.id.play_mp4);
        mPlay_H264 = findViewById(R.id.play_h264);
        surfaceViewOut=findViewById(R.id.surface2);
        mEnd = findViewById(R.id.end);
        mGenerate_mp4.setOnClickListener(this);
        mPlay_MP4.setOnClickListener(this);
        mPlay_H264.setOnClickListener(this);
        mEnd.setOnClickListener(this);
    }

    private void initSurfaceHolder() {
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera();
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

    private void initSurfaceHolder2() {
        surfaceHolder2 = surfaceViewOut.getHolder();
        surfaceHolder2.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

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
        mCamera.setDisplayOrientation(90);
        if (parameters == null) {
            parameters = mCamera.getParameters();
        }
        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setPreviewSize(1280, 720);
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
        if (YUVQueue.size() > 10) {
            YUVQueue.poll();
        }
        YUVQueue.add(data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.generate_mp4:
                mEncoder.startEncode();
                break;
            case R.id.play_mp4:
                mDecoder.startMP4Decode();
                break;
            case R.id.play_h264:
                mDecoder.startH264Decode();
                break;
            case R.id.end:
                mEncoder.close();
                mDecoder.close();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyCamera();
        mDecoder.close();
        mEncoder.close();
    }
}
