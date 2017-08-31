package com.example.luc.controlbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by LUC on 2017-8-23.
 */

public class ConnectBluetooth extends Activity {
    ListView listView;

    public static final int NOTICE_VIEW = 1;
    final private static int MESSAGE_READ = 100;
    public static final int RECV_VIEW = 0;
    int i = 0;

    BluetoothAdapter bluetoothAdapter;
    ArrayList<BluetoothDevice> devices;
    public static BluetoothSocket btSocket;
    ArrayList<String> deviceArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blue_tooth_connect);

        listView = (ListView)findViewById(R.id.list);
        deviceArray = new ArrayList<>();

        // 获取BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(ConnectBluetooth.this,"Device does not support Bluetooth",
                    Toast.LENGTH_SHORT).show();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(ConnectBluetooth.this, "蓝牙开启成功", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
            Toast.makeText(ConnectBluetooth.this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
        }
        //通过适配器获取蓝牙设备的名称
        bluetoothAdapter.startDiscovery();
        Set<BluetoothDevice> deviceList = bluetoothAdapter.getBondedDevices();
        for(BluetoothDevice dev:deviceList){
            deviceArray.add(dev.getName()+'\n'+dev.getAddress());
        }
        listView.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, deviceArray));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = listView.getItemAtPosition(position).toString();
                String macAdrress = item.split("\n")[1];
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAdrress);
                try {
                    Method clientMethod = device.getClass()
                            .getMethod("createRfcommSocket", new Class[]{int.class});
                    btSocket = (BluetoothSocket) clientMethod.invoke(device, 1);
                    connect(btSocket);//连接设备

                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    Log.e("crash","down");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    Log.e("crash","ret");
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    Log.e("crash","tur");
                }
            }
        });

    }


    /**
     * 连接蓝牙及获取数据
     */
    public void connect(final BluetoothSocket btSocket) {
        try {
            btSocket.connect();//连接
            if (btSocket.isConnected()) {
                Log.e("----connect--- :", "连接成功");
                Toast.makeText(getApplicationContext(), "蓝牙连接成功", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent();
                intent.setClass(ConnectBluetooth.this, HomePage.class);
                startActivity(intent);

            } else {
                Toast.makeText(getApplicationContext(), "蓝牙连接失败", Toast.LENGTH_SHORT).show();
                btSocket.close();
                listView.setVisibility(View.VISIBLE);
                Log.e("--------- :", "连接关闭");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("crash","frit");
        }

    }
//
//    private class ConnetThread extends Thread {
//        public void run() {
//
//
//        }
//
//    }


//    private Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case MESSAGE_READ:
//                    String result = (String) msg.obj;
//                    String data = result.split("\\r\\n")[0];
//                    Log.e("----data：----- :", ">>>" + data);
//                    if (i < 6) {
//                        Editable text = (Editable) btAllData.getText();
//                        text.append(data);
//                        btAllData.setText(text + "\r\n");
//                        i++;
//                    } else {
//                        btAllData.setText(data + "\r\n");
//                        i = 0;
//                    }
//                    break;
//            }
//        }
//    };
}
