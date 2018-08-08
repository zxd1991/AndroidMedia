package com.example.mediatest;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec.BufferInfo;
import android.view.Surface;

/**
 * Created by zhangxd on 2018/7/6.
 */

public class Decoder {

    private static final String TAG = "Decoder";

    public static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/1.mp4";

    private static Decoder instance;

    private MediaCodec mediaCodec;

    private MediaFormat mediaFormat;

    private long frameIndex;

    private boolean isDecodeFinish = false;

    private MediaExtractor mediaExtractor;

    private Decoder() {

    }

    public static Decoder getInstance() {
        if (instance == null) {
            instance = new Decoder();
        }
        return instance;
    }

    /**
     *  Asynchronous callback decoding
     */
    public void initMediaCodecAsy() {
        try {
            mediaCodec = MediaCodec.createDecoderByType("video/avc");
            mediaFormat = MediaFormat.createVideoFormat("video/avc", 1280, 720);
            mediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    Log.d(TAG, "onInputBufferAvailable index: " + index);
                    ByteBuffer byteBuffer = mediaCodec.getInputBuffer(index);
                    int sampSize = mediaExtractor.readSampleData(byteBuffer, 0);
                    long time = mediaExtractor.getSampleTime();
                    if (sampSize > 0 && time > 0) {
                        mediaCodec.queueInputBuffer(index, 0, sampSize, time, 0);
                        mediaExtractor.advance();
                    }

                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull BufferInfo info) {
                    Log.d(TAG, "onOutputBufferAvailable index: " + index);
                    mediaCodec.releaseOutputBuffer(index, true);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    Log.e(TAG, "onError: e" + e);
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    Log.e(TAG, "onOutputFormatChanged: format");
                }
            });
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(path);
            Log.d(TAG, "getTrackCount: " + mediaExtractor.getTrackCount());
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video")) {
                    mediaFormat = format;
                    mediaExtractor.selectTrack(i);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Surface surface = MainActivity.surfaceHolder2.getSurface();
        mediaCodec.configure(mediaFormat, surface, null, 0);
        mediaCodec.start();
    }

    /**
     ** Synchronized callback decoding
     */
    private void initMediaCodecSys(){
        try {
            mediaCodec = MediaCodec.createDecoderByType("video/avc");
            mediaFormat = MediaFormat.createVideoFormat("video/avc", 1280, 720);
            mediaExtractor = new MediaExtractor();
            //MP4 文件存放位置
            mediaExtractor.setDataSource(path);
            Log.d(TAG, "getTrackCount: " + mediaExtractor.getTrackCount());
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video")) {
                    mediaFormat = format;
                    mediaExtractor.selectTrack(i);
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        Surface surface = MainActivity.surfaceHolder2.getSurface();
        mediaCodec.configure(mediaFormat, surface, null, 0);
        mediaCodec.start();
    }

    /**
     *  Play the MP4 file Thread
     */
    private class DecoderMP4Thread extends Thread {
        long pts = 0;

        @Override
        public void run() {
            super.run();
            while (!isDecodeFinish) {
                int inputIndex = mediaCodec.dequeueInputBuffer(-1);
                Log.d(TAG, "inputIndex: " + inputIndex);
                if (inputIndex >= 0) {
                    ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputIndex);
                    //读取一片或者一帧数据
                    int sampSize = mediaExtractor.readSampleData(byteBuffer,0);
                    //读取时间戳
                    long time = mediaExtractor.getSampleTime();
                    Log.d(TAG, "sampSize: " + sampSize + "time: " + time);
                    if (sampSize > 0 && time > 0) {
                        mediaCodec.queueInputBuffer(inputIndex, 0, sampSize, time, 0);
                        //读取一帧后必须调用，提取下一帧
                        mediaExtractor.advance();
                        //控制帧率在30帧左右
                        try {
                            sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
                BufferInfo bufferInfo = new BufferInfo();
                int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                Log.d(TAG, "outIndex: " + outIndex);
                if (outIndex >= 0) {
                    mediaCodec.releaseOutputBuffer(outIndex, true);
                }
            }
        }

    }

    /**
     *  解析播放H264码流
     */
    private class DecoderH264Thread extends Thread {
        long pts = 0;

        @Override
        public void run() {
            super.run();
            while (!isDecodeFinish) {
                int inputIndex = mediaCodec.dequeueInputBuffer(-1);
                if (inputIndex >= 0) {
                    ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputIndex);
                    int sampSize = DecodeH264File.getInstance().readSampleData(byteBuffer);
                    long time = computePresentationTime();
                    if (sampSize > 0 && time > 0) {
                        mediaCodec.queueInputBuffer(inputIndex, 0, sampSize, time, 0);
                        try {
                            sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
                BufferInfo bufferInfo = new BufferInfo();
                int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (outIndex >= 0) {
                    mediaCodec.releaseOutputBuffer(outIndex, true);
                }
            }
        }

    }



    public void close() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            isDecodeFinish=true;
        }
    }


    public void startMP4Decode() {
        initMediaCodecSys();
        new DecoderMP4Thread().start();
    }

    public void startH264Decode(){
        initMediaCodecSys();
        new DecoderH264Thread().start();
    }



    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime() {
        frameIndex++;
        return 132 + frameIndex * 1000000 / 30;
    }
}
