package com.example.overlord.btech_project;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.wahoofitness.connector.HardwareConnector;
import com.wahoofitness.connector.HardwareConnectorEnums;
import com.wahoofitness.connector.HardwareConnectorTypes;
import com.wahoofitness.connector.capabilities.Capability;
import com.wahoofitness.connector.capabilities.Heartrate;
import com.wahoofitness.connector.conn.connections.SensorConnection;
import com.wahoofitness.connector.conn.connections.params.ConnectionParams;
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class AbstractService extends Service implements SensorEventListener {

    private FirebaseDatabase realtime_database;
    private DatabaseReference root;
    private DatabaseReference heartRef;

    private SensorManager sensorManager;
    private Sensor sensor;

    private int count = 0;
    private boolean sixty_reached = false;

    private HeartBeatSource source;

    public class Fused {
        public Double heartBeat;
        public float x;
        public float y;
        public float z;

        public Fused() {}

        public Fused(Double heartBeat, float values[]) {
            this.heartBeat = heartBeat;
            x = values[0];
            y = values[1];
            z = values[2];
        }

        public Double getHeartBeat() {
            return heartBeat;
        }

        public void setHeartBeat(Double heartBeat) {
            this.heartBeat = heartBeat;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public float getZ() {
            return z;
        }

        public void setZ(float z) {
            this.z = z;
        }
    }

    private Deque<Fused> heartBeats = new ArrayDeque<>();

    private void initFirebaseAndBuffers() {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        realtime_database = FirebaseDatabase.getInstance();
        root = realtime_database.getReference();
        heartRef = root.child("heartBeat");

        accelerometerBuffer = new AccelerometerBuffer();
    }

    private class HeartBeatSource {

        //BlackBox for less dev time.
        private Heartrate heartrate;
        private HardwareConnector connector;

        private SensorConnection connection;

        //Only info logging.
        private final HardwareConnector.Listener connectorListener = new HardwareConnector.Listener() {
            @Override
            public void onHardwareConnectorStateChanged(
                    HardwareConnectorTypes.NetworkType networkType,
                    HardwareConnectorEnums.HardwareConnectorState hardwareConnectorState
            ){
                Log.i("HardwareConnectorChange", hardwareConnectorState.name() + networkType.toString());
            }
            @Override
            public void onFirmwareUpdateRequired(SensorConnection sensorConnection, String s, String s1) {
                Log.i("FirmwareUpdate", sensorConnection.toString() + s + s1);
            }
        };

        private HeartBeatSource(Service service) {
            connector = new HardwareConnector(service, connectorListener);
        }

        private SensorConnection.Listener getSensorConnection(Consumer<Heartrate.Data> onNewHeartBeat) {
            return new SensorConnection.Listener() {
                @Override
                public void onNewCapabilityDetected(SensorConnection sensorConnection, Capability.CapabilityType capabilityType) {

                    if (capabilityType == Capability.CapabilityType.Heartrate) {

                        connector.stopDiscovery();

                        heartrate = (Heartrate) sensorConnection.getCurrentCapability(Capability.CapabilityType.Heartrate);
                        heartrate.addListener(new Heartrate.Listener() {
                            @Override
                            public void onHeartrateData(Heartrate.Data data) {
                                //Actual Specific Code
                                onNewHeartBeat.accept(data);
                            }
                            @Override
                            public void onHeartrateDataReset() {
                                Log.i("HeartRateReset", "");
                            }
                        });
                    }
                }
                @Override
                public void onSensorConnectionStateChanged(SensorConnection sensorConnection, HardwareConnectorEnums.SensorConnectionState sensorConnectionState) {
                    Log.i("SensorConnChange", sensorConnectionState.toString());
                }
                @Override
                public void onSensorConnectionError(SensorConnection sensorConnection, HardwareConnectorEnums.SensorConnectionError sensorConnectionError) {
                    Log.e("SensorConnErr", sensorConnectionError.toString());
                }
            };
        }

        private DiscoveryListener getDiscoveryListener(Consumer<Heartrate.Data> onNewHeartBeat) {
            return new DiscoveryListener() {
                @Override
                public void onDeviceDiscovered(ConnectionParams connectionParams) {
                    //Above Function
                    connection = connector.requestSensorConnection(connectionParams, getSensorConnection(onNewHeartBeat));
                }

                @Override
                public void onDiscoveredDeviceLost(ConnectionParams connectionParams) {
                    Log.i("DeviceLost", connectionParams.toString());
                }

                @Override
                public void onDiscoveredDeviceRssiChanged(ConnectionParams connectionParams, int i) {
                    Log.i("DiscoveredDeviceRSSi", connectionParams.toString());
                }
            };
        }

        private void onNewHeartBeat(Consumer<Heartrate.Data> onNewHeartBeatConsumer) {
            connector.startDiscovery(
                    //Above Function
                    getDiscoveryListener(
                            onNewHeartBeatConsumer
                    )
            );
        }

    }

    public String getTimeLabel(Heartrate.Data data) {
        return data.getTime().format("dd_MM_yyyy_HH_mm_ss");
    }

    public Double getHeartBeat(Heartrate.Data data) {
        return data.getHeartrate().asEventsPerMinute();
    }


    public ArrayList<Fused> getList(Deque<Fused> heartBeats) {
        ArrayList<Fused> beats = new ArrayList<>();
        beats.addAll(heartBeats);
        return beats;
    }

    public void storeHeartBeat(String time, Deque<Fused> heartBeats) {
        heartRef.child(time)
                .setValue(getList(heartBeats))
                .addOnCompleteListener(task -> {

                    if ( ! task.isSuccessful()) {

                        String message = "UNKNOWN";

                        if (task.getException() != null)
                            message = task.getException().getMessage();

                        Log.i("Fbase HeartRate Err", message);
                    }
                });
    }

    public void addBeat(Heartrate.Data data) {

        int second = getSecondsFromTimestamp(data.getTimeMs());

        float []values = accelerometerBuffer.getAverage(second);
        accelerometerBuffer.remove(second);

        Fused dataPoint = new Fused(getHeartBeat(data), values);


        //Deque not full
        if (heartBeats.size() != 60) {
            heartBeats.addLast(dataPoint);
        }
        //Full Deque
        else {
            //Deque got full for the first time
            if ( !sixty_reached ) {
                sixty_reached = true;
                storeHeartBeat(getTimeLabel(data), heartBeats);
            }
            //Moving Window of 30 seconds
            else {
                heartBeats.removeFirst();
                heartBeats.addLast(dataPoint);
                count++;

                if (count == 30) {
                    count = 0;
                    storeHeartBeat(getTimeLabel(data), heartBeats);
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initFirebaseAndBuffers();
        Log.i("SERVICE", "STARTED");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        Fused fused = new Fused(50.3, new float[] {1, 2, 3}) ;

        heartRef.child("TESTING").setValue(fused);

        source = new HeartBeatSource(this);
        source.onNewHeartBeat(data -> {
            Log.d("Heart Rate Incoming", data.toString());
            addBeat(data);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        source.connector.shutdown();
    }

    public AbstractService() {}

    public AccelerometerBuffer accelerometerBuffer;
    public class AccelerometerBuffer {
        private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<float []>> accleroValues;

        public AccelerometerBuffer() {
            accleroValues = new ConcurrentHashMap<>();

            for (int i = 0; i < 60; i++)
                accleroValues.put(i, new ConcurrentLinkedQueue<>());
        }

        public void add(int second, float []values) {
            accleroValues.get(second).add(values);
        }

        public float[] getAverage(int second) {

            float []average = new float[3];
            ConcurrentLinkedQueue<float[]> queue = accleroValues.get(second);

            for (float[] values : queue)
                for (int i = 0; i < 3; i++)
                    average[i] += values[i];

            for (int i = 0; i < 3; i++)
                average[i] = average[i] / (float) queue.size();

            return average;
        }

        public void remove(int second) {
            accleroValues.get(second).clear();
        }
    }

    @Exclude
    public int getSecondsFromTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp / ( 1000 * 1000 ));
        return calendar.get(Calendar.SECOND);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        accelerometerBuffer.add(
                getSecondsFromTimestamp(sensorEvent.timestamp),
                sensorEvent.values
        );
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
