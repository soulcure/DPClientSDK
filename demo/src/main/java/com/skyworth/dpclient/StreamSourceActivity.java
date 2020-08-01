package com.skyworth.dpclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.skyworth.dpclientsdk.ConnectState;
import com.skyworth.dpclientsdk.StreamChannelSource;
import com.skyworth.dpclientsdk.StreamSinkCallback;
import com.skyworth.dpclientsdk.StreamSourceCallback;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class StreamSourceActivity extends AppCompatActivity implements View.OnClickListener {
    private StreamChannelSource test2;
    private boolean mStop;
    private StreamSourceCallback callback;

    private static final int HANDLER_THREAD_SEND_DATA = 100;

    private boolean isConnect = true;
    private int connectCount = 0;

    EditText input;
    private ProcessHandler mProcessHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);
        initHandler();

        input = findViewById(R.id.ip_edit);
        //input.setText(DeviceUtil.getLocalIPAddress(this));
        input.setText("192.168.137.198");


        Button start = findViewById(R.id.button_stream_client_start);
        start.setOnClickListener(this);
        Button stop = findViewById(R.id.button_stream_client_stop);
        stop.setOnClickListener(this);
        callback = new StreamSourceCallback() {
            @Override
            public void onConnectState(ConnectState state) {
                if (state == ConnectState.CONNECT) {
                    mProcessHandler.sendEmptyMessage(HANDLER_THREAD_SEND_DATA);

                    //byte[] bytes = "幼儿园".getBytes();
                    //test2.sendData(bytes);
                }
                Log.d("cuixiyuan", "StreamSourceState: " + state);
            }
        };
    }


    /**
     * 线程初始化
     */
    private void initHandler() {
        if (mProcessHandler == null) {
            HandlerThread handlerThread = new HandlerThread(
                    "handler looper Thread");
            handlerThread.start();
            mProcessHandler = new ProcessHandler(handlerThread.getLooper());
        }
    }


    /**
     * 子线程handler,looper
     *
     * @author Administrator
     */
    private class ProcessHandler extends Handler {

        public ProcessHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_THREAD_SEND_DATA:
                    break;
                default:
                    break;
            }

        }

    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_stream_client_start:
                String ip = input.getText().toString();
                //发送命令
                //实力化一个命令发送端，参数为接受端的IP地址和端口号。这个会和发现服务（云端or局域网）对接，由发现服务提供
                test2 = new StreamChannelSource(ip, 38888, callback);
                mStop = false;

                //客户端没有连接状态回调，测试发现刚连上就开始send会有问题，这个需要继续优化
                break;
            case R.id.button_stream_client_stop:
                mStop = true;
                test2.close();
                break;
        }
    }

    private byte[] InputStream2ByteArray(String filePath) throws IOException {

        InputStream in = new FileInputStream(filePath);
        byte[] data = toByteArray(in);
        in.close();

        return data;
    }

    private byte[] toByteArray(InputStream in) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[(1460 - 8) * 10];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }


}
