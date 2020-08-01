package com.skyworth.dpclientsdk;

import android.media.MediaCodec;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class StreamChannelSource implements Runnable {

    private static final String TAG = StreamChannelSource.class.getSimpleName();

    private static final int SOCKET_SEND_BUFFER_SIZE = 5 * 1024 * 1024; //5MB
    private static final int SOCKET_RECEIVE_BUFFER_SIZE = 1024; //1KB

    public enum PduType {
        LocalData, VideoFrame, AudioFrame
    }


    private volatile boolean isExit = false;  //是否退出

    private String mAddress;
    private int port;
    private boolean isConnected = false;
    private StreamSourceCallback mStreamSourceCallback;

    private SocketChannel socketChannel;


    /**
     * 发送队列
     */
    private final LinkedBlockingQueue<ByteBuffer> mSendQueue;


    /**
     * 发送队列
     */

    public StreamChannelSource(String address, int port, StreamSourceCallback callback) {
        Log.d(TAG, "Create Socket Client Task ");
        this.mAddress = address;
        this.port = port;
        mStreamSourceCallback = callback;
        mSendQueue = new LinkedBlockingQueue<>();

        new Thread(this, "tcpClient-thread").start();
    }


    /**
     * 发送视频或音频帧
     *
     * @param type   1 video frame ; 2 audio frame
     * @param buffer
     * @return
     */
    public void sendData(PduType type, MediaCodec.BufferInfo bufferInfo, ByteBuffer buffer) {
        int length = buffer.remaining();
        byte typeValue;
        if (type == PduType.VideoFrame) {
            typeValue = 0x01;
        } else if (type == PduType.AudioFrame) {
            typeValue = 0x02;
        } else {
            typeValue = 0x00;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(PduBase.PDU_HEADER_LENGTH + length);
        byteBuffer.clear();

        byteBuffer.putInt(PduBase.pduStartFlag);
        byteBuffer.put(typeValue);
        byteBuffer.putInt(bufferInfo.offset);
        byteBuffer.putInt(bufferInfo.size);
        byteBuffer.putLong(bufferInfo.presentationTimeUs);
        byteBuffer.putInt(bufferInfo.flags);
        byteBuffer.putInt(0);  //reserved
        byteBuffer.putInt(length);
        byteBuffer.put(buffer);

        synchronized (this) {
            mSendQueue.offer(byteBuffer);
            notify();
        }
    }


    /**
     * 发送local channel data
     *
     * @return
     */
    public void sendData(byte[] data) {
        int length = PduBase.PDU_HEADER_LENGTH + data.length;

        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        byteBuffer.clear();

        PduBase pduBase = new PduBase();
        pduBase.pduType = 0;
        pduBase.length = data.length;
        pduBase.body = data;

        byteBuffer.putInt(PduBase.pduStartFlag);
        byteBuffer.put(pduBase.pduType);
        byteBuffer.putInt(pduBase.offset);
        byteBuffer.putInt(pduBase.size);
        byteBuffer.putLong(pduBase.presentationTimeUs);
        byteBuffer.putInt(pduBase.flags);
        byteBuffer.putInt(pduBase.reserved);  //reserved
        byteBuffer.putInt(pduBase.length);
        byteBuffer.put(pduBase.body);

        synchronized (this) {
            mSendQueue.offer(byteBuffer);
            notify();
        }
    }


    @Override
    public void run() {
        Log.d(TAG, "create DataSocketClientThread ");
        try {
            socketConnect();
            sendLoop();
        } catch (Exception e) {
            isConnected = false;
            Log.e(TAG, "socket failed on  " + mAddress + ":" + port + "  " + e.toString());
        }

    }//#run


    /**
     * 连接socket
     *
     * @throws IOException
     */
    private void socketConnect() throws Exception {
        SocketAddress isa = new InetSocketAddress(InetAddress.getByName(mAddress), port);
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.socket().setSendBufferSize(SOCKET_SEND_BUFFER_SIZE);
        socketChannel.socket().setReceiveBufferSize(SOCKET_RECEIVE_BUFFER_SIZE);
        socketChannel.socket().setKeepAlive(true);
        //socketChannel.socket().setReuseAddress(false);
        socketChannel.socket().setSoLinger(false, 0);
        socketChannel.socket().setSoTimeout(5);  //超时5秒
        //socketChannel.socket().setTcpNoDelay(true);
        socketChannel.connect(isa);

        while (!socketChannel.finishConnect()) {  //非阻塞模式,必需设置
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e(TAG, "socket connect" + e.toString());
            }
        }

        if (socketChannel.isConnected()) {
            Log.d(TAG, "connect socket success ");
            isConnected = true;
            mStreamSourceCallback.onConnectState(ConnectState.CONNECT);
        } else {
            Log.e(TAG, "connect socket failed on port :" + port);
            isConnected = false;
            mStreamSourceCallback.onConnectState(ConnectState.ERROR);
        }

    }


    private void sendLoop() throws Exception {
        while (!isExit) {
            Log.v(TAG, "tcpClient-thread send loop is running");

            synchronized (mSendQueue) {
                while (!mSendQueue.isEmpty()
                        && socketChannel != null
                        && socketChannel.isConnected()) {
                    ByteBuffer buffer = mSendQueue.poll();
                    if (buffer == null) {
                        continue;
                    }
                    buffer.flip();
                    Log.v(TAG, "tcp will send buffer to:" + mAddress + ":" + port +
                            "&header:" + buffer.getInt(0) +
                            "&length:" + buffer.getInt(PduBase.PDU_BODY_LENGTH_INDEX));

                    if (buffer.remaining() > 0) {
                        int count;
                        while (buffer.hasRemaining() && (count = socketChannel.write(buffer)) > 0) {
                            Log.v(TAG, "tcp send buffer count:" + count);
                            Log.e("colin", "colin start time06 --- tv Encoder data send finish by socket");
                        }
                    }
                    //Thread.sleep(1);  //增加发送速度，不设置终端

                }//#while
            }

            synchronized (this) {
                Log.v(TAG, "tcp send buffer done and wait...");
                wait();// 发送完消息后，线程进入等待状态
            }
        }
    }


    /**
     * 关闭socket
     */
    public void close() {
        synchronized (this) {
            isExit = true;
            notify();
        }

        try {
            if (socketChannel != null) {
                socketChannel.close();
                socketChannel = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "socket close error :" + e.toString());
        }

        isConnected = false;
        mSendQueue.clear();
    }


    /**
     * Socket连接是否是正常的
     *
     * @return 是否连接
     */
    public boolean isOpen() {
        return isConnected;
    }


}