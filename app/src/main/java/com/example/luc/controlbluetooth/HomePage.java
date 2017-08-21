package com.example.luc.controlbluetooth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import static android.widget.AdapterView.*;

public class HomePage extends AppCompatActivity {

    private Spinner on_or_off;
    private ArrayAdapter<String> adapter = null;
    private static final String [] Switch ={"On","Off"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        on_or_off = (Spinner)findViewById(R.id.spinner);

        //设置下拉列表风格
        adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,Switch);
        //将适配器添加到spinner中去
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        on_or_off.setAdapter(adapter);
        on_or_off.setVisibility(View.VISIBLE);//设置默认显示
        on_or_off.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

    }

}
