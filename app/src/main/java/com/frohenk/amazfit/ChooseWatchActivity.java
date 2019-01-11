package com.frohenk.amazfit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

        refresh();
    }

    private void refresh() {
        final ArrayList<BluetoothDevice> devices = new ArrayList<>();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        ArrayList<String> rows = new ArrayList<>();
        for (BluetoothDevice device : bondedDevices)
            if (device.getName().contains("Amazfit Bip")) {
                devices.add(device);
                rows.add(device.getName() + " | " + device.getAddress());
            }
        ArrayAdapter<String> simpleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                rows);
        watchesList.setAdapter(simpleAdapter);
        watchesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DPreference preferences = new DPreference(ChooseWatchActivity.this, getString(R.string.preference_file_key));

                preferences.setPrefString(getString(R.string.preferences_watch_address), devices.get(position).getAddress());
                Intent intent = new Intent(ChooseWatchActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        nothingFoundText.setVisibility(devices.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
