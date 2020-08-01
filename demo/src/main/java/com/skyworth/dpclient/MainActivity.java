package com.skyworth.dpclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_websocket).setOnClickListener(this);
        findViewById(R.id.btn_tcp).setOnClickListener(this);
        findViewById(R.id.btn_udp).setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_websocket:
                startActivity(new Intent(this, StreamSourceActivity.class));
                break;
            case R.id.btn_tcp:
                startActivity(new Intent(this, UdpClientActivity.class));
                break;
            case R.id.btn_udp:
                startActivity(new Intent(this, UdpServerActivity.class));
                break;
        }
    }
}
