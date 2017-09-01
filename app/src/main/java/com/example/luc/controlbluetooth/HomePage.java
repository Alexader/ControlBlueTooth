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
    Handler handler;

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
                if (btSocket == null) {
                    makeToast("还未连接到蓝牙或者连接蓝牙失败");
                }
                ConnectedThread connectedThread = new ConnectedThread(btSocket);
                connectedThread.start();
            }
        });

        //向单片机发送数据
        ensure_temp = (Button) findViewById(R.id.button1);
        ensure_temp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String send_data = setTemp.getText().toString();
                try {
                    output = btSocket.getOutputStream();
                    input = btSocket.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("IOError", "cant't connect to outputstream");
                }
                //使用write()方法需要处理异常
                try {
                    output.write(send_data.getBytes());
                    byte[] buff = new byte[1024];
                    int bytes = 0;
                    bytes = input.read(buff);
                    String rec = new String(buff, "ISO-8859-1");
                    rec = rec.substring(0, bytes);
                    if (bytes != 0) {
                        makeToast("收到数据" + rec);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ: {
                        String result = msg.getData().getString("recv");
                        String data = result.split("\\r\\n")[0];
                        String temperature = data.split("\n")[1];
                        String humidity = data.split("\n")[0];
                        Log.e("----data：----- :", ">>>" + data);
                        showTmep.setText(temperature);
                        showHumid.setText(humidity);
                    }
                }
            }
        };
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

    // 客户端与服务器建立连接成功后，用ConnectedThread收发数据
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream input = null;
            OutputStream output = null;

            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.inputStream = input;
            this.outputStream = output;
        }

        public void run() {
            StringBuilder recvText = new StringBuilder();
            byte[] buff = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buff);
                    String str = new String(buff, "ISO-8859-1");
                    str = str.substring(0, bytes);

                    // 收到数据，单片机发送上来的数据以"#"结束，这样手机知道一条数据发送结束
                    Log.d("read", str);
                    if (!str.endsWith("#")) {
                        recvText.append(str);
                        continue;
                    }
                    recvText.append(str.substring(0, str.length() - 1)); // 去除'#'

                    Bundle bundle = new Bundle();
                    Message message = Message.obtain();

                    bundle.putString("recv", recvText.toString());
                    message.what = MESSAGE_READ;
                    message.setData(bundle);
                    handler.sendMessage(message);
                    recvText.replace(0, recvText.length(), "");//清空回收数据中的数据
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                catch (Exception e){
                    Log.d("info", "运行线程");
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}