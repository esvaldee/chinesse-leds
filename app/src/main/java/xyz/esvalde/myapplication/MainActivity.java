package xyz.esvalde.myapplication;

import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.*;
import xyz.esvalde.myapplication.adapter.BleDeviceAdapter;
import xyz.esvalde.myapplication.adapter.fragment.ControllerFragment;

import java.util.*;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1001;
    private static final int REQUEST_ENABLE_BT = 1002;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    private BleDeviceAdapter deviceAdapter;

    private Button btnScan;
    private TextView tvStatus;
    private RecyclerView rvDevices;
    private FrameLayout layoutController;

    private final List<BluetoothDevice> deviceList = new ArrayList<>();
    private volatile boolean isScanning = false;

    public static final UUID SERVICE_UUID       = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_WRITE_UUID    = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");
    public static final UUID SERVICE_UUID_ALT   = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_WRITE_UUID_ALT= UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic writeCharacteristic;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (!deviceList.contains(device)) {
                deviceList.add(device);
                runOnUiThread(() -> deviceAdapter.notifyDataSetChanged());
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(() -> tvStatus.setText("Połączono! Odkrywam usługi..."));
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                writeCharacteristic = null;
                runOnUiThread(() -> {
                    tvStatus.setText("Rozłączono");
                    layoutController.setVisibility(View.GONE);
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) return;

            BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service != null) {
                writeCharacteristic = service.getCharacteristic(CHAR_WRITE_UUID);
            }
            if (writeCharacteristic == null) {
                service = gatt.getService(SERVICE_UUID_ALT);
                if (service != null) {
                    writeCharacteristic = service.getCharacteristic(CHAR_WRITE_UUID_ALT);
                }
            }
            if (writeCharacteristic == null) {
                outer:
                for (BluetoothGattService s : gatt.getServices()) {
                    for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                        int props = c.getProperties();
                        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                                (props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                            writeCharacteristic = c;
                            break outer;
                        }
                    }
                }
            }

            final boolean found = writeCharacteristic != null;
            runOnUiThread(() -> {
                if (found) {
                    tvStatus.setText("Gotowy do sterowania!");
                    layoutController.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.layout_controller, new ControllerFragment())
                            .commitAllowingStateLoss();
                } else {
                    tvStatus.setText("Połączono, ale nie znaleziono charakterystyki LED");
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus     = findViewById(R.id.tv_status);
        btnScan      = findViewById(R.id.btn_scan);
        rvDevices    = findViewById(R.id.rv_devices);
        layoutController = findViewById(R.id.layout_controller);

        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (btManager != null) {
            bluetoothAdapter = btManager.getAdapter();
        }

        deviceAdapter = new BleDeviceAdapter(deviceList, this::connectToDevice);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(deviceAdapter);

        btnScan.setOnClickListener(v -> {
            if (isScanning) stopScan();
            else startScan();
        });

        checkPermissionsAndBluetooth();
    }

    private void checkPermissionsAndBluetooth() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_SCAN);
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQUEST_PERMISSIONS);
            return;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }
    }

    private void startScan() {
        if (bluetoothAdapter == null) return;
        deviceList.clear();
        deviceAdapter.notifyDataSetChanged();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) return;

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bleScanner.startScan(null, settings, scanCallback);
            isScanning = true;
            btnScan.setText("Zatrzymaj skan");
            tvStatus.setText("Skanowanie BLE...");
            new Handler(Looper.getMainLooper()).postDelayed(this::stopScan, 15000);
        }
    }

    private void stopScan() {
        if (!isScanning || bleScanner == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bleScanner.stopScan(scanCallback);
            isScanning = false;
            btnScan.setText("Skanuj BLE");
            tvStatus.setText("Znaleziono: " + deviceList.size() + " urządzeń");
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        stopScan();
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                bluetoothGatt.close();
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            tvStatus.setText("Łączę z " + (device.getName() != null ? device.getName() : device.getAddress()) + "...");
            bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        }
    }

    public void sendColor(int r, int g, int b) {
        byte[] cmd = new byte[]{ 0x7e, 0x00, 0x05, 0x03, (byte) r, (byte) g, (byte) b, 0x00, (byte) 0xef };
        writeCommand(cmd);
    }

    public void sendBrightness(int brightness) {
        byte level = (byte) Math.round(brightness * 255.0 / 100);
        byte[] cmd = new byte[]{ 0x7e, 0x00, 0x01, level, 0x00, 0x00, 0x00, 0x00, (byte) 0xef };
        writeCommand(cmd);
    }

    public void sendEffect(int effectCode) {
        byte[] cmd = new byte[]{ 0x7e, 0x00, 0x03, (byte) effectCode, 0x03, 0x00, 0x00, 0x00, (byte) 0xef };
        writeCommand(cmd);
    }

    public void sendPower(boolean on) {
        byte[] cmd = new byte[]{ 0x7e, 0x00, 0x04, on ? (byte)0x01 : 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xef };
        writeCommand(cmd);
    }

    private void writeCommand(byte[] data) {
        if (bluetoothGatt == null || writeCharacteristic == null) {
            Toast.makeText(this, "Nie połączono z paskiem LED", Toast.LENGTH_SHORT).show();
            return;
        }
        writeCharacteristic.setValue(data);
        writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                bluetoothGatt.close();
            }
        }
    }
}