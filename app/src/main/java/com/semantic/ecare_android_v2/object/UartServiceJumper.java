package com.semantic.ecare_android_v2.object;

import java.util.List;
import java.util.UUID;

import com.semantic.ecare_android_v2.util.Constants;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

@SuppressLint("NewApi")
public class UartServiceJumper extends Service {

	private final static String TAG = UartServiceJumper.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.jumper.uart.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.jumper.uart.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.jumper.uart.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.jumper.uart.ACTION_DATA_AVAILABLE";

    public final static String ACTION_DATA_Notification =
            "com.jumper.uart.ACTION_DATA_Notification";
    public final static String EXTRA_DATA =
            "com.jumper.uart.EXTRA_DATA";
    public final static String EXTRA_TEMPERATURE =
            "com.jumper.uart.EXTRA_TEMPERATURE";
    public final static String EXTRA_SATURATION =
            "com.jumper.uart.EXTRA_SATURATION";
    public final static String EXTRA_FREQUENCE =
            "com.jumper.uart.EXTRA_FREQUENCE";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART =
            "com.jumper.uart.DEVICE_DOES_NOT_SUPPORT_UART";
    public final static String ACTION_GET_TEMPERATURE =
            "com.jumper.uart.GET_TEMPERATURE";
    public final static String ACTION_GET_OXYMETRE =
            "com.jumper.uart.GET_OXYMETRE";

    static int nbr=4;


/*
    public static final UUID SERVICE_UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_CHAR_UUID = UUID.fromString("0000FFF7-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_CHAR_UUID = UUID.fromString("0000FFF6-0000-1000-8000-00805f9b34fb");
*/



    public static final UUID SERVICE_UUID = UUID.fromString("CDEACB80-5235-4C07-8846-93A37EE6B86D");
    public static final UUID RX_CHAR_UUID = UUID.fromString("CDEACB81-5235-4C07-8846-93A37EE6B86D");
    public static final UUID TX_CHAR_UUID = UUID.fromString("CDEACB82-5235-4C07-8846-93A37EE6B86D");


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt );


                List<BluetoothGattService> RxServiceList= mBluetoothGatt.getServices();
                for(BluetoothGattService service: RxServiceList)
                {
                    Log.e(TAG, " +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                    Log.e(TAG, " +++++++ service: "+service.toString()+"  ::::  "+service.getUuid());
                    List<BluetoothGattCharacteristic> characteristics= service.getCharacteristics();
                    for(BluetoothGattCharacteristic characteristic: characteristics)
                    {
                        Log.e(TAG, " -----   characteristic: "+characteristic.toString()+"  ::::  "+characteristic.getUuid());
                    }
                }


                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            broadcastUpdate(ACTION_DATA_Notification, characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            // TODO Auto-generated method stub
            super.onReadRemoteRssi(gatt, rssi, status);
        }

    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (RX_CHAR_UUID.equals(characteristic.getUuid())) {

            byte[] data=characteristic.getValue();

            Log.e(TAG, "Received RX : "+ Tools.hex2HexString(data));
            //Log.e(TAG, "Received RX : "+ data[0]+"   "+data[1]+"   "+data[2]+"   "+data[3]+"   "+data[4]+"   "+data[5]+"   "+data[6]+"   "+data[7]+"   ");
            //Log.e(TAG, "suite : "+ data[8]+"   "+data[9]+"   "+data[10]+"   "+data[11]+"   "+data[12]+"   "+data[13]+"   "+data[14]+"   "+data[15]+"   ");
            int byte0 = data[0] & 0xff;

            if(byte0== Constants.BYTE0_OXY) //0x81 oxymètre
            {
                Log.e(TAG, " reception oxy ");
                if(data[1]!=0xFF && data[2]!=0x7F && data[3]!=0x00)
                {
                    final Intent intentOxy = new Intent(ACTION_GET_OXYMETRE);
                    intentOxy.putExtra(EXTRA_SATURATION, (int)data[2]);
                    intentOxy.putExtra(EXTRA_FREQUENCE, (int)data[1]);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intentOxy);
                }
            }

            if(byte0== Constants.BYTE0_THERMO) //0xAA thermomètre
            {
                Log.e(TAG, " reception temperature ");
                final Intent intentTemp = new Intent(ACTION_GET_TEMPERATURE);
                float temp= Tools.twoByteToInt(data[3],data[2]);
                temp= temp/100;

                intentTemp.putExtra(EXTRA_TEMPERATURE, temp);

                LocalBroadcastManager.getInstance(this).sendBroadcast(intentTemp);
            }


            intent.putExtra(EXTRA_DATA, data);
        } else {
            byte[] data=characteristic.getValue();
            Log.d(TAG, "Received TX ");
            for(int i=0; i<data.length;i++)
            {
                String hexa = String.format("%06X", data[i]);
                Log.e("data", "read: "+i+"  "+hexa);
            }
            nbr++;
            Log.e("data", "+++++++++++++++++++++++++"+nbr);

            //  Log.d(TAG, String.format("Received TX: %d",data));
            intent.putExtra(EXTRA_DATA, data);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //����server���������ⲿ����
    public class LocalBinder extends Binder {
    	public UartServiceJumper getService() {
            Log.e("data", "+++++++++++++++++++++++++ LocalBinder jumper");
            return UartServiceJumper.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.e("data", "+++++++++++++++++++++++++ onBind jumper");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e("data", "+++++++++++++++++++++++++ onUnbind jumper");
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }



    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        Log.i(TAG, "init...");
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        Log.d(TAG, "CONNECT ....");
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
       
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
        
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        // mBluetoothGatt.close();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.w(TAG, "mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);


        if (TX_CHAR_UUID.equals(characteristic.getUuid()))
        {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(TX_CHAR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        //amine
        else
        {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(RX_CHAR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }

    }

    /**
     * Enable TXNotification
     *
     * @return
     */
    public void enableTXNotification()
    {
        if (mBluetoothGatt == null) {
            showMessage("mBluetoothGatt null" + mBluetoothGatt);
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattService RxService = mBluetoothGatt.getService(SERVICE_UUID);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattService TxService = mBluetoothGatt.getService(SERVICE_UUID);
        BluetoothGattCharacteristic TxChar = TxService.getCharacteristic(RX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar,true);
        List<BluetoothGattDescriptor> bl=TxChar.getDescriptors();
        BluetoothGattDescriptor descriptor=bl.get(0);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void writeRXCharacteristic(byte[] value)
    {
        Log.e(TAG," write: daaaaaaaaaaaaaay");
        BluetoothGattService RxService = mBluetoothGatt.getService(SERVICE_UUID);
        showMessage("mBluetoothGatt null"+ mBluetoothGatt);

        if (RxService == null) {

            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Rx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        TxChar.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(TxChar);

        Log.d(TAG, "write TXchar - status=" + status);

        char[] digital = "0123456789ABCDEF".toCharArray();
        StringBuffer sb = new StringBuffer("");
        for(byte d:value)
        {
            sb.append(digital[(d&0xf0)>>8]);
            sb.append(digital[d&0x0f]);
            sb.append(" ");
        }
        showMessage("TX data:"+sb.toString());
        enableTXNotification();
        readCharacteristic(RxChar);
    }

    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }
	
}