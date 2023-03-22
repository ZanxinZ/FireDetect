package com.example.firedetect.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.example.firedetect.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.UUID;

public class AcceptThread extends Thread{
    private Context context;
    private Handler editText_out_handler;
    private BluetoothAdapter bluetoothAdapter;
    public BluetoothDevice bluetoothDevice;
    BluetoothSocket socket = null;
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public AcceptThread(Context context, BluetoothDevice device) {
        this.context = context;
        this.editText_out_handler = ((MainActivity)context).editText_out_handler;
        //this.bluetoothAdapter = bluetoothAdapter;
        this.bluetoothDevice = device;

        BluetoothSocket tmp = null;
        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
            Log.d("蓝牙", "connected");

        } catch (Exception e) {

        }
        socket = tmp;

    }
    public void run () {

        Log.d("ddd", "开始监听蓝牙");
        ArrayList<Byte> queue = new ArrayList<>();
        while (true) {
            if (socket == null) {
                try {
                    socket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                } catch (Exception e) {
                    continue;
                }
            }

            String str = null;
            if (socket.isConnected()) {
                //do work
                try {
                    //BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    byte[] buf = new byte[128];
                    int bytes;

                    bytes = socket.getInputStream().read(buf);
                    if (bytes >= 6)
                        str = new String(buf, 0, bytes);
                    else
                        continue;
                    /*byte[] bs = str.getBytes();
                    for (byte b:
                         bs) {
                        queue.add(b);
                    }*/

                } catch (IOException e) {
                    socket = null;
                    Log.d("出错", "读取失败蓝牙连接关闭。");
                }
            } else {
                try {
                    socket.connect();
                    ((MainActivity)context).link_establish_handler.sendEmptyMessage(0);
                } catch (IOException e) {
                    Log.d("蓝牙socket", "fail");
                    break;
                }
            }

            if (str != null) {
                /*
                StringBuilder strBuf = new StringBuilder();

                StringBuilder string = new StringBuilder();
                for (byte b:
                     queue) {
                    string.append((char)b);
                }
                Log.d("ddd", string.toString());
                byte b;
                while (true) {
                    b = queue.get(0);
                    if (queue.size() <= 0) break;
                    if (b == '!') {
                        queue.remove(0);
                        break;
                    }
                    strBuf.append((char) b);
                    queue.remove(0);
                }
                */
                Message msg = new Message();

                msg.obj = str;
                msg.what = 0;
                Log.d("收到：", str);
                editText_out_handler.sendMessage(msg);
            }

//            try {
//                Thread.sleep(1000);
//            } catch (Exception e) {
//
//            }
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }



    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e("蓝牙socket", "Could not close the connect socket", e);
        }
    }
}
