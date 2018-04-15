package com.example.overlord.btech_project.services;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.example.overlord.btech_project.base.HeartBeatService;
import com.example.overlord.btech_project.globals_immutable.AccelerometerSensor;
import com.example.overlord.btech_project.globals_immutable.TimeUtils;
import com.example.overlord.btech_project.globals_mutable.Singleton;
import com.example.overlord.btech_project.model.AccelerometerBuffer;
import com.example.overlord.btech_project.model.Fused;
import com.example.overlord.btech_project.model.MovingWindowBuffer;
import com.wahoofitness.connector.capabilities.Heartrate;

import java.util.ArrayList;
import java.util.Calendar;


public class WahooService extends HeartBeatService {

    private Singleton global = Singleton.getInstance();

    private HeartBeatSource source;
    private AccelerometerSensor accelerometerSensor;
    private AccelerometerBuffer accelerometerBuffer;
    private MovingWindowBuffer<Fused> windowBuffer;


    public String getTimeLabel(Heartrate.Data data) {
        return data.getTime().format("dd_MM_yyyy_HH_mm_ss");
    }

    public <T> void storeHeartBeat(String time, ArrayList<T> heartBeats) {
        Log.i("StoreHeartBeatAt", time);
        global.heartRef
                .child(time)
                .setValue(heartBeats)
                .addOnFailureListener(exception ->
                    Log.e("Fbase HeartRate Err", exception.getMessage())
                );
    }

    public void addBeat(Heartrate.Data data) {

        int second = TimeUtils.getSecondsFromTimestamp(data.getTimeMs());

        float []values = accelerometerBuffer.getAverage(second);
        accelerometerBuffer.remove(second);

        Fused dataPoint = new Fused(getHeartBeat(data), values);
        Log.i("DataPoint", dataPoint.toString());

        windowBuffer.add(dataPoint)
                .setOnBufferFullListener(array ->
                            storeHeartBeat(getTimeLabel(data), array)
                );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.enable();

        windowBuffer = new MovingWindowBuffer<>(60);
        accelerometerBuffer = new AccelerometerBuffer();

        accelerometerSensor = new AccelerometerSensor(this);
        accelerometerSensor.registerListener(
                sensorValues -> {

                    long timestamp = TimeUtils.getTimeStamp();
                    int second = TimeUtils.getSecondsFromTimestamp(timestamp);

                    accelerometerBuffer.add(
                            second,
                            sensorValues
                    );
                }
        );


        source = getSource(this);
        source.setOnNewHeartBeatListener(this::addBeat);
    }
}
