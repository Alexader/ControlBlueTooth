package com.example.luc.controlbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Set;

import static android.widget.AdapterView.*;

public class HomePage extends AppCompatActivity {

    ArrayAdapter<String> adapter = null;
    ArrayAdapter<String> listAdapter;
    private static final String[] Switch = {"On", "Off"};
    public final int MESSAGE_READ = 1;
    public final int READ_TEMP = 2;


    Spinner on_or_off;
    ListView listView;
    TextView info;
    TextView showTmep;
    TextView showHumid;
    EditText setTemp;
    EditText setHumid;

    Button openBluetooth;
    Button ensure_temp;
    Button ensure_humid;
    Button show_value;

    BluetoothAdapter bluetoothAdapter;
    public static BluetoothSocket btSocket;
    ArrayList<String> deviceArray;

    OutputStream output;
    InputStream input;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_READ: {
                    String result = msg.getData().getString("humid");
                    showHumid.setText(result);

                }
                case READ_TEMP:{
                    String temp = msg.getData().getString("temp");
                    showTmep.setText(temp);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        on_or_off = (Spinner) findViewById(R.id.spinner);
        showTmep = (TextView) findViewById(R.id.temp_table);
        showHumid = (TextView) findViewById(R.id.humidity_show);

        //注册监听事件，显示温湿度的元素
        setTemp = (EditText) findViewById(R.id.temp_value);
        setHumid = (EditText) findViewById(R.id.humid_value);

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
                switch (arg2) {
                    case 0: {
                        try {
                            if(btSocket==null)
                                makeToast("还未连接蓝牙");
                            else {
                                output = btSocket.getOutputStream();
                                output.write("o".getBytes());//选择第一个下拉列表则向单片机发送open风扇额信号
                            }
                        } catch (IOException io){
                            io.printStackTrace();
                        }
                    }
                    case 1: {
                        try {
                            if(btSocket==null)
                                makeToast("还未连接蓝牙");
                            else {
                                output = btSocket.getOutputStream();
                                output.write("c".getBytes());//选择第一个下拉列表则向单片机发送close风扇额信号
                            }
                        } catch (IOException io){
                            io.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        listView = (ListView) findViewById(R.id.list);
        deviceArray = new ArrayList<>();
        info = (TextView) findViewById(R.id.info);

        listView.setEmptyView(info);
        listAdapter = new ArrayAdapter<String>(HomePage.this,
                android.R.layout.simple_list_item_1, deviceArray);
        listView.setAdapter(listAdapter);


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
                    Log.e("crash", "down");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    Log.e("crash", "ret");
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    Log.e("crash", "tur");
                }
            }
        });

        // 获取BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            makeToast("Device does not support Bluetooth");
        }

        if (!bluetoothAdapter.isEnabled()) {
            makeToast("蓝牙开启成功");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
            makeToast("蓝牙已开启");
        }

        //搜索并连接蓝牙，更新listview中的内容
        openBluetooth = (Button) findViewById(R.id.openBluetooth);
        try {
            openBluetooth.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    //通过适配器获取蓝牙设备的名称
                    bluetoothAdapter.startDiscovery();
                    Set<BluetoothDevice> deviceList = bluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice dev : deviceList) {
                        deviceArray.add(dev.getName() + '\n' + dev.getAddress());
                    }
                    listAdapter.notifyDataSetChanged();

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        show_value = (Button) findViewById(R.id.show_value);
        show_value.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new ConnectedThread(btSocket).start();
