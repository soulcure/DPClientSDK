package com.skyworth.dpclientsdk;

import android.media.MediaCodec;
import android.util.Log;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class UdpServer extends PduUtil implements Runnable {

    private static final String TAG = UdpServer.class.getSimpleName();

    private static final int BUFFER_SIZE = 2 * 1024 * 1024; //2MB

    private volatile boolean isExit = false;

    private StreamSinkCallback mCallback;

    private DatagramSocket udpSocket;
    private int port;

    private boolean isOpen = false;

    private ProcessHandler processHandler;  //子线程Handler

    public UdpServer(int port, StreamSinkCallback callback) {
        this.port = port;
        this.mCallback = callback;

        processHandler = new ProcessHandler("draw-surface", true);
    }

    /**
     * 打开 udp server
     */
    public void open() {
        new Thread(this, "udpServer-thread").start();
    }


    /**
     * 关闭 udp server
     */
    public void close() {
        isExit = true;
        isOpen = false;
        udpSocket.close();
    }


    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void run() {
        try {
            udpServerStart();
        } catch (Exception e) {
            isOpen = false;
            e.printStackTrace();
            Log.e(TAG, "UdpServer listen:" + e.toString());
            if (mCallback != null) {
                mCallback.onConnectState(ConnectState.ERROR);
            }
        }
    }


    @Override
    public void OnRec(PduBase pduBase, SocketChannel channel) {

    }


    @Override
    public void OnRec(final PduBase pduBase) {
        if (mCallback != null) {
            processHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (pduBase.pduType == PDU_BYTES) {
                        byte[] cmd = pduBase.body;
                        Log.d(TAG, "UdpServer local OnRec byte length:" + cmd);
                        mCallback.onData(cmd);
                    } else if (pduBase.pduType == PDU_STRING) {
                        byte[] cmd = pduBase.body;
                        String msg = new String(cmd);
                        Log.d(TAG, "UdpServer local OnRec String:" + msg);
                        mCallback.onData(msg);
                    } else if (pduBase.pduType == PDU_VIDEO) {
                        Log.d(TAG, "UdpServer OnRec videoFrame size:" + pduBase.size);
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        bufferInfo.set(pduBase.offset, pduBase.size, pduBase.presentationTimeUs, pduBase.flags);

                        ByteBuffer byteBuffer = ByteBuffer.wrap(pduBase.body);
                        mCallback.onVideoFrame(bufferInfo, byteBuffer);

                    } else if (pduBase.pduType == PDU_AUDIO) {
                        Log.d(TAG, "UdpServer OnRec audioFrame size:" + pduBase.size);
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        bufferInfo.set(pduBase.offset, pduBase.size, pduBase.presentationTimeUs, pduBase.flags);

                        ByteBuffer byteBuffer = ByteBuffer.wrap(pduBase.body);
                        mCallback.onVideoFrame(bufferInfo, byteBuffer);
                    }
                }

            });
        }
    }


    private void udpServerStart() throws Exception {
        udpSocket = new DatagramSocket(port);
        Log.d(TAG, "UdpServer bind to port:" + port);
        isOpen = true;

        if (mCallback != null) {
            mCallback.onConnectState(ConnectState.CONNECT);
        }

        while (!isExit) {
            byte[] container = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(container, container.length);

            udpSocket.receive(packet);  // blocks until a packet is received
            byte[] buffer = packet.getData();  //read buffer

            parsePdu(buffer);
        }
    }


}






