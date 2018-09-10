package com.example.mediatest.manager;

import net.butterflytv.rtmp_client.RtmpClient;

/**
 * Created by zhangxd on 2018/9/5.
 */

public class RtmpClientManager {

    private static final String TAG = RtmpClientManager.class.getSimpleName();

    private static final String URL = "rtmp://live.hkstv.hk.lxdns.com/live/hks";

    private static RtmpClientManager instance;

    public RtmpClient mRtmpClient = new RtmpClient();

    public RtmpClientManager getInstance() {
        if (instance == null) {
            instance = new RtmpClientManager();
        }
        return instance;
    }

    public void open() {
        try {
            mRtmpClient.open(URL, false);
        } catch (RtmpClient.RtmpIOException e) {
            e.printStackTrace();
        }

    }


}
