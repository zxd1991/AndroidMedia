package com.example.mediatest.encode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;

import com.example.mediatest.activity.RecordActivity;
import com.example.mediatest.manager.MediaMuxerManager;
import com.example.mediatest.manager.RtmpPushManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import io.vov.vitamio.utils.Log;

/**
 * Created by zhangxd on 2018/7/3.
 * 编码，支持同步和异步方式
 * 生成码流与MP4文件
 */

public class EncoderManager {

    private static final String TAG = EncoderManager.class.getSimpleName();

    public static String PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test1.h264";

    private BufferedOutputStream outputStream;

    private MediaCodec mediaCodec;

    private long frameIndex;

    private static EncoderManager instance;

    private volatile boolean isMuxFinish = false;

    private int mTrackIndex;

    private boolean pushStream;

    private Thread mEncodeThread;

    private int mWidth;

    private int mHeight;

    private byte[] yuv420spsrc;

    public EncoderManager() {
        init();
    }

    public static EncoderManager getInstance() {
        if (instance == null) {
            instance = new EncoderManager();
        }
        return instance;
    }


    private void createfile() {
        File file = new File(PATH);
        if (!file.exists()) {
            file.mkdir();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void init() {
        createfile();
    }


    public void initMediaCodec() {
        mWidth = RecordActivity.getSize().width;
        mHeight = RecordActivity.getSize().height;
        yuv420spsrc = new byte[mWidth * mHeight * 3 / 2];
        //编码格式，AVC对应的是H264
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        //YUV 420 对应的是图片颜色采样格式
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        //比特率
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3000000);
        //帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        //I 帧间隔
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            //创建生成MP4初始化对象
        } catch (IOException e) {
            e.printStackTrace();
        }
        //进入配置状态
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //进行生命周期执行状态
        mediaCodec.start();
    }

    private byte[] getInputBuffer() {
        byte[] input = (byte[]) RecordActivity.getYUVQueue().poll();
        if (input == null) {
            return null;
        }
        NV21ToNV12(input, yuv420spsrc, mWidth, mHeight);
        return yuv420spsrc;
    }



    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) {
            return;
        }
        int framesize = width * height;
        int i, j;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }


    public void close() {
        isMuxFinish = true;
        try {
            mEncodeThread.join(1000);
        } catch (InterruptedException e) {
            Log.d(TAG, "InterruptedException " + e);
        }
        Log.d(TAG, " EncodeThread isAlive: " + mEncodeThread.isAlive());
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
        try {
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        MediaMuxerManager.getInstance().close();
        mediaCodec = null;
        instance = null;
    }

    public void startEncode() {
        initMediaCodec();
        mEncodeThread = new VideoEncoderThread();
        mEncodeThread.setName("VideoEncoderThread");
        mEncodeThread.start();
    }

    class VideoEncoderThread extends Thread {
        @Override
        public void run() {
            super.run();
            long startTime = System.nanoTime();
            while (!isMuxFinish) {
                // 拿到有空闲的输入缓存区下标
                int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
                if (inputBufferId >= 0) {
                    //有效的空的缓存区
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                    byte[] tempByte = getInputBuffer();
                    if (tempByte == null) {
                        break;
                    }
                    inputBuffer.put(tempByte);
                    //将数据放到编码队列
                    mediaCodec.queueInputBuffer(inputBufferId, 0, tempByte.length, (System.nanoTime() - startTime) / 1000, 0);
                }
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                //得到成功编码后输出的out buffer Id
                int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (outputBufferId >= 0) {
                    ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferId);
                    byte[] out = new byte[bufferInfo.size];
                    outputBuffer.get(out);
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    // 将编码后的数据写入到MP4复用器
                    if (pushStream) {
                        RtmpPushManager.getInstance().writeVideoData(out, (int) bufferInfo.presentationTimeUs);
                    } else if (MediaMuxerManager.getInstance().isReady()) {
                        MediaMuxerManager.getInstance().writeSampleData(mTrackIndex, outputBuffer, bufferInfo);
                    }
                    //释放output buffer
                    mediaCodec.releaseOutputBuffer(outputBufferId, false);
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                    mTrackIndex = MediaMuxerManager.getInstance().addTrack(mediaFormat);
                    MediaMuxerManager.getInstance().start();
                }
            }

        }
    }

    public void setIsPushStream(boolean pushStream) {
        this.pushStream = pushStream;
    }

}
