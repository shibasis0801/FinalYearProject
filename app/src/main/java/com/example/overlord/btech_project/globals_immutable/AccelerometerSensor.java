package com.example.overlord.btech_project.globals_immutable;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by overlord on 15/4/18.
 */

public class AccelerometerSensor {
    private SensorManager sensorManager;
    private Sensor sensor;

    public AccelerometerSensor(Service service) {

        sensorManager = (SensorManager) service.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void registerListener(Consumer<float []> sensorValuesConsumer) {
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                sensorValuesConsumer.accept(sensorEvent.values);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                Log.i("Accuracy Changed, Code", Integer.toString(i));

            }
        },
        sensor,
        SensorManager.SENSOR_DELAY_NORMAL);
    }
}
