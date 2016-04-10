package org.ahlab.wavesense.ui;

import java.util.List;
import java.util.UUID;

import org.ahlab.wavesense.bluetooth.BluetoothLeService;
import org.ahlab.wavesense.bluetooth.SampleGattAttributes;
import org.ahlab.wavesense.classification.RecognitionManager;
import org.ahlab.wavesense.data.BLeData;
import org.ahlab.wavesense.data.WSQueue;
import org.ahlab.wavesenselib.R;

import com.unity3d.player.UnityPlayerActivity;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends UnityPlayerActivity{
	private static final String TAG = MainActivity.class.getSimpleName();
	
	private String mDeviceAddress;	
	private boolean mServiceBound = false;
	private boolean mConnected = false;
	private BluetoothLeService mBluetoothLeService;
	List<BluetoothGattService> bluetoothGattServices;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	
	long currentTime, previousTime, twoFingerPreviousTime = 0;
    private static final long GESTURE_UPDATE_THRESHOLD_PERIOD = 1000;
    private int currentGesture = -1;
    
    int[] dataarray;
	
	public static Context appContext;
	public static MainActivity instance;
	
	TextView textviewMain;
	
	WSQueue wSQueue;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //setContentView(R.layout.activity_main);        
        //textviewMain = (TextView)findViewById(R.id.textviewMain);
        instance = this;
        wSQueue = WSQueue.getInstance();
        
        // Get the message from the intent
        Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(BluetoothScanActivity.EXTRA_DEVICE_ADDRESS);
        
        new Thread(new Runnable() {
            public void run() {
                RecognitionManager.getInstance(getApplicationContext());
                BLeData.getInstance().setNumberOfInputs(RecognitionManager.getInstance(getApplicationContext()).getNumberOfInputs());
            }
        }).start();
        
        if(!mServiceBound){
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            mServiceBound = true;
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }

        appContext = getApplication();        
    }	        

    @Override
    protected void onResume() {
        super.onResume();
        
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();        

        try{
            unregisterReceiver(mGattUpdateReceiver);
        }
        catch(IllegalArgumentException receiverNotRegisteredException){
            receiverNotRegisteredException.printStackTrace();
        }

        BLeData.getInstance().resetIterationNumber();        
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       
        mBluetoothLeService.disconnect();
        bluetoothGattServices = null;
        if(mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        mBluetoothLeService = null;
    }
    
 // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //Log.w(TAG, "ACTION: " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                //Log.w(TAG, "ACTION_GATT_CONNECTED");
                mConnected = true;                
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);                
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {                
                //Log.w(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                bluetoothGattServices = mBluetoothLeService.getSupportedGattServices();
                updateConnectionState(R.string.connected);
                startReceivingData();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //Log.i(TAG, "ACTION_DATA_AVAILABLE");
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                dataarray = intent.getIntArrayExtra(BluetoothLeService.EXTRA_DATA);                
                //Log.i(TAG, "dataArray: " + BLeData.getInstance().getIterationNumber() + " " + Arrays.toString(dataarray));

                int predictionNumber = invokePrediction(dataarray);
                //Log.w(TAG, "Prediction Number: " + predictionNumber);
                currentTime = System.currentTimeMillis();
                if(((currentTime - previousTime) > GESTURE_UPDATE_THRESHOLD_PERIOD)){                 	
                	switch (predictionNumber) {
                    	case 0:  
                    		wSQueue.enqueue("leftswipe");
                    		Log.w(TAG, "Prediction: " + "leftswipe");
                    		previousTime = currentTime;
                    		break;
                    	case 1:  
                    		wSQueue.enqueue("rightswipe");
                    		Log.w(TAG, "Prediction: " + "rightswipe");
                    		previousTime = currentTime;
                    		break;
                    	case 2:  
                    		wSQueue.enqueue("push");
                    		Log.w(TAG, "Prediction: " + "pull");
                    		previousTime = currentTime;
                    		break;
                    	case 3:  
                    		wSQueue.enqueue("pull");
                    		Log.w(TAG, "Prediction: " + "pull");
                    		previousTime = currentTime;
                    		break;
                	}                	                    
                }                
                
                //Log.w(TAG, "Prediction Number: " + predictionNumber);
                /*currentTime = System.currentTimeMillis();
                if (predictionNumber != -1 && predictionNumber != 10 && predictionNumber != 20 && ((currentTime - previousTime) > GESTURE_UPDATE_THRESHOLD_PERIOD)) {
                    if(predictionNumber != 30){
                    	writePredictionToNative(predictionNumber);
                    	Toast.makeText(getApplication(), predictionNumber, Toast.LENGTH_SHORT).show();                    	
                    	Log.w(TAG, "Prediction Number: " + predictionNumber);
                    }                    
                    previousTime = currentTime;
                    currentGesture = predictionNumber;
                }*/
                //writePredictionToNative(predictionNumber);
            }
        }
    };
    
    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	Toast.makeText(getApplication(), resourceId, Toast.LENGTH_SHORT).show();                
            }
        });
    }
    
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
    private int invokePrediction(int[] dataarray){
        try{
            return RecognitionManager.getInstance(getApplicationContext()).predict(dataarray);
        }
        catch(InterruptedException iExp){
            iExp.printStackTrace();
        }
        return -1;
    }
    
    private void startReceivingData() {
        //Log.i(TAG, "startReceivingData");
        for (BluetoothGattService service : bluetoothGattServices) {
            //Log.w(TAG, "services: " + service.getUuid());
            if (service.getUuid().equals(UUID.fromString(SampleGattAttributes.ZSENSE_SERVICE))) {
                //Log.i(TAG, "service UART");
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    if (characteristic.getUuid().equals(UUID.fromString(SampleGattAttributes.ZSENSE_CHARACTERISTIC))) {
                        //Log.i(TAG, "characteristic UART TX");
                        handleGattCharacteristic(characteristic);
                    }
                }
            }
        }
    }

    private void handleGattCharacteristic(BluetoothGattCharacteristic characteristic) {
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            //Log.i(TAG, "PROPERTY_READ");
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic != null) {
                mBluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            mBluetoothLeService.readCharacteristic(characteristic);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            //Log.i(TAG, "PROPERTY_NOTIFY");
            mNotifyCharacteristic = characteristic;
            mBluetoothLeService.setCharacteristicNotification(
                    characteristic, true);
        }
    }
    
    public static Context getAppContext() {
        return appContext;
    }
    
    public String getGesture(){
    	String gesturemsg = wSQueue.dequeue();
    	Log.w(TAG, "Prediction in  getGesture(): " + gesturemsg);
    	return gesturemsg;
    }
    
    public String checkLoad(){
		return "WSQueueLoaded";
	}
}
