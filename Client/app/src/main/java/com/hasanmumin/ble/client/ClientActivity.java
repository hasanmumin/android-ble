package com.hasanmumin.ble.client;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;

import com.hasanmumin.ble.client.log.Slf4jHelper;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class ClientActivity extends AppCompatActivity implements BleManagerCallbacks {

    private static final Logger LOGGER = Slf4jHelper.getLogger(ClientActivity.class.getName());
    private static Timer timerConnectionCheck;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Handler mHandlerLog = new Handler(Looper.getMainLooper());
    private TextView mClientLog;
    private NestedScrollView mClientScrollView;
    private BluetoothDevice mServerDevice;
    private ClientManager mClientManager;
    private boolean mBluetoothRegistered = false;
    private boolean mScanning = false;
    private final ClientScanCallback scanCallback = new ClientScanCallback() {

        @Override
        void onScanComplete(BluetoothDevice bluetoothDevice) {
            stopScan();
            if (mServerDevice == null) {
                mServerDevice = bluetoothDevice;
                connect();
            }
        }

        @Override
        void onScanFailed(String message, int errorCode) {
            log(message, errorCode);
            stopScan();
        }
    };

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        // Bluetooth has been turned off;
                        enableBluetooth();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        // Bluetooth has been on
                        startScan();
                        break;
                }
            }
        }
    };

    public static boolean enableBluetooth() {
        return BluetoothAdapter.getDefaultAdapter().enable();
    }

    public static boolean disableBluetooth() {
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            return BluetoothAdapter.getDefaultAdapter().disable();
        }
        return true;
    }

    public void cancelTimerConnectionCheck() {
        try {
            if (timerConnectionCheck != null) {
                timerConnectionCheck.cancel();
                timerConnectionCheck.purge();
                timerConnectionCheck = null;
            }
        } catch (Exception e) {
            LOGGER.error("ERROR", e);
        }
    }

    private void registerReceiverBluetooth() {
        try {
            registerReceiver(mBluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            mBluetoothRegistered = true;
        } catch (Exception e) {
            LOGGER.error("registerReceiverBluetooth()", e);
        }
    }

    private void unRegisterReceiverBluetooth() {
        mBluetoothRegistered = false;
        try {
            unregisterReceiver(mBluetoothReceiver);
        } catch (Exception e) {
            LOGGER.error("unRegisterReceiverBluetooth()", e);
        }
    }

    public void startTimerConnectionCheck() {
        cancelTimerConnectionCheck();
        timerConnectionCheck = new Timer();
        timerConnectionCheck.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(() -> {
                    if (mClientManager.isConnected() && mClientManager.isReady()) {
                        mClientManager.send("alive");
                    }

                });
            }
        }, 1000 * 10, 1000 * 30);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        mClientLog = findViewById(R.id.client_log);
        mClientScrollView = findViewById(R.id.scroll_view);
        ClientApplication.setActivity(this);
        log("onCreate(), Waiting 1 minute for search server");

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        }, 1);
    }

    private void connect() {
        mClientManager = new ClientManager(this);
        mClientManager.setGattCallbacks(this);

        mClientManager.connect(mServerDevice)
                .retry(3, 100)
                .useAutoConnect(false)
                .enqueue();
    }

    public boolean isConnected() {
        return mClientManager != null && mClientManager.isConnected();
    }

    private boolean needBluetoothEnable() {
        if (!mBluetoothRegistered) {
            registerReceiverBluetooth();
        }
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            return !enableBluetooth();
        }
        return true;
    }

    public void startScan() {
        if (mScanning || isConnected() || !needBluetoothEnable()) {
            return;
        }
        log("startScan()");
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(500)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setUseHardwareBatchingIfSupported(false)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(ClientManager.DEVICE_UUID_SERVICE)).build());
        mScanning = true;
        scanner.startScan(filters, settings, scanCallback);
        mHandler.postDelayed(this::stopScan, 1000 * 60);
    }

    public void stopScan() {
        if (mScanning) {
            log("stopScan()");
            mScanning = false;
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(scanCallback);
        }
    }

    public void log(String message, Object... arguments) {
        String log = String.format(message, arguments);
        LOGGER.info(log);
        mHandlerLog.post(() -> {
            mClientLog.append(log + "\n");
            mClientScrollView.post(() -> mClientScrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
        log("onDeviceConnecting() %s", device.getAddress());
    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        log("onDeviceConnected() %s", device.getAddress());
    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
        log("onDeviceDisconnecting() %s", device.getAddress());
    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
        log("onDeviceDisconnected() %s", device.getAddress());
    }

    @Override
    public void onLinkLossOccurred(@NonNull BluetoothDevice device) {
        log("onLinkLossOccurred() %s", device.getAddress());
    }

    @Override
    public void onServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {
        log("onServicesDiscovered() %s", device.getAddress());
    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {
        log("onDeviceReady() %s", device.getAddress());
        startTimerConnectionCheck();
    }

    @Override
    public void onBondingRequired(@NonNull BluetoothDevice device) {
        log("onBondingRequired() %s", device.getAddress());
    }

    @Override
    public void onBonded(@NonNull BluetoothDevice device) {
        log("onBonded() %s", device.getAddress());
    }

    @Override
    public void onBondingFailed(@NonNull BluetoothDevice device) {
        log("onBondingFailed() %s", device.getAddress());
    }

    @Override
    public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {
        log("onError() %s Message %s errorCode %s", device.getAddress(), message, errorCode);
    }

    @Override
    public void onDeviceNotSupported(@NonNull BluetoothDevice device) {
        log("onDeviceNotSupported() %s", device.getAddress());
    }
}
