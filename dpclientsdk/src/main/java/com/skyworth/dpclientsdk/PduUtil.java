package com.skyworth.dpclientsdk;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public abstract class PduUtil {

    private static final String TAG = PduUtil.class.getSimpleName();

    public static final byte PDU_BYTES = 0x00;
    public static final byte PDU_STRING = 0x01;
    public static final byte PDU_VIDEO = 0x02;
    public static final byte PDU_AUDIO = 0x03;
    public static final byte PDU_PING = 0x0E;
    public static final byte PDU_PONG = 0x0F;


    public abstract void OnRec(PduBase pduBase);

    public abstract void OnRec(PduBase pduBase, SocketChannel channel);


    public int parsePdu(ByteBuffer buffer, SocketChannel channel) {

        if (buffer.limit() > PduBase.PDU_BASIC_LENGTH) {
            int begin = buffer.getInt(0);
            Log.v(TAG, "pdu begin is " + begin);
            if (begin != PduBase.pduStartFlag) {
                Log.e(TAG, "pdu header error buffer limit:" + buffer.limit());
                buffer.clear();
                return -1;
            }
        } else {    //did not contain a start flag yet.continue read.
            Log.v(TAG, "did not has full start flag");
            buffer.position(buffer.limit());
            buffer.limit(buffer.capacity());
            return 0;
        }

        if (buffer.limit() >= PduBase.PDU_HEADER_LENGTH) {
            //has full header
            int bodyLength = buffer.getInt(PduBase.PDU_BODY_LENGTH_INDEX);
            int totalLength = bodyLength + PduBase.PDU_HEADER_LENGTH;

            if (totalLength <= buffer.limit()) {
                //has a full pack.
                byte[] packByte = new byte[totalLength];
                buffer.get(packByte);
                PduBase pduBase = buildPdu(packByte);
                OnRec(pduBase, channel);
                buffer.compact();
                //read to read.
                buffer.flip();

                Log.v(TAG, "pdu read is totalLength:" + totalLength);
                return totalLength;

            } else {
                buffer.position(buffer.limit());
                buffer.limit(buffer.capacity());
                return 0;
            }

        } else {
            Log.v(TAG, " not a full header");
            buffer.position(buffer.limit());
            buffer.limit(buffer.capacity());
            return -2;
        }
    }

    public int parsePdu(ByteBuffer buffer) {
        if (buffer.limit() > PduBase.PDU_BASIC_LENGTH) {
            int begin = buffer.getInt(0);
            Log.v(TAG, "pdu begin is " + begin);
            if (begin != PduBase.pduStartFlag) {
                Log.e(TAG, "pdu header error buffer limit:" + buffer.limit());
                buffer.clear();
                return -1;
            }
        } else {    //did not contain a start flag yet.continue read.
            Log.v(TAG, "did not has full start flag");
            buffer.position(buffer.limit());
            buffer.limit(buffer.capacity());
            return 0;
        }

        if (buffer.limit() >= PduBase.PDU_HEADER_LENGTH) {
            //has full header
            int bodyLength = buffer.getInt(PduBase.PDU_BODY_LENGTH_INDEX);
            int totalLength = bodyLength + PduBase.PDU_HEADER_LENGTH;

            if (totalLength <= buffer.limit()) {
                //has a full pack.
                byte[] packByte = new byte[totalLength];
                buffer.get(packByte);
                PduBase pduBase = buildPdu(packByte);
                OnRec(pduBase);
                buffer.compact();
                //read to read.
                buffer.flip();

                Log.v(TAG, "pdu read is totalLength:" + totalLength);
                return totalLength;

            } else {
                buffer.position(buffer.limit());
                buffer.limit(buffer.capacity());
                return 0;
            }

        } else {
            Log.v(TAG, " not a full header");
            buffer.position(buffer.limit());
            buffer.limit(buffer.capacity());
            return -2;
        }
    }


    public void parsePdu(byte[] packByte) {
        //has a full pack.
        int totalLength = packByte.length;
        PduBase pduBase = buildPdu(packByte);
        OnRec(pduBase);
        Log.v(TAG, "pdu read is totalLength:" + totalLength);
    }


    private PduBase buildPdu(byte[] bytes) {
        PduBase units = new PduBase();
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();

        buffer.getInt();  //units.flag
        units.pduType = buffer.get();
        units.offset = buffer.getInt();
        units.size = buffer.getInt();
        units.presentationTimeUs = buffer.getLong();
        units.flags = buffer.getInt();
        units.reserved = buffer.getInt();
        int length = buffer.getInt();
        units.length = length;
        units.body = new byte[length];
        buffer.get(units.body);
        return units;
    }


}
