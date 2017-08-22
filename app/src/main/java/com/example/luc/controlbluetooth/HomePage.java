package com.example.luc.controlbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static android.widget.AdapterView.*;

public class HomePage extends AppCompatActivity {

    private ArrayAdapter<String> adapter = null;
    private static final String [] Switch ={"On","Off"};
    public static final int INFO_ID = 23434;
    private boolean isRunning = true;

    private TextView Temp;
    private TextView Humid;
    private Spinner on_or_off;

    private Button openBluetooth;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private BroadcastReceiver rec;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        on_or_off = (Spinner) findViewById(R.id.spinner);
        Temp = (TextView) findViewById(R.id.temp);
        Humid = (TextView) findViewById(R.id.humidity_show);

        // 获取BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(HomePage.this,"Device does not support Bluetooth",Toast.LENGTH_SHORT).show();
            return;
        }
        //注册监听事件
        Temp = (TextView) findViewById(R.id.temp_table);
        Humid = (TextView) findViewById(R.id.humidity_show);

        //设置下拉列表风格
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Switch);
        //将适配器添加到spinner中去
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        on_or_off.setAdapter(adapter);
        on_or_off.setVisibility(View.VISIBLE);//设置默认显示
        //设置选项
        on_or_off.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        //使用按钮打开蓝牙开关，并选择连接设备
        openBluetooth = (Button) findViewById(R.id.openBluetooth);
        openBluetooth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(HomePage.this, "蓝牙开启成功", Toast.LENGTH_SHORT).show();
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                } else {
                    Toast.makeText(HomePage.this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
                }
                //从找到的设备中连接
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice dev : pairedDevices) {
                        device = dev;
                    }
                }
                connectThread =  new ConnectThread(device);
                connectThread.start();
            }
        });
    }

    @Override
    protected void onPause(){
        isRunning = false;
        super.onPause();
    }
    @Override
    protected void onResume(){
        isRunning = false;
        super.onResume();
    }

    //开启蓝牙需要新的线程
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString(
                "00001101-0000-1000-8000-00805f9b34fb");


        private ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            this.mmDevice = device;
            try {
                tmp = this.mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.mmSocket = tmp;
        }
        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                this.mmSocket.connect();
                connectedThread  = new ConnectedThread(this.mmSocket);
                connectedThread.start();
            } catch (IOException connectException) {
                try {
                    this.mmSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
                return;
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //用于沟通单片机和Android设备
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            while (true) {
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for(int i = begin; i < bytes; i++) {
                        if(buffer[i] == "#".getBytes()[0]) {
                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if(i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    //将Connectedthread中的数据跨线程传输到UIThread中去，利用handler进行通信
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;
            switch(msg.what) {
                case 1:
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    break;
            }
        }
    };

}
