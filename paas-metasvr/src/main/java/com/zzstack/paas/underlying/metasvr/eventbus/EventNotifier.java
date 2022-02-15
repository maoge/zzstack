package com.zzstack.paas.underlying.metasvr.eventbus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventNotifier implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(EventNotifier.class);

    private static final int FIX_HEAD_LEN = 10;
    private static final int FIX_PREHEAD_LEN = 6;
    private static final byte[] PRE_HEAD = { '$', 'H', 'E', 'A', 'D', ':' };

    private String ip;
    private int port;
    private String msg;

    public EventNotifier(String ip, int port, String msg) {
        this.ip = ip;
        this.port = port;
        this.msg = msg;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(ip, port);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            DataOutputStream dout = new DataOutputStream(out);
            DataInputStream din = new DataInputStream(in);

            int bodyLen = msg.length();
            int len = FIX_HEAD_LEN + bodyLen;
            byte[] sendData = new byte[len];

            prepareData(sendData, msg);

            dout.write(sendData, 0, len);
            dout.flush();

            dout.close();
            out.close();

            din.close();
            in.close();

            socket.close();

        } catch (UnknownHostException e) {
            logger.error("EventNotifier " + ip + ":" + port, e);
        } catch (IOException e) {
            logger.error("EventNotifier " + ip + ":" + port, e);
        }
    }

    void GetIntBytes(byte[] bs, int i) {
        int idx = FIX_PREHEAD_LEN;
        bs[idx++] = (byte) (i & 0xff);
        bs[idx++] = (byte) ((i >>> 8) & 0xff);
        bs[idx++] = (byte) ((i >>> 16) & 0xff);
        bs[idx++] = (byte) ((i >>> 24) & 0xff);
    }

    void prepareData(byte[] sendData, String body) {
        System.arraycopy(PRE_HEAD, 0, sendData, 0, FIX_PREHEAD_LEN);

        GetIntBytes(sendData, body.length());

        byte[] bodyBytes = body.getBytes();
        System.arraycopy(bodyBytes, 0, sendData, FIX_HEAD_LEN, bodyBytes.length);
    }

}
