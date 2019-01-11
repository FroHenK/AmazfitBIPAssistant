package com.frohenk.amazfit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Set;
import java.util.UUID;

import me.dozen.dpreference.DPreference;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_CONNECTING;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTING;

public class ConnectionService extends Service {
    public static final String CHANNEL_ID = "KEKID";
    public static final int NOTIFICATION_ID = 228;
    public static final int DELAY_STEP = 300;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private boolean isOperational;
    private Handler handler;
    private DPreference preferences;
    private Notification.Builder builder;
    private NotificationManager notificationManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i("kek", "service received start command");
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Foreground service";
            String description = "Just here, so service won't die";
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setVibrationPattern(null);
            channel.enableVibration(true);
            channel.enableLights(false);
            channel.setLightColor(0);
            channel.setSound(null, null);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (!notificationManager.getNotificationChannels().isEmpty())
                notificationManager.deleteNotificationChannel(CHANNEL_ID);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = new DPreference(this, getString(R.string.preference_file_key));
        handler = new Handler();
        Log.i("kek", "service onCreate starting");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
            notification = builder
                    .setContentTitle("Disconnected")
                    .setSmallIcon(R.mipmap.cool_launcher)
                    .setContentIntent(pendingIntent)
                    .build();
        } else {
            builder = new Notification.Builder(this);
            notification = builder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                    .setContentTitle("Disconnected")
                    .setSmallIcon(R.mipmap.cool_launcher).setVibrate(null)
                    .setContentIntent(pendingIntent).setPriority(Notification.PRIORITY_MIN)
                    .build();
        }
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        ;
        startForeground(NOTIFICATION_ID, notification);
        initBluetooth();
    }

    private BluetoothDevice device;
    private int numberOfClicks = 0;

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device : bondedDevices)
            if (device.getAddress().equals(preferences.getPrefString(getString(R.string.preferences_watch_address), "")))
                this.device = device;
        if (this.device == null)
            return;
        bluetoothGatt = device.connectGatt(this, true, new BluetoothGattCallback() {

            private BluetoothGattCharacteristic characteristic;

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {


                switch (newState) {
                    case STATE_CONNECTED:
                        Log.i("kek", "device connected");
                        gatt.discoverServices();
                        isOperational = true;
                        builder.setContentTitle("Connected");
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                        break;
                    case STATE_CONNECTING:
                        Log.i("kek", "device connecting");
                        isOperational = false;
                        builder.setContentTitle("Connecting");
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                        break;
                    case STATE_DISCONNECTING:
                        isOperational = false;
                        Log.i("kek", "device disconnecting");
                        builder.setContentTitle("Disconnecting");
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                        break;
                    case STATE_DISCONNECTED:
                        Log.i("kek", "device disconnected");
                        isOperational = false;
                        builder.setContentTitle("Disconnected");
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                        bluetoothGatt.connect();
                        break;


                }

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                isOperational = true;
                BluetoothGattService service = gatt.getService(UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb"));
                characteristic = service.getCharacteristic(UUID.fromString("00000010-0000-3512-2118-0009af100700"));
                gatt.setCharacteristicNotification(characteristic, true);

                System.out.println(characteristic);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.readCharacteristic(characteristic);
                gatt.writeDescriptor(descriptor);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                Log.i("kek", "Received data: " + String.valueOf(characteristic.getValue()[0]));

                if (characteristic.getValue()[0] == 11) {

                }

                if (characteristic.getValue()[0] == 4) {
                    numberOfClicks++;
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("kek", "Number of clicks: " + numberOfClicks);
                            switch (preferences.getPrefInt(getString(R.string.multiple_click_action), R.id.action2Pause3Next)) {
                                case R.id.action2Pause3Next:
                                    if (numberOfClicks == 2)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                                    if (numberOfClicks == 3)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_NEXT);
                                    break;
                                case R.id.action2Previous3Next:
                                    if (numberOfClicks == 2)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                                    if (numberOfClicks == 3)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_NEXT);
                                    break;

                                case R.id.action2Next3Previous:
                                    if (numberOfClicks == 2)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_NEXT);
                                    if (numberOfClicks == 3)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                                    break;

                                case R.id.action2Pause3Next4Previous:
                                    if (numberOfClicks == 2)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                                    if (numberOfClicks == 3)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_NEXT);
                                    if (numberOfClicks == 4)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                                    break;

                                case R.id.action2Previous3Pause4Next:
                                    if (numberOfClicks == 2)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                                    if (numberOfClicks == 3)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                                    if (numberOfClicks == 4)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_NEXT);
                                    break;

                                case R.id.action2Pause3Previous4Next:
                                    if (numberOfClicks == 2)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                                    if (numberOfClicks == 3)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                                    if (numberOfClicks == 4)
                                        mediaButtonControl(KeyEvent.KEYCODE_MEDIA_NEXT);
                                    break;


                            }
                            numberOfClicks = 0;
                        }
                    }, preferences.getPrefInt(getString(R.string.multiple_delay), 0) * DELAY_STEP + DELAY_STEP);
                }
            }
        });
    }

    private void mediaButtonControl(int keycode) {
        KeyEvent ky_down = new KeyEvent(KeyEvent.ACTION_DOWN, keycode);
        KeyEvent ky_up = new KeyEvent(KeyEvent.ACTION_UP, keycode);
        AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        am.dispatchMediaKeyEvent(ky_down);
        am.dispatchMediaKeyEvent(ky_up);

    }

    @Override
    public void onDestroy() {
        Log.i("kek", "service onDestroy");
        stopForeground(true);
        stopSelf();
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.cancelAll();
        }
    }
}

