package org.ahlab.wavesense.data;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

/**
 * Created by Shanaka on 7/8/2015.
 */
public class BLeDevicesList {
    private final static String TAG = BLeDevicesList.class.getSimpleName();

    private static BLeDevicesList instance = null;
    private ArrayList<BluetoothDevice> mLeDevices = null;
    private BLeDevicesList() {
        //Log.w(TAG, "BLeDevicesList constructor");
        mLeDevices = new ArrayList<BluetoothDevice>();
    }

    public static BLeDevicesList getInstance() {
        //Log.w(TAG, "BLeDevicesList getInstance");
        if(instance == null) {
            //Log.w(TAG, "BLeDevicesList getInstance - inside");
            instance = new BLeDevicesList();
        }
        return instance;
    }

    public ArrayList<BluetoothDevice> getBLeDevicesList(){
        return new ArrayList<BluetoothDevice>(mLeDevices);
    }

    public boolean addBLeDevice(BluetoothDevice device){
        boolean status = false;
        if(!mLeDevices.contains(device)) {
            status = mLeDevices.add(device);
        }
        return status;
    }

    public BluetoothDevice getBLeDevice(int deviceId){
        return mLeDevices.get(deviceId);
    }

    public int getNumberOfDevices(){
        return mLeDevices.size();
    }

    public void removeBLeDevice(int deviceId){
        mLeDevices.remove(deviceId);
    }
    public boolean removeAllBLeDevices(){
        return mLeDevices.removeAll(mLeDevices);
    }
}
