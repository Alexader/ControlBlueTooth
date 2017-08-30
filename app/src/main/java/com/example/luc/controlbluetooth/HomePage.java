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


import java.io.InputStream;

import static android.widget.AdapterView.*;

public class HomePage extends AppCompatActivity {

    ArrayAdapter<String> adapter = null;
    private static final String [] Switch ={"On","Off"};

    TextView Temp;
    TextView Humid;
    Spinner on_or_off;

    Button openBluetooth;
    private BluetoothDevice device;
    private BroadcastReceiver rec;
    Button ensure_temp;
    Button ensure_humid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        on_or_off = (Spinner) findViewById(R.id.spinner);
        Temp = (TextView) findViewById(R.id.temp);
        Humid = (TextView) findViewById(R.id.humidity_show);

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

        //跳转到蓝牙设置
        openBluetooth = (Button)findViewById(R.id.openBluetooth);
        try{
        openBluetooth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, ConnectBluetooth.class);
                startActivity(intent);
            }
        });
        }catch (Exception e){
            e.printStackTrace();
        }
        ensure_temp = (Button)findViewById(R.id.button1);
        ensure_temp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String send_data = Temp.getText().toString();
                InputStream tinputStream = BluetoothSocket.getInputStream();
            }
        });

}
}

