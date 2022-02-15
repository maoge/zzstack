package com.zzstack.paas.underlying.metasvr.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmsWebConsoleConnector {
    
    private static Logger logger = LoggerFactory.getLogger(SmsWebConsoleConnector.class);

    private final String ip;
    private final int port;
    private byte[] dest;
    
    @SuppressWarnings("unused")
    private final String passwd;
    
    private Socket socket;
    
    private final int CONN_TIMEOUT = 100;
    private final int READ_TIMEOUT = 200;

    public SmsWebConsoleConnector(String ip, int port, String passwd) {
        super();
        this.ip = ip;
        this.port = port;
        this.passwd = passwd;
    }

    public void connect() throws IOException {
        if (socket == null) {
            socket = new Socket();
            socket.setSoTimeout(READ_TIMEOUT);
            socket.connect(new InetSocketAddress(ip, port), CONN_TIMEOUT);
        }
    }

    public boolean sendData(String[] message) throws Exception {
        boolean res = false;
        try {
            OutputStream os = socket.getOutputStream();
            // InputStream in = socket.getInputStream();
            PrintWriter pw = new PrintWriter(os);
            String messages = String.join("\t", message) + "\r\n";
            messages = new String(messages.getBytes(), Charset.forName("GBK"));
            pw.write(messages);
            pw.flush();
            
            // byte[] src = new byte[64] ;
            // int len = in.read(src);
            // if (len > 0) {
            //     dest = new byte[len];
            //     System.arraycopy(src, 0, dest, 0, len);
            // }
            
            res = true;
        } catch (Exception e) {
            throw e;
        }
        
        return res;
    }

    public String getResponse() {
        return new String(dest);
    }

    public void close() throws IOException {
        if (socket != null && socket.isConnected()) {
            socket.close();
        }
    }

    public static void main(String[] args) {
        try {
            String ip = "172.20.0.207";
            int port = 5104;
            // int port = 5003;
            String password = "";
            
            // String[] pingParams = new String[]{"PING", "PONG"};
            // String[] dbTypeParams = new String[]{"SwitchDBType", "master", "redis"};
            
            // String[] redisWeightParams = new String[]{"AdjustRedisWeight", "A:10,B:90"};
            String[] pingParams = new String[]{"PING"};
            long beg = System.currentTimeMillis();
            
            SmsWebConsoleConnector smsWebConsoleConnector = new SmsWebConsoleConnector(ip, port, password);
            smsWebConsoleConnector.connect();
            smsWebConsoleConnector.sendData(pingParams);
            smsWebConsoleConnector.close();
            long end = System.currentTimeMillis();
            
            // System.out.println("<" + smsWebConsoleConnector.getResponse() + ">");
            System.out.println("cost:" + (end - beg));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
