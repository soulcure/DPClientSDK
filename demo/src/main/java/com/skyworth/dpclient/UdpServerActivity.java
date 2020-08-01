package com.skyworth.dpclient;


import android.media.MediaCodec;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.skyworth.dpclientsdk.ConnectState;
import com.skyworth.dpclientsdk.StreamChannelSink;
import com.skyworth.dpclientsdk.StreamSinkCallback;
import com.skyworth.dpclientsdk.StreamUdpServer;

import java.nio.ByteBuffer;

public class UdpServerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "server";

    private StreamUdpServer udpServer;

    private TextView tv_msg;

    StreamSinkCallback mCallback = new StreamSinkCallback() {
        @Override
        public void onData(byte[] data) {
            Log.d(TAG, "onData:" + data.length);
        }

        @Override
        public void onData(String data) {
            Log.d(TAG, "onData:" + data);
            showUI(data);
        }

        @Override
        public void onAudioFrame(MediaCodec.BufferInfo bufferInfo, ByteBuffer data) {

        }

        @Override
        public void onVideoFrame(MediaCodec.BufferInfo bufferInfo, ByteBuffer data) {

        }

        @Override
        public void onConnectState(ConnectState state) {
            Log.d(TAG, "onConnectState:" + state);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_server);

        tv_msg = findViewById(R.id.tv_msg);
        findViewById(R.id.btn_open).setOnClickListener(this);
        findViewById(R.id.btn_close).setOnClickListener(this);
    }


    private void showUI(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_msg.setText(msg);
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_open:
                udpServer = new StreamUdpServer(39999, mCallback);
                udpServer.open();
                break;
            case R.id.btn_close:
                if (udpServer != null) {
                    udpServer.close();
                    udpServer = null;
                }
                break;
        }
    }

}
