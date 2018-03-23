package com.example.overlord.btech_project;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.wahoofitness.common.datatypes.Rate;
import com.wahoofitness.common.datatypes.TimeInstant;
import com.wahoofitness.common.datatypes.TimePeriod;
import com.wahoofitness.connector.HardwareConnector;
import com.wahoofitness.connector.HardwareConnectorEnums;
import com.wahoofitness.connector.HardwareConnectorTypes;
import com.wahoofitness.connector.capabilities.Capability;
import com.wahoofitness.connector.capabilities.Heartrate;
import com.wahoofitness.connector.conn.connections.SensorConnection;
import com.wahoofitness.connector.conn.connections.params.ConnectionParams;
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener;

import java.util.Deque;


public class WahooService extends Service {
    private HeartBeatSource source;

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

    FirebaseDatabase realtime_database = FirebaseDatabase.getInstance();
    DatabaseReference root = realtime_database.getReference();
    DatabaseReference heartRef = root.child("heartBeat");

    public void storeHeartBeat(String time, Deque<Double> HeartBeats) {
        heartRef.child(time)
                .setValue(HeartBeats)
                .addOnCompleteListener(task -> {
                    if ( ! task.isSuccessful()) {
                        String message = "UNKNOWN";
                        if (task.getException() != null)
                            message = task.getException().getMessage();

                        Log.e("Fbase HeartRate Err", message);
                    }
                });
    }

    public String getTimeLabel(Heartrate.Data data) {
        return data.getTime().format("dd_MM_yyyy_HH_mm");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        source = new HeartBeatSource(this);
        Log.i("SERVICE", "STARTED");

        source.onNewHeartBeat(data -> {
            Log.d("Heart Rate Incoming", data.toString());

            /**
             * Implementing moving window algorithm
             * 60 seconds with 30 second overlap
             * Event driven loop for activity to fetch
             */

            Deque<Heartrate>

            storeHeartBeat(time, data);
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        source.connector.shutdown();
    }

    public WahooService() {}

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
