package com.skyworth.dpclient;


import android.media.MediaCodec;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.skyworth.dpclientsdk.ConnectState;
import com.skyworth.dpclientsdk.StreamChannelSink;
import com.skyworth.dpclientsdk.StreamSinkCallback;

import java.nio.ByteBuffer;

public class StreamSinkActivity extends AppCompatActivity implements View.OnClickListener {

    StreamChannelSink test;
    public int sinkIndex = 0;
    private long firstTime;
    private int count = 0;
    private long temp;
    StreamSinkCallback mCallback = new StreamSinkCallback() {
        @Override
        public void onData(byte[] data) {

        }

        @Override
        public void onData(String data) {

        }

        @Override
        public void onAudioFrame(MediaCodec.BufferInfo bufferInfo, ByteBuffer data) {

        }

        @Override
        public void onVideoFrame(MediaCodec.BufferInfo bufferInfo, ByteBuffer data) {

        }

        @Override
        public void onConnectState(ConnectState state) {
            Log.d("cuixiyuan", "onConnectState:" + state);
        }
    };

    private void checkData(byte[] data) {
        Log.e("lfzzz", "onData: lenght --- data --- " + data);
        count++;

        if (count >= 294) {
            Log.e("lfzzz", "onData: 共用时 --- " + (System.currentTimeMillis() - firstTime));
            count = 0;
            firstTime = 0;
            return;
        }

        if (firstTime == 0) {
            firstTime = System.currentTimeMillis();
            temp = firstTime;
            Log.e("lfzzz", "onData: 第" + count + " 帧数据收到时间 --- " + firstTime);
        } else {
            Log.e("lfzzz", "onData: 第" + count + "帧数据收到时间 --- " + System.currentTimeMillis());
            Log.e("lfzzz", "onData: 第 " + count + "帧数据传输用时 ---" + (System.currentTimeMillis() - temp));
            temp = System.currentTimeMillis();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Button start_button = findViewById(R.id.button_stream_server_start);
        start_button.setOnClickListener(this);
        Button stop_button = findViewById(R.id.button_stream_server_stop);
        stop_button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_stream_server_start:
                test = new StreamChannelSink(38888, mCallback);
                sinkIndex = 0;
                break;
            case R.id.button_stream_server_stop:
                if (test != null) {
                    test.close();
                    test = null;
                }
                break;
        }
    }

}
