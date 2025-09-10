package com.example.batterymonitor;

import java.io.*;
import java.net.*;
public class TCPClient {
    public void sendMessage(String sever_ip, int battery_Pct, String token ) throws IOException {
        Socket socket = null;
        String serverAddress = sever_ip; // Server IP
        String battery = Integer.toString(battery_Pct);
        try {
            socket = new Socket(serverAddress, 2453);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            out.println(battery);
            out.println(token);
            // System.out.println("Client Send: " + battery);
            socket.close();
        } finally {
            if (socket != null) socket.close();
        }

    } // end sendMessage()

} // end TCPClient
