package com.example.luc.controlbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static android.widget.AdapterView.*;

public class HomePage extends AppCompatActivity {

    ArrayAdapter<String> adapter = null;
    ArrayAdapter<String> listAdapter;
    private static final String[] Switch = {"On", "Off"};//管理风扇的开关


    Spinner on_or_off;//选择风扇开关的下拉列表
    ListView listView;//显示以配对的蓝牙
    TextView info;//当没有配对蓝牙是显示的提示
    TextView showTmep;
    TextView showHumid;
    EditText setTemp;
    EditText setHumid;

    Button openBluetooth;
    Button ensure_temp;//发送温度阙值
    Button ensure_humid;//发送湿度阙值
    Button show_value;

    BluetoothAdapter bluetoothAdapter;
    public static BluetoothSocket btSocket;
    ArrayList<String> deviceArray;

    OutputStream output;

    Timer timer = new Timer(true);
    Timer timer1 = new Timer(true);

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
                try {
                    if(btSocket==null)
                        makeToast("还未连接蓝牙");
                    else {
                        output = btSocket.getOutputStream();
                        output.write("o".getBytes());//选择第一个下拉列表则向单片机发送open风扇额信号
                        makeToast("发送成功");
                    }
                } catch (IOException io){
                    io.printStackTrace();
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
        listAdapter = new ArrayAdapter<>(HomePage.this,
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
            makeToast("设备不支持蓝牙");
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
                /*和单片机的通信协议是发送“a“表示想要获取温度数据
                发送“b”表示想要获取湿度数据
                 */
                TimerTask tempTask = new TimerTask() {
                    @Override
                    public void run() {
                        UpdateText tempText = new UpdateText(btSocket, showTmep, "a");
                        tempText.execute();
                    }
                };
                TimerTask humidTask = new TimerTask() {
                    @Override
                    public void run() {
                        UpdateText updateText = new UpdateText(btSocket, showHumid, "b");
                        updateText.execute();
                    }
                };
                /*获取数据的速度是每一秒一次，两次获取数据叉开500ms，
                而且向单片机问询数据的时间间隔不宜太短，单片机处理能力有限
                 */
                timer.schedule(humidTask, 1000, 1000);
                timer1.schedule(tempTask, 1500, 1000);
            }
        });


        //向单片机发送温度数据
        ensure_temp = (Button) findViewById(R.id.button1);
        ensure_temp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer send_data = Integer.parseInt(setTemp.getText().toString());
                try {
                    OutputStream output = btSocket.getOutputStream();
                    output.write("t".getBytes());
                    output.write(send_data.byteValue());
                    makeToast("发送成功");
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
                Integer send_data = Integer.parseInt(setTemp.getText().toString());
                try {
                    if(btSocket==null) {
                        makeToast("未连接蓝牙");
                    } else {
                        OutputStream output = btSocket.getOutputStream();
                        output.write("h".getBytes());
                        output.write(send_data.byteValue());
                        makeToast("发送成功");
                    }

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

//温度和湿度数据的获取以及显示需要异步执行
    private class UpdateText extends AsyncTask<Void, String, Void> {
        private BluetoothSocket socket;
        private OutputStream outputStream;
        private BufferedInputStream inputStream;
        private TextView tx;
        private String option;

        private UpdateText(BluetoothSocket soc, TextView textView, String opt) {
            socket = soc;
            tx = textView;
            option = opt;
            try {
                inputStream = new BufferedInputStream(socket.getInputStream());
                outputStream = socket.getOutputStream();
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            String recText;
            try {
                byte[] buff = new byte[100];//手动获取缓冲区
                int bytes;
                //读取湿度或者温度数据
                outputStream.write(option.getBytes());
                bytes = inputStream.read(buff);
                //从单片机获取的字节数据，需要指定译码格式
                recText = new String(buff, "ISO-8859-1");
                recText = recText.substring(0, bytes);
                publishProgress(recText);//将获取的数据移交到onProgressUpdate()方法处理，因为需要更新UI
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... results) {
            super.onProgressUpdate(results);
            tx.setText(results[0]);
        }

        @Override
        protected void onCancelled() {
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
    }
}