package com.example.mediatest.manager;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Environment;

import net.butterflytv.rtmp_client.RtmpClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import io.vov.vitamio.utils.Log;

/**
 * Created by zhangxd on 2018/8/24.
 * 音频采集成PCM
 */

public class AudioRecordManager {

    private static final String TAG = AudioRecordManager.class.getSimpleName();

    public static String PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio.pcm";

    private static AudioRecordManager instance;

    private AudioRecord mAudioRecord;

    private String MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;

    private int mSampleRate = 44100;

    private int channelCount = 2;

    private int channelConfig = AudioFormat.CHANNEL_IN_STEREO;

    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private int bufferSize;

    private volatile boolean isStart;

    private BufferedOutputStream outputStream;

    private MediaCodec mAudioMediaCodec;

    private int KEY_BIT_RATE = 64000;

    private long nanoTime;

    private long frameIndex;

    private int mAudioTrackIndex;

    private RtmpClient mRtmpClient = new RtmpClient();

    public boolean pushStream;

    private Thread mAudioThread;

    public static AudioRecordManager getInstance() {
        if (instance == null) {
            instance = new AudioRecordManager();
        }
        return instance;
    }

    private AudioRecordManager() {
        init();
    }

    private void init() {
        bufferSize = AudioRecord.getMinBufferSize(mSampleRate, channelConfig, audioFormat);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, channelConfig, audioFormat, bufferSize * 2);
        createfile();
        initMediaCodec();
    }

    private void initMediaCodec() {
        try {
            mAudioMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MIME_TYPE, mSampleRate, channelCount);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mAudioMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mAudioMediaCodec.start();
        } catch (IOException e) {

        }

    }

    public void startRecordThread() {
        isStart = true;
        mAudioThread = new RecordThread();
        mAudioThread.start();
    }

    public void close() {
        isStart = false;
        try {
            mAudioThread.join(2000);
        } catch (InterruptedException e) {
            Log.e(TAG, "stopRecordThread InterruptedException");
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
        }
        instance = null;
    }


    class RecordThread extends Thread {
        @Override
        public void run() {
            super.run();
            if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                return;
            }
            long startTime = System.nanoTime();
            mAudioRecord.startRecording();
            while (isStart) {
                int inputBufferIndex = mAudioMediaCodec.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = mAudioMediaCodec.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    int length = mAudioRecord.read(inputBuffer, bufferSize);
                    mAudioMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, (System.nanoTime() - startTime) / 1000, 0);
                }
                MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mAudioMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                if (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = mAudioMediaCodec.getOutputBuffer(outputBufferIndex);
                    byte[] audioBytes = new byte[mBufferInfo.size];
                    outputBuffer.get(audioBytes);

                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                    mAudioMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    if (pushStream) {
                        RtmpPushManager.getInstance().writeAudioData(audioBytes, (int) mBufferInfo.presentationTimeUs);
                    } else if (MediaMuxerManager.getInstance().isReady()) {
                        MediaMuxerManager.getInstance().writeSampleData(mAudioTrackIndex, outputBuffer, mBufferInfo);
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat mediaFormat = mAudioMediaCodec.getOutputFormat();
                    mAudioTrackIndex = MediaMuxerManager.getInstance().addTrack(mediaFormat);
                    MediaMuxerManager.getInstance().start();
                }
            }
        }
    }


    /**
     * 给编码出的aac裸流添加adts头字段
     *
     * @param packet    要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
        //}

    }


    private void createfile() {
        File file = new File(PATH);
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setIsPushStream(boolean pushStream) {
        this.pushStream = pushStream;
    }

}
