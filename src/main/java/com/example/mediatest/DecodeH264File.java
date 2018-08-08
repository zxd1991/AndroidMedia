package com.example.mediatest;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by zhangxd on 2018/7/26.
 * 解析H264文件类
 */

public class DecodeH264File {

    private static final String TAG = "DecodeH264File";

    public static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test2.h264";

    private FileInputStream inputStream;

    private RandomAccessFile rf;

    //当前读到的帧位置
    private int curIndex;

    private StringBuilder builder = new StringBuilder();

    private String[] SLICE;

    private List<Byte> byteList = new ArrayList();

    private static DecodeH264File instance;

    private boolean isStartCode4;

    public static DecodeH264File getInstance() {
        if (instance == null) {
            instance = new DecodeH264File();
        }
        return instance;
    }

    public DecodeH264File() {

    }

    public void init() {
        initInputStream();
    }

    private void initInputStream() {
        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("h264 file does not exists");
        }
        try {
            rf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * 读取每一帧数据
     * @param buffer
     * @return
     */
    public int readSampleData(ByteBuffer buffer) {
        byte[] nal = getNALU();
        buffer.put(nal);
        return nal.length;
    }

    private byte[] getNALU() {
        try {
            int curpos = 0;
            byte[] bb = new byte[100000];
            rf.read(bb, 0, 4);
            if (findStartCode4(bb, 0)) {
                curpos = 4;
            } else {
                rf.seek(0);
                rf.read(bb, 0, 3);
                if (findStartCode3(bb, 0)) {
                    curpos = 3;
                }
            }
            boolean findNALStartCode = false;
            int nextNalStartPos = 0;
            int reWind = 0;
            while (!findNALStartCode) {
                int hex = rf.read();
                if (curpos >= bb.length) {
                    break;
                }
                bb[curpos++] = (byte) hex;
                if (hex == -1) {
                    nextNalStartPos = curpos;
                }
                if (findStartCode4(bb, curpos - 4)) {
                    findNALStartCode = true;
                    reWind = 4;
                    nextNalStartPos = curpos - reWind;

                } else if (findStartCode3(bb, curpos - 3)) {
                    findNALStartCode = true;
                    reWind = 3;
                    nextNalStartPos = curpos - reWind;
                }
            }
            byte[] nal = new byte[nextNalStartPos];
            System.arraycopy(bb, 0, nal, 0, nextNalStartPos);
            long pos = rf.getFilePointer();
            long setPos = pos - reWind;
            rf.seek(setPos);
            return nal;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //find match "00 00 00 01"
    private boolean findStartCode4(byte[] bb, int offSet) {
        if (offSet < 0) {
            return false;
        }
        if (bb[offSet] == 0 && bb[offSet + 1] == 0 && bb[offSet + 2] == 0 && bb[offSet + 3] == 1) {
            return true;
        }
        return false;
    }

    //find match "00 00 01"
    private boolean findStartCode3(byte[] bb, int offSet) {
        if (offSet <= 0) {
            return false;
        }
        if (bb[offSet] == 0 && bb[offSet + 1] == 0 && bb[offSet + 2] == 1) {
            return true;
        }
        return false;
    }


}
