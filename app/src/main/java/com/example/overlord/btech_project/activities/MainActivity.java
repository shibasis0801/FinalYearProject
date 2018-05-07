package com.example.overlord.btech_project.activities;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.BoringLayout;
import android.widget.TextView;

import com.example.overlord.btech_project.R;
import com.example.overlord.btech_project.services.WahooService;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.text_view);
        textView.setText("Hello Brother");


    }

    private ServiceConnection serviceConnection;
    private WahooService wahooService;
    private Boolean isBound = false;

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, WahooService.class);
        serviceConnection = getServiceConnection();

        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
    }

    protected ServiceConnection getServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                WahooService.LocalBinder binder = (WahooService.LocalBinder) service;
                wahooService = binder.getService();
                isBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }
        };
    }

}
