package com.hasanmumin.ble.server;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.hasanmumin.ble.server.log.Slf4jHelper;

import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ServerActivity extends AppCompatActivity {

    public final static UUID DEVICE_UUID_SERVICE = UUID.fromString("00001523-1212-efde-1523-785feabcd123");
    public final static UUID DEVICE_UUID_CHAR_WRITE = UUID.fromString("00001524-1212-efde-1523-785feabcd123");
    public final static UUID DEVICE_UUID_CHAR_NOTIFY = UUID.fromString("00001525-1212-efde-1523-785feabcd123");
    public final static UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Logger LOGGER = Slf4jHelper.getLogger(this.getClass().getName());
    private TextView mServerLog;
    private TextView serverMessage;
    private Handler mHandlerLog = new Handler(Looper.getMainLooper());
    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            log("Peripheral advertising started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            log("Peripheral advertising failed: " + errorCode);
        }
    };
    private Handler mHandlerHelper = new Handler();
    private BluetoothGattServer mGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothDevice bluetoothClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        mServerLog = findViewById(R.id.server_log);
        serverMessage = findViewById(R.id.server_message);
        findViewById(R.id.start_server).setOnClickListener(view -> startServer());
        findViewById(R.id.send_server_message).setOnClickListener(view -> sendMessage());
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        }, 1);
    }

    public void sendMessage() {
        if (bluetoothClient != null) {
            String message = serverMessage.getEditableText().toString();
            log("Sending message to client is %s", message);
            BluetoothGattService service = mGattServer.getService(DEVICE_UUID_SERVICE);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(DEVICE_UUID_CHAR_NOTIFY);
            characteristic.setValue(message.getBytes(StandardCharsets.UTF_8));
            mGattServer.notifyCharacteristicChanged(bluetoothClient, characteristic, false);
        }
    }

    private void log(String message, Object... arguments) {
        String log = String.format(message, arguments);
        LOGGER.info(log);
        mHandlerLog.post(() -> mServerLog.append(log + "\n"));
    }

    public void startServer() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        GattServerCallback gattServerCallback = new GattServerCallback();
        mGattServer = bluetoothManager.openGattServer(this, gattServerCallback);
        setupServer();
        startAdvertising();
    }

    private void setupServer() {
        BluetoothGattService service = new BluetoothGattService(DEVICE_UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        int properties = BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY;
        int permissions = BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ;

        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(DEVICE_UUID_CHAR_WRITE,
                properties,
                permissions);

        BluetoothGattCharacteristic characteristicNotify = new BluetoothGattCharacteristic(DEVICE_UUID_CHAR_NOTIFY,
                properties,
                permissions);
        BluetoothGattDescriptor descriptorNotify = new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID,
                permissions);

        descriptorNotify.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        characteristicNotify.addDescriptor(descriptorNotify);

        service.addCharacteristic(characteristicWrite);
        service.addCharacteristic(characteristicNotify);
        mGattServer.addService(service);
    }

    private void startAdvertising() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();
        ParcelUuid parcelUuid = new ParcelUuid(DEVICE_UUID_SERVICE);
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(parcelUuid)
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private final class GattServerCallback extends BluetoothGattServerCallback {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                log("Device is disconnected. %s", device.toString());
                bluetoothClient = null;
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                log("Device is connected. %s", device.toString());
                bluetoothClient = device;
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            log("onCharacteristicReadRequest() %s ", device.getAddress());
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            log("onCharacteristicWriteRequest() %s ", device.getAddress());
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            log("Message is %s", new String(value));
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            log("onNotificationSent " + status);
        }
    }
}
