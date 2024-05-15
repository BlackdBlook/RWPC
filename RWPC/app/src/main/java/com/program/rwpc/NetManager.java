package com.program.rwpc;

import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetManager {
    private final DatagramSocket socket;

    ExecutorService executor = Executors.newFixedThreadPool(5);

    private Thread recvThread;
    
    Handler handler;
    
    public NetManager(Handler handler) {
        this.handler = handler;
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
            StrictMode.setThreadPolicy(policy);
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(String HostName, int Port, String message) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                InetAddress address = null;
                try {
                    address = InetAddress.getByName(HostName);
                } catch (UnknownHostException e) {
                    Message msg = new Message();
                    msg.obj = e.getMessage();
                    handler.sendMessage(msg);
                }
                byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, Port);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    Message msg = new Message();
                    msg.obj = e.getMessage();
                    handler.sendMessage(msg);
                }

                StringBuilder stringBuilder = new StringBuilder();
                assert address != null;
                stringBuilder.append(address.getHostAddress());
                stringBuilder.append(':');
                stringBuilder.append(Port);
                stringBuilder.append('#');
                stringBuilder.append(message);
                stringBuilder.append('\n');
                
                Message msg = new Message();
                msg.obj = stringBuilder;
                handler.sendMessage(msg);
            }
        });
    }

    public String recv(String HostName, int Port) throws IOException {
        byte[] buffer = new byte[1024];
        InetAddress address = InetAddress.getByName(HostName); 
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, Port);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    }

    public void beginCheckSocketData() {
        recvThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (!Thread.currentThread().isInterrupted()) {
                    // 设置socket超时时间为1秒
                    socket.setSoTimeout(1000);
                    try {
                        // 尝试接收数据
                        socket.receive(packet);

                        Message message = new Message();
                        message.obj = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                        message.arg1 = 1;
                        handler.sendMessage(message);
                        
                    } catch (SocketTimeoutException e) {
                        // 如果抛出了SocketTimeoutException，说明在超时时间内没有接收到数据，继续循环
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        recvThread.start();
    }



    private String getAddress(String HostName) {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(HostName);
        } catch (Exception e) {
            return "Unknow Address";
        }
        return address.getHostAddress();
    }
    
    public void popupAddress(String HostNames){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.obj = getAddress(HostNames);
                handler.sendMessage(msg);
            }
        });
    }
}