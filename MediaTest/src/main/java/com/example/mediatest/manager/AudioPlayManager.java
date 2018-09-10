package com.example.mediatest.manager;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import com.example.mediatest.MyApplication;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by zhangxd on 2018/8/25.
 */

public class AudioPlayManager {

    private static final String TAG = AudioTrack.class.getSimpleName();

    private static AudioPlayManager instance;

    private int mSampleRate = 44100;

    private int channelCount = 2;

    private AudioTrack mAudioTrack;

    private int channelConfig = AudioFormat.CHANNEL_IN_STEREO;

    private int bufferSize;

    private int audioFormatEncode = AudioFormat.ENCODING_PCM_16BIT;

    private AudioManager mAudioManager;

    private Context mContext;

    private AudioFormat mAudioFormat;

    private FileInputStream fileInputStream;

    private boolean startPlay = false;

    private MediaCodec mediaCodec;

    private MediaFormat mediaFormat;

    private MediaExtractor mediaExtractor;

    private boolean isDecodeFinish = false;

    public AudioPlayManager() {

    }

    public static AudioPlayManager getInstance() {
        if (instance == null) {
            instance = new AudioPlayManager();
        }
        return instance;
    }

    public void setContext(Context context) {
        this.mContext = context;
        init();
        initMediaExactor();
    }

    private void init() {
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        bufferSize = AudioTrack.getMinBufferSize(mSampleRate, channelConfig, audioFormatEncode);
        int sessionId = mAudioManager.generateAudioSessionId();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(mSampleRate)
                .setEncoding(audioFormatEncode)
                .setChannelMask(channelConfig)
                .build();
        mAudioTrack = new AudioTrack(audioAttributes, audioFormat, bufferSize * 2, AudioTrack.MODE_STREAM, sessionId);
    }

    private void initMediaExactor() {
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, mSampleRate, channelCount);
            mediaExtractor = new MediaExtractor();
            //MP4 文件存放位置
            mediaExtractor.setDataSource(MyApplication.MP4_PLAY_PATH);
            Log.d(TAG, "getTrackCount: " + mediaExtractor.getTrackCount());
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, "mime: " + mime);
                if (mime.startsWith("audio")) {
                    mediaFormat = format;
                    mediaExtractor.selectTrack(i);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaCodec.configure(mediaFormat, null, null, 0);
        mediaCodec.start();
    }

    public void startThread() {
        isDecodeFinish = false;
        new PlayThread().start();
    }

    public void close() {
        isDecodeFinish = true;
        mAudioTrack.stop();
        mAudioTrack.release();
        instance = null;
    }

    class PlayThread extends Thread {
        @Override
        public void run() {
            super.run();
            mAudioTrack.play();
            while (!isDecodeFinish) {
                int inputIndex = mediaCodec.dequeueInputBuffer(-1);
                Log.d(TAG, "inputIndex: " + inputIndex);
                if (inputIndex >= 0) {
                    ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputIndex);
                    //读取一片或者一帧数据
                    int sampSize = mediaExtractor.readSampleData(byteBuffer, 0);
                    //读取时间戳
                    long time = mediaExtractor.getSampleTime();
                    if (sampSize > 0 && time >= 0) {
                        mediaCodec.queueInputBuffer(inputIndex, 0, sampSize, time, 0);
                        //读取一帧后必须调用，提取下一帧
                        mediaExtractor.advance();
                        //控制帧率在30帧左右
                    }

                }
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (outIndex >= 0) {
                    ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
                    byte[] bytes = new byte[bufferInfo.size];
                    byteBuffer.get(bytes);
                    mAudioTrack.write(bytes, 0, bytes.length);
                    mediaCodec.releaseOutputBuffer(outIndex, true);
                }
            }
        }
    }


}
