package com.skyworth.dpclientsdk;

import android.media.MediaCodec;
import android.util.Log;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;


public class StreamChannelSink extends PduUtil implements Runnable {

    private static final String TAG = StreamChannelSink.class.getSimpleName();

    private static final int BUFFER_SIZE = 5 * 1024 * 1024; //5MB

    private volatile boolean isExit = false;

    private Selector selector;
    private StreamSinkCallback mCallback;
    private int port;
    private ServerSocketChannel listenerChannel;

    private ProcessHandler processHandler;  //子线程Handler

    public StreamChannelSink(int port, StreamSinkCallback callback) {
        this.port = port;
        this.mCallback = callback;

        processHandler = new ProcessHandler("draw-surface", true);

        new Thread(this, "tcpServer-thread").start();
    }


    /**
     * 关闭tcp server
     */
    public void close() {
        try {
            isExit = true;
            selector.close();
            listenerChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "close tcp server error:" + e.toString());
        }
    }


    @Override
    public void run() {
        try {
            tcpServerStart();
        } catch (BindException e) {
            Log.d(TAG, "TcpServer listen:" + e.toString());
            if (mCallback != null) {
                mCallback.onConnectState(ConnectState.CONNECT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "TcpServer listen:" + e.toString());
            if (mCallback != null) {
                mCallback.onConnectState(ConnectState.ERROR);
            }
        }
    }


    @Override
    public void OnRec(PduBase pduBase) {

    }

    @Override
    public void OnRec(final PduBase pduBase, SocketChannel channel) {
        if (mCallback != null) {
            processHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (pduBase.pduType == 0) {
                        byte[] cmd = pduBase.body;
                        Log.d(TAG, "TcpServer local OnRec:" + new String(cmd));
                        mCallback.onData(cmd);
                    } else if (pduBase.pduType == 1) {
                        Log.d(TAG, "TcpServer OnRec videoFrame size:" + pduBase.size);
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        bufferInfo.set(pduBase.offset, pduBase.size, pduBase.presentationTimeUs, pduBase.flags);

                        ByteBuffer byteBuffer = ByteBuffer.wrap(pduBase.body);
                        mCallback.onVideoFrame(bufferInfo, byteBuffer);

                    } else if (pduBase.pduType == 2) {
                        Log.d(TAG, "TcpServer OnRec audioFrame size:" + pduBase.size);
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        bufferInfo.set(pduBase.offset, pduBase.size, pduBase.presentationTimeUs, pduBase.flags);

                        ByteBuffer byteBuffer = ByteBuffer.wrap(pduBase.body);
                        mCallback.onVideoFrame(bufferInfo, byteBuffer);
                    }
                }

            });
        }
    }


    private void tcpServerStart() throws Exception {
        // 创建选择器
        selector = Selector.open();
        // 打开监听信道
        listenerChannel = ServerSocketChannel.open();

        listenerChannel.socket().setReuseAddress(true);
        // 与本地端口绑定
        listenerChannel.socket().bind(new InetSocketAddress(port));

        listenerChannel.configureBlocking(false);
        // 注册到Selector中，ACCEPT操作
        listenerChannel.register(selector, SelectionKey.OP_ACCEPT);

        Log.d(TAG, "tcp server bind to port:" + port);

        if (mCallback != null) {
            mCallback.onConnectState(ConnectState.CONNECT);
        }

        // 不断轮询Selector
        while (!isExit) {
            // 当准备好的通道大于0才有往下的操作
            if (selector.select() > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    // 处理过的key要移除掉
                    iterator.remove();
                    // 接收状态
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    // 可读状态
                    if (key.isReadable()) {
                        handleRead(key);
                    }

                }
            }
        }
    }


    /**
     * 客户端连接到来
     *
     * @param key
     * @throws Exception
     */
    private void handleAccept(SelectionKey key) throws Exception {
        Log.d(TAG, "tcp server handleAccept:" + key.channel().isOpen());
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        // 获取客户端链接，并注册到Selector中
        SocketChannel clientChannel = serverSocketChannel.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            // 讲通道注册到Selector里头，然后设置为读操作
            clientChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_SIZE));
            //clientChannel.register(key.selector(), SelectionKey.OP_READ);
        }

    }


    /**
     * 客户端发送到的数据可读
     *
     * @param key
     * @throws Exception
     */
    private void handleRead(SelectionKey key) throws Exception {
        Log.d(TAG, "tcp server handleRead read to ByteBuffer...");

        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = (ByteBuffer) key.attachment();

        long byteRead = 0;

        try {
            byteRead = clientChannel.read(byteBuffer);
        } catch (Exception exception) {
            byteRead = -1;
        }

        if (byteRead == -1) { //客户端关闭了socket
            // 没有读取到内容的情况
            clientChannel.close();
            key.cancel();
        } else {
            // 将缓冲区准备为数据传出状态
            byteBuffer.flip();
            int readResult = 0;
            while ((readResult = parsePdu(byteBuffer, clientChannel)) > 0) {
                //loop parse
                Log.d(TAG, "socket read length:" + readResult);
            }
            //判断起始标记
            key.interestOps(SelectionKey.OP_READ);
        }
    }
}






