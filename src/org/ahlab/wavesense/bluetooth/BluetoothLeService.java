/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ahlab.wavesense.bluetooth;

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
import android.util.Log;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

import org.ahlab.wavesense.data.BLeData;;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    boolean fillBuffer = false;

    long currentTime, previousTime = 0;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
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
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                //Log.w(TAG, "onServicesDiscovered received: " + status);
            }
            //Log.w(TAG, "onServicesDiscovered received: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            //Log.i(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //Log.i(TAG, "onCharacteristicChanged");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        //Log.w(TAG, "broadcastUpdate-single: " + action);
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }



    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        if (characteristic.getUuid().equals(UUID.fromString(SampleGattAttributes.ZSENSE_CHARACTERISTIC))) {
            currentTime = System.currentTimeMillis();
            //Log.i(TAG,"Duration: " + (currentTime - previousTime));
            previousTime = currentTime;
            final byte[] data = characteristic.getValue();
            byte[] decodedData = decodeData(data);
            if (data != null && data.length > 0) {
                //Log.i(TAG,"Data Length: " + data.length);
                //Log.i(TAG,"Decoded Data Length: " + decodedData.length);
                /*for(byte eachbyte : data) {
                    Byte b = eachbyte;
                    int nn = b.intValue();
                    Log.i(TAG,"" + nn);
                    if(nn==0){
                        //Log.i(TAG,"START" + nn);
                        BLeData.getInstance().resetBLeDataBuffer();
                        fillBuffer = true;
                        Log.i(TAG, "broadcastUpdate: " + BLeData.getInstance().getIterationNumber() + " " + Arrays.toString(BLeData.getInstance().getBLeDataBuffer()));
                        intent.putExtra(EXTRA_DATA, BLeData.getInstance().getBLeDataBuffer());
                        sendBroadcast(intent);
                        continue;
                    }
                    else if(BLeData.getInstance().getBufferFilled() >= BLeData.getInstance().getNumberOfInputs())continue;
                    else if(fillBuffer) {
                        BLeData.getInstance().addData(nn);
                        continue;
                    }
                }*/
                int i = 0;
                BLeData.getInstance().resetBLeDataBuffer();
                fillBuffer = false;
                for(byte eachbyte : decodedData){
                    int nn = ((Byte)eachbyte).intValue();
                    //Log.i(TAG,"" + nn);
                    if(i == 0){
                        fillBuffer = true;
                        i++;
                        BLeData.getInstance().addData(nn);
                        continue;
                    }
                    else if(BLeData.getInstance().getBufferFilled() >= BLeData.getInstance().getNumberOfInputs())continue;
                    else if(fillBuffer) {
                        BLeData.getInstance().addData(nn);
                    }
                }
                //Log.i(TAG, "broadcastUpdate: " + BLeData.getInstance().getIterationNumber() + " " + Arrays.toString(BLeData.getInstance().getBLeDataBuffer()));
                intent.putExtra(EXTRA_DATA, BLeData.getInstance().getBLeDataBuffer());
                sendBroadcast(intent);
            }
        }
        else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                Log.i(TAG, "broadcastUpdate: " + new String(data) + "\n" + stringBuilder.toString());
            }
        }

    }

    private byte[] decodeBleData(byte[] bytes){
        BitSet bits = byteArray2BitArray(bytes);
        byte[] byteArray = new byte[((int)(bits.length()/7))];
        for(int byteNumber = 0; byteNumber < ((int)(bits.length()/7)); byteNumber++){
            bits.get(byteNumber * 8, (byteNumber * 7) + 7);
        }
        return byteArray;
    }

    private byte[] decodeData(byte[] p_encoded_buffer){
        int MAX_ZS_LEN = 22;
        byte[] mask = {63, 31, 15, 7, 3, 1};
        byte[] result = new byte[MAX_ZS_LEN];
        int i, ind, bte, bte1, ofs;

        for(i = 0; i < MAX_ZS_LEN; i++ ){
            ind = i * 7; // current data byte should start at this bit
            bte = ind / 8; // ind is at this byte of transmission stream
            bte1 = (ind + 7) / 8; // next byte next ind's  byte of transmission stream
            ofs = ind % 8; // ind is at this offset of the byte of transmission stream
            if(ofs == 1){ // not zero
                result[i] = (byte)((p_encoded_buffer[bte]) & 0x7F);
            }else if(ofs == 0){
                result[i] = (byte)((p_encoded_buffer[bte] >> 1) & 0x7F);
            }else{
                result[i] = (byte)((p_encoded_buffer[bte] << (ofs - 1)) & 0x7F);
                result[i] |= (p_encoded_buffer[bte1] >> (9 - ofs)) & mask[7-ofs];
            }
        }
        return result;
    }

    private BitSet byteArray2BitArray(byte[] bytes) {
        BitSet bits = new BitSet(bytes.length * 8);
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0)
                bits.set(i);
        }
        return bits;
    }

    public static int twoBytesToShort(byte b1, byte b2) {
        return  ((b2 << 8) | (b1 & 0xFF));
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
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
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
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
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.w(TAG, "setCharacteristicNotification" + characteristic.getUuid());
        if (characteristic.getUuid().equals(UUID.fromString(SampleGattAttributes.ZSENSE_CHARACTERISTIC))) {
            //Log.w(TAG, "ENABLE_NOTIFICATION_VALUE");
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }

    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
