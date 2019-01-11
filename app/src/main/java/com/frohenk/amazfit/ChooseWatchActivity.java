package com.frohenk.amazfit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import me.dozen.dpreference.DPreference;

import static com.frohenk.amazfit.ConnectionService.convertFromInteger;

public class ChooseWatchActivity extends AppCompatActivity {

    private TextView nothingFoundText;
    private TextView refreshButton;
    private ListView watchesList;
    private BluetoothAdapter bluetoothAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_watch);
        nothingFoundText = findViewById(R.id.nothingFoundTextView);
        refreshButton = findViewById(R.id.refreshButton);
        watchesList = findViewById(R.id.watchesList);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        connections = new ArrayList<>();
        devices = new ArrayList<>();
        alreadyExplored = new HashSet<>();
        refresh();
    }

    Set<String> alreadyExplored;
    ArrayList<BluetoothDevice> devices;
    ArrayList<BluetoothGatt> connections;

    private void disconnectAll() {
        for (BluetoothGatt bluetoothGatt : connections) {
            bluetoothGatt.close();
        }
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        connections.clear();
    }

    private void refreshList() {
        ArrayList<String> rows = new ArrayList<>();
        for (BluetoothDevice device : devices)
            rows.add(device.getName() + " | " + device.getAddress());
        ArrayAdapter<String> simpleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                rows);
        watchesList.setAdapter(simpleAdapter);
        watchesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DPreference preferences = new DPreference(ChooseWatchActivity.this, getString(R.string.preference_file_key));
                preferences.setPrefString(getString(R.string.preferences_watch_address), devices.get(position).getAddress());
                disconnectAll();
                Intent intent = new Intent(ChooseWatchActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        nothingFoundText.setVisibility(devices.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void refresh() {
        //disconnectAll();
        //devices.clear();
        refreshList();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices)
            try {
                if (device.getBluetoothClass().toString().contains("1f00")) {
                    if (alreadyExplored.contains(device.getAddress()))
                        continue;
                    alreadyExplored.add(device.getAddress());

                    device.connectGatt(this, true, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            if (newState == BluetoothAdapter.STATE_CONNECTED) {
                                gatt.discoverServices();
                            }
                        }

                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                BluetoothGattService service1 = gatt.getService(convertFromInteger(0x1811));
                                if (service1 == null)
                                    return;
                                if (service1.getCharacteristic(UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb")) == null)
                                    return;

                                BluetoothGattService service2 = gatt.getService(UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb"));
                                if (service2 == null)
                                    return;
                                BluetoothGattCharacteristic characteristic = service2.getCharacteristic(UUID.fromString("00000010-0000-3512-2118-0009af100700"));
                                if (characteristic == null)
                                    return;
                                if (characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) == null)
                                    return;
                                devices.add(gatt.getDevice());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshList();
                                    }
                                });
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("kek1", "some error", e);
            }
    }
}
