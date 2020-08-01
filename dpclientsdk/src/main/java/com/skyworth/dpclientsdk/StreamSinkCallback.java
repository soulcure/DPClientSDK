package com.skyworth.dpclientsdk;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

public interface StreamSinkCallback {

    void onConnectState(ConnectState state);

    //local socket string data
    void onData(byte[] data);

    //local socket string data
    void onData(String data);

    //audio frame
    void onAudioFrame(MediaCodec.BufferInfo bufferInfo, ByteBuffer data);

    //video frame
    void onVideoFrame(MediaCodec.BufferInfo bufferInfo, ByteBuffer data);

}
