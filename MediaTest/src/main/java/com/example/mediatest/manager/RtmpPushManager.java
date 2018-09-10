package com.example.mediatest.manager;

import android.util.Log;

import net.butterflytv.rtmp_client.RTMPMuxer;

/**
 * Created by zhangxd on 2018/8/29.
 * rtmp 推流客户端
 */

public class RtmpPushManager {

    private static final String TAG = "RtmpClientManager";

    private static final String URL = "rtmp://10.1.62.2:11935/live/135808cf-7cb2-4b5b-90b0-3a6e7ac9c717?s=666";

    private static RtmpPushManager instance;

    private RTMPMuxer mRtmpMuxer = new RTMPMuxer();

    public static RtmpPushManager getInstance() {
        if (instance == null) {
            instance = new RtmpPushManager();
        }
        return instance;
    }


    public void init(String url) {
        if (url == null || url.equals("")) {
            url = URL;
        }
        int status = mRtmpMuxer.open(url, 1280, 720);
        Log.d(TAG, " RtmpClientManager open status :" + status +" url: "+ url);
        if (mRtmpMuxer.isConnected() == 1) {
            Log.d(TAG, " RtmpClientManager connected..");
        }

    }

    public void writeVideoData(byte[] data, int timestamp) {
        mRtmpMuxer.writeVideo(data, 0, data.length, timestamp);
    }

    public void writeAudioData(byte[] data, int timestamp) {
        mRtmpMuxer.writeAudio(data, 0, data.length, timestamp);
    }


    public void close() {
        mRtmpMuxer.close();
    }
}
