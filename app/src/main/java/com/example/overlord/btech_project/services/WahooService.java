package com.example.overlord.btech_project.services;

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


public class WahooService extends HeartBeatService {

    private Singleton global = Singleton.getInstance();

    private HeartBeatSource source;
    private AccelerometerSensor accelerometerSensor;
    private AccelerometerBuffer accelerometerBuffer;
    private MovingWindowBuffer<Fused> windowBuffer = new MovingWindowBuffer<>(60);


    public String getTimeLabel(Heartrate.Data data) {
        return data.getTime().format("dd_MM_yyyy_HH_mm_ss");
    }

    public void storeHeartBeat(String time, ArrayList<Fused> heartBeats) {
        global.heartRef
                .child(time)
                .setValue(heartBeats)
                .addOnFailureListener(exception ->
                    Log.e("Fbase HeartRate Err", exception.getMessage())
                );
    }

    public void addBeat(Heartrate.Data data) {
        Log.i("New Data", data.toString());

        int second = TimeUtils.getSecondsFromTimestamp(data.getTimeMs());

        float []values = accelerometerBuffer.getAverage(second);
        accelerometerBuffer.remove(second);

        Fused dataPoint = new Fused(getHeartBeat(data), values);

        windowBuffer.add(dataPoint)
                    .onBufferFull(array ->
                            storeHeartBeat(getTimeLabel(data), array)
                    );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        accelerometerBuffer = new AccelerometerBuffer();

        accelerometerSensor = new AccelerometerSensor(this);
        accelerometerSensor.registerListener(
                sensorEvent ->
                    accelerometerBuffer.add(
                            //From NanoSeconds to MilliSeconds
                            TimeUtils.getSecondsFromTimestamp(sensorEvent.timestamp / (1000 * 1000)),
                            sensorEvent.values
                    ));

        source = getSource(this);
        source.setOnNewHeartBeatListener(this::addBeat);
    }
}
