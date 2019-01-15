package com.frohenk.amazfit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import me.dozen.dpreference.DPreference;

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
                if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC)
                    continue;

                if (device.getBluetoothClass().toString().contains("1f00") || device.getName().toLowerCase().contains("amazfit") || device.getName().toLowerCase().contains("bip") || device.getName().toLowerCase().contains("watch")) {
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
                                BluetoothGattCharacteristic charNotification = null;
                                BluetoothGattCharacteristic charCallback = null;

                                for (BluetoothGattService serv : gatt.getServices()) {
                                    if (serv.getCharacteristic(UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb")) != null) {
                                        charNotification = serv.getCharacteristic(UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb"));
                                    }
                                    if (serv.getCharacteristic(UUID.fromString("00000010-0000-3512-2118-0009af100700")) != null) {
                                        charCallback = serv.getCharacteristic(UUID.fromString("00000010-0000-3512-2118-0009af100700"));
                                    }
                                }
                                if (charNotification == null)
                                    return;
                                if (charCallback == null)
                                    return;
                                if (charCallback.getDescriptors().isEmpty())
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
