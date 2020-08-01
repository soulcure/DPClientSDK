package com.skyworth.dpclientsdk;

import android.media.MediaCodec;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class StreamUdpClient extends PduUtil implements Runnable {

    private static final String TAG = StreamUdpClient.class.getSimpleName();

    private static final int SOCKET_RECEIVE_BUFFER_SIZE = 1024; //1KB

    private String mAddress;
    private int port;
    private boolean isConnected = false;
    private StreamSourceCallback mStreamSourceCallback;

    private DatagramSocket udpSocket;
    private UdpSendThread mSender;

    /**
     * 发送队列
     */
    private final LinkedBlockingQueue<ByteBuffer> mSendQueue;

    /**
     * 接收buffer
     */
    private byte[] receiveBuffer;

    /**
     * 发送队列
     */

    public StreamUdpClient(String address, int port, StreamSourceCallback callback) {
        Log.d(TAG, "Create UDP Client Task---");
        this.mAddress = address;
        this.port = port;
        mStreamSourceCallback = callback;
        mSendQueue = new LinkedBlockingQueue<>();

        receiveBuffer = new byte[SOCKET_RECEIVE_BUFFER_SIZE];//预存buffer

    }


    public void open() {
        new Thread(this, "udpClient-thread").start();
    }


    /**
     * 发送视频或音频帧
     *
     * @param type   1 video frame ; 2 audio frame
     * @param buffer
     * @return
     */
    public void sendData(byte type, MediaCodec.BufferInfo bufferInfo, ByteBuffer buffer) {
        int length = buffer.remaining();

        ByteBuffer byteBuffer = ByteBuffer.allocate(PduBase.PDU_HEADER_LENGTH + length);
        byteBuffer.clear();

        byteBuffer.putInt(PduBase.pduStartFlag);
        byteBuffer.put(type);
        byteBuffer.putInt(bufferInfo.offset);
        byteBuffer.putInt(bufferInfo.size);
        byteBuffer.putLong(bufferInfo.presentationTimeUs);
        byteBuffer.putInt(bufferInfo.flags);
        byteBuffer.putInt(0);  //reserved
        byteBuffer.putInt(length);
        byteBuffer.put(buffer);

        mSender.send(byteBuffer);
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
        pduBase.pduType = PDU_BYTES;
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

        mSender.send(byteBuffer);
    }


    /**
     * 发送local channel data
     *
     * @return
     */
    public void sendData(String data) {
        byte[] bytes = data.getBytes();
        int length = PduBase.PDU_HEADER_LENGTH + bytes.length;

        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        byteBuffer.clear();

        PduBase pduBase = new PduBase();
        pduBase.pduType = PDU_STRING;
        pduBase.length = bytes.length;
        pduBase.body = bytes;

        byteBuffer.putInt(PduBase.pduStartFlag);
        byteBuffer.put(pduBase.pduType);
        byteBuffer.putInt(pduBase.offset);
        byteBuffer.putInt(pduBase.size);
        byteBuffer.putLong(pduBase.presentationTimeUs);
        byteBuffer.putInt(pduBase.flags);
        byteBuffer.putInt(pduBase.reserved);  //reserved
        byteBuffer.putInt(pduBase.length);
        byteBuffer.put(pduBase.body);

        mSender.send(byteBuffer);

    }

    @Override
    public void OnRec(PduBase pduBase) {

    }

    @Override
    public void OnRec(PduBase pduBase, SocketChannel channel) {

    }

    @Override
    public void run() {
        Log.d(TAG, "create DataSocketClientThread ");
        try {
            socketConnect();
            udpReceive();
        } catch (Exception e) {
            isConnected = false;
            Log.e(TAG, "socket failed on  " + mAddress + ":" + port + "  " + e.toString());
        }

    }//#run


    /**
     * socket receive
     *
     * @throws IOException
     */

    private void udpReceive() throws IOException {
        while (udpSocket.isConnected()) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            udpSocket.receive(receivePacket);

            if (receivePacket.getData() == null) return;

            int size = receivePacket.getLength();     //此为获取后的有效长度

            Log.d(TAG, "receivePacket data size = " + size);
            byte[] dataBuffer = Arrays.copyOf(receiveBuffer, size);
            parsePdu(dataBuffer);

        }

    }


    /**
     * 连接socket
     *
     * @throws IOException
     */
    private void socketConnect() throws IOException {

        InetAddress ipAddress = InetAddress.getByName(mAddress);
        udpSocket = new DatagramSocket();
        udpSocket.connect(ipAddress, port); //连接

        if (udpSocket.isConnected()) {
            Log.d(TAG, "connect socket success ");

            mSender = new UdpSendThread();
            mSender.start();
            isConnected = true;
            mStreamSourceCallback.onConnectState(ConnectState.CONNECT);
        } else {
            Log.e(TAG, "connect socket failed on port :" + port);
            isConnected = false;
            mStreamSourceCallback.onConnectState(ConnectState.ERROR);
        }

    }


    /**
     * 关闭socket
     */
    public void close() {
        udpSocket.close();

        if (mSender != null) {
            mSender.close();
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


    /**
     * socket 发送线程类
     */
    private class UdpSendThread implements Runnable {
        boolean isExit = false;  //是否退出

        /**
         * 发送线程开启
         */
        public void start() {
            Thread thread = new Thread(this);
            thread.setName("tcpSend-thread");
            thread.start();
        }

        public void send(ByteBuffer buffer) {
            synchronized (this) {
                mSendQueue.offer(buffer);
                notify();
            }

        }


        /**
         * 发送线程关闭
         */
        public void close() {
            synchronized (this) { // 激活线程
                isExit = true;
                notify();
            }
        }

        @Override
        public void run() {
            while (!isExit) {
                Log.v(TAG, "tcpClient-thread send loop is running");
                synchronized (mSendQueue) {
                    while (!mSendQueue.isEmpty()
                            && udpSocket != null
                            && udpSocket.isConnected()) {
                        ByteBuffer buffer = mSendQueue.poll();
                        if (buffer == null) {
                            continue;
                        }
                        buffer.flip();
                        Log.v(TAG, "tcp will send buffer to:" + mAddress + ":" + port +
                                "&header:" + buffer.getInt(0) +
                                "&length:" + buffer.getInt(PduBase.PDU_BODY_LENGTH_INDEX));

                        try {
                            InetAddress ipAddress = InetAddress.getByName(mAddress);
                            DatagramPacket sendPacket = new DatagramPacket(buffer.array(), buffer.remaining(), ipAddress, port);
                            udpSocket.send(sendPacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.e("colin", "colin start time06 --- tv Encoder data send finish by socket");
                    }//#while
                }

                synchronized (this) {
                    Log.v(TAG, "tcp send buffer done and wait...");
                    try {
                        wait();// 发送完消息后，线程进入等待状态
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }//#run


    }//# TcpSendThread

}