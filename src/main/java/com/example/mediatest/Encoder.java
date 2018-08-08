package com.example.mediatest;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by zhangxd on 2018/7/3.
 * 编码，支持同步和异步方式
 * 生成码流与MP4文件
 */

public class Encoder {

    private static final String TAG = "Encoder";

    public static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test1.h264";

    private static final String MP4_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.mp4";

    private BufferedOutputStream outputStream;

    private MediaCodec mediaCodec;

    private MediaMuxer mediaMuxer;

    private long frameIndex;

    private static Encoder instance;

    private volatile boolean isMuxFinish = false;

    private int mTrackIndex;


    public Encoder() {
        init();
    }

    public static Encoder getInstance() {
        if (instance == null) {
            instance = new Encoder();
        }
        return instance;
    }


    private void createfile() {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
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

    public void initMediaCodecAsy() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 1280, 720);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3000000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            mediaMuxer = new MediaMuxer(MP4_PATH, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                Log.d(TAG, "onInputBufferAvailable ");
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
                byte[] temp = getInputBuffer();
                inputBuffer.put(temp);
                long time = computePresentationTime();
                mediaCodec.queueInputBuffer(index, 0, temp.length, time, 0);
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                Log.d(TAG, "onOutputBufferAvailable ");
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
                byte[] temp = new byte[info.size];
                outputBuffer.get(temp);
                writeBytesToFile(temp);
                outputBuffer.position(info.offset);
                outputBuffer.limit(info.offset + info.size);
                mediaMuxer.writeSampleData(mTrackIndex, outputBuffer, info);
                mediaCodec.releaseOutputBuffer(index, false);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                Log.d(TAG, "onError ");
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                Log.d(TAG, "onOutputFormatChanged ");
                MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                mTrackIndex = mediaMuxer.addTrack(mediaFormat);
                mediaMuxer.start();
            }
        });
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    public void initSysMediaCodec() {
        //编码格式，AVC对应的是H264
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720);
        //YUV 420 对应的是图片颜色采样格式
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        //比特率
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3000000);
        //帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
        //I 帧间隔
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            //创建生成MP4初始化对象
            mediaMuxer = new MediaMuxer(MP4_PATH, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //进入配置状态
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //进行生命周期执行状态
        mediaCodec.start();
    }

    private byte[] getInputBuffer() {
        byte[] input = MainActivity.YUVQueue.poll();
        byte[] yuv420sp = new byte[1280 * 720 * 3 / 2];
        NV21ToNV12(input, yuv420sp, 1280, 720);
        return yuv420sp;
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

    private void writeBytesToFile(byte[] buffer) {
        try {
            outputStream.write(buffer, 0, buffer.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void close() {
        isMuxFinish = true;
        mediaCodec.stop();
        mediaCodec.release();
        try {
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
        }
        mediaCodec = null;
        mediaMuxer = null;
    }

    public void startEncode() {
        initSysMediaCodec();
         new EncoderThread().start();
    }

    class EncoderThread extends Thread {
        @Override
        public void run() {
            long pts = 0;
            super.run();
            while (!isMuxFinish) {
                if (mediaCodec == null) {
                    break;
                }
                // 拿到有空闲的输入缓存区下标
                int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
                if (inputBufferId >= 0) {
                    pts = computePresentationTime();
                    //有效的空的缓存区
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                    byte[] tempByte = getInputBuffer();
                    if(isMuxFinish){
                        break;
                    }
                    inputBuffer.put(tempByte);
                    //将数据放到编码队列
                    mediaCodec.queueInputBuffer(inputBufferId, 0, tempByte.length, pts, 0);
                }
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                //得到成功编码后输出的out buffer Id
                int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (outputBufferId >= 0) {
                    ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferId);
                    byte[] out = new byte[bufferInfo.size];
                    outputBuffer.get(out);
                    writeBytesToFile(out);
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    // 将编码后的数据写入到MP4复用器
                    mediaMuxer.writeSampleData(mTrackIndex, outputBuffer, bufferInfo);
                    //释放output buffer
                    mediaCodec.releaseOutputBuffer(outputBufferId, false);
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                    mTrackIndex = mediaMuxer.addTrack(mediaFormat);
                    mediaMuxer.start();
                }
            }

        }
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime() {
        frameIndex++;
        return 132 + frameIndex * 1000000 / 30;
    }

}
