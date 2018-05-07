package com.example.overlord.btech_project.base;

import android.app.Service;
import android.content.Intent;
import android.hardware.SensorEventListener;
import android.os.IBinder;
import android.util.Log;

import com.example.overlord.btech_project.globals_immutable.Consumer;
import com.wahoofitness.connector.HardwareConnector;
import com.wahoofitness.connector.HardwareConnectorEnums;
import com.wahoofitness.connector.HardwareConnectorTypes;
import com.wahoofitness.connector.capabilities.Accelerometer;
import com.wahoofitness.connector.capabilities.Capability;
import com.wahoofitness.connector.capabilities.Heartrate;
import com.wahoofitness.connector.conn.connections.SensorConnection;
import com.wahoofitness.connector.conn.connections.params.ConnectionParams;
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener;


public abstract class HeartBeatService extends Service {

    protected HeartBeatSource source;

    public Double getHeartBeat(Heartrate.Data data) {
        return data.getHeartrate().asEventsPerMinute();
    }

    protected class HeartBeatSource {

        //BlackBox for less dev time.
        protected Heartrate heartrate;
        protected HardwareConnector connector;

        protected SensorConnection connection;

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

        public void setOnNewHeartBeatListener(Consumer<Heartrate.Data> onNewHeartBeatConsumer) {
            connector.startDiscovery(
                    //Above Function
                    getDiscoveryListener(
                            onNewHeartBeatConsumer
                    )
            );
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("SERVICE", "STARTED");
    }

    //Singleton
    public HeartBeatSource getSource(Service callingService) {
        if (source == null)
            source = new HeartBeatSource(callingService);

        return source;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        source.connector.shutdown();
    }

    public HeartBeatService() {}

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