//                new ConnectedThread1(btSocket).start();
            }
        });


        //向单片机发送温度数据
        ensure_temp = (Button) findViewById(R.id.button1);
        ensure_temp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String send_data = setTemp.getText().toString()+'t';
                try {
                    OutputStream output = btSocket.getOutputStream();
                    output.write(send_data.getBytes());
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

        });

        //向单片机发送湿度数据
        ensure_humid = (Button) findViewById(R.id.button2);
        ensure_humid.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String send_data = setTemp.getText().toString()+'h';
                try {
                    OutputStream output = btSocket.getOutputStream();
                    output.write(send_data.getBytes());
                } catch (IOException e){
                    e.printStackTrace();

                }
            }

        });

    }

    //用于提醒用户
    private void makeToast(String msg){
        Toast.makeText(HomePage.this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 连接蓝牙及获取数据
     */
    public void connect(final BluetoothSocket btSocket) {
        try {
            makeToast("正在连接，请稍等");
            btSocket.connect();//连接
            if (btSocket.isConnected()) {
                Log.e("----connect--- :", "连接成功");
                makeToast("蓝牙连接成功");

            } else {
                makeToast("蓝牙连接失败");
                btSocket.close();
                listView.setVisibility(View.VISIBLE);
                Log.e("--------- :", "连接关闭");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("crash", "crack");
        }

    }

    private void deal(OutputStream outputStream, BufferedInputStream inputStream,
                                   String sendText, String id, int msg){
        String recText;
        synchronized(outputStream) {
            try {
                byte[] buff = new byte[100];
                int bytes;
                //读取湿度数据
                outputStream.write(sendText.getBytes());
                bytes = inputStream.read(buff);
                recText = new String(buff, "ISO-8859-1");
                recText = recText.substring(0, bytes);
                Bundle bundle = new Bundle();
                bundle.putString(id, recText);
                Message message = Message.obtain();
                message.what = msg;
                message.setData(bundle);
                handler.sendMessage(message);
                Thread.sleep(200);//显示数据太快看着不舒服，单片机发送数据也跟不上
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    // 客户端与服务器建立连接成功后，用ConnectedThread收取湿度数据
    private class ConnectedThread extends Thread{
        private BluetoothSocket socket;
        private OutputStream outputStream;
        private BufferedInputStream inputStream;

        ConnectedThread(BluetoothSocket soc){
            this.socket = soc;
            this.outputStream = null;
            this.inputStream = null;
    }
    @Override
    public void run(){
        String recText;
        String recTemp;
        try {
            this.inputStream = new BufferedInputStream(socket.getInputStream());
            this.outputStream = socket.getOutputStream();
        } catch (IOException e){
            e.printStackTrace();
        }

        while(socket!=null){
            synchronized(inputStream) {
                try {
                    byte[] buff = new byte[100];
                    int bytes;
                    //读取湿度数据
                    outputStream.write("a".getBytes());
                    bytes = inputStream.read(buff);
                    recText = new String(buff, "ISO-8859-1");
                    recText = recText.substring(0, bytes);
                    Bundle bundle = new Bundle();
                    bundle.putString("humid", recText);
                    Message message = Message.obtain();
                    message.what = MESSAGE_READ;
                    message.setData(bundle);
                    handler.sendMessage(message);

                    outputStream.flush();
                    Thread.sleep(100);

                    byte[] buff_temp = new byte[100];
                    outputStream.write("b".getBytes());
                    bytes = inputStream.read(buff_temp);
                    recTemp = new String(buff_temp, "ISO-8859-1");
                    recTemp = recTemp.substring(0,bytes);
                    Bundle bundle_temp = new Bundle();
                    Message message_temp = Message.obtain();
                    bundle_temp.putString("temp", recTemp);
                    message_temp.what = READ_TEMP;
                    message_temp.setData(bundle_temp);
                    handler.sendMessage(message_temp);

                    Thread.sleep(100);//显示数据太快看着不舒服，单片机发送数据也跟不上
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        makeToast("蓝牙已丢失");
    }
    }

    // 客户端与服务器建立连接成功后，用ConnectedThread收取湿度数据
//    private class ConnectedThread1 extends Thread{
//        private BluetoothSocket socket;
//        private OutputStream outputStream;
//        private BufferedInputStream inputStream;
//
//        ConnectedThread1(BluetoothSocket soc){
//            this.socket = soc;
//            this.outputStream = null;
//            this.inputStream = null;
//        }
//        @Override
//        public void run(){
//            String recText;
//            try {
//                this.inputStream = new BufferedInputStream(socket.getInputStream());
//                this.outputStream = socket.getOutputStream();
//            } catch (IOException e){
//                e.printStackTrace();
//            }
//
//            while(socket!=null){
//                synchronized(inputStream) {
//                    try {
//                        byte[] buff = new byte[100];
//                        int bytes;
//                        //读取湿度数据
//                        outputStream.write("b".getBytes());
//                        bytes = inputStream.read(buff);
//                        recText = new String(buff, "ISO-8859-1");
//                        recText = recText.substring(0, bytes);
//                        Bundle bundle = new Bundle();
//                        bundle.putString("temp", recText);
//                        Message message = Message.obtain();
//                        message.what = READ_TEMP;
//                        message.setData(bundle);
//                        handler.sendMessage(message);
//                        Thread.sleep(100);//显示数据太快看着不舒服，单片机发送数据也跟不上
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (InterruptedException ie) {
//                        ie.printStackTrace();
//                    }
//                }
//            }
//            makeToast("蓝牙已丢失");
//        }
//    }
}