package org.ahlab.wavesenselib.ui;

/**
 * Created by Shanaka on 7/6/2015.
 */

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ahlab.wavesenselib.R;
import org.ahlab.wavesenselib.data.BLeDevicesList;

public class BluetoothScanActivity extends Activity {
//public class BluetoothScanActivity extends VrActivity {
    private final static String TAG = BluetoothScanActivity.class.getSimpleName();

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 5000;
    private static final int REQUEST_ENABLE_BT = 1;
    //private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private boolean mConnected = false;
    //private boolean mScanning;

    public static final int MAX_TIME = (int) SCAN_PERIOD/1000;      // 10s countdown timer
    public static final int START_TIME = (int) SCAN_PERIOD/1000;    // Countdown from 30 to zero

    private static CircularProgressDrawable mCircularProgressTimer;

    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private ImageView mCircularImageView;
    private TextView mtextView;
    private ImageButton mRescan;
    private Button mUSB;

    private Handler mHandler;
    
    ListView mListView;
    public final static String EXTRA_DEVICE_ADDRESS = "org.ahlab.wavesense.ui.DEVICE_ADDRESS";
    
    public static CheckBox checkBoxStream;
    public AutoCompleteTextView autoCompleteTextViewIP;
    
    public static final String SERVER_IP_ADDRESS = "server_ip_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //interfacePtr = nativeSetAppInterface(this);
        setContentView(R.layout.activity_bluetooth_scan);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        /*Intent intent = getIntent();
        String commandString = VrActivity.getCommandStringFromIntent( intent );
        String fromPackageNameString = VrActivity.getPackageStringFromIntent( intent );
        String uriString = VrActivity.getUriStringFromIntent( intent );*/
        
        /*interfacePtr = nativeSetAppInterface(this, fromPackageNameString,
                commandString, uriString);*/        
        //setAppPtr(interfacePtr);

        mHandler = new Handler();
        initBluetooth();
        
        mtextView = (TextView) findViewById(R.id.textViewScanning);
        // Get the list component from the layout of the activity
        mListView =
                (ListView) findViewById(R.id.device_list);
        
        addListenerOnimageButtonRescan();
        addingBluetoothScanProgress();
        addOnItemClickListenerOnList();
        
        //BLeDevicesList.getInstance().removeAllBLeDevices();   
        //updateList();
        
        updateReScanUis(true);
        scanLeDevice(false);
        
        autoCompleteTextViewIP = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewIP);
        autoCompleteTextViewIP.setText(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(SERVER_IP_ADDRESS, ""));
                
        checkBoxStream = (CheckBox) findViewById(R.id.checkBoxStream);
		checkBoxStream
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked)
							autoCompleteTextViewIP.setEnabled(true);
						else
							autoCompleteTextViewIP.setEnabled(false);
					}
				});
    }
    
    public void addOnItemClickListenerOnList() {
    	// Set a click listener
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
            	
            	if(checkBoxStream.isChecked() && autoCompleteTextViewIP.getText().toString().equals("")){
            		autoCompleteTextViewIP.setError("Set IP Address of Server");
            		return;
            	}
            	else if(checkBoxStream.isChecked() && !autoCompleteTextViewIP.getText().toString().equals("")){
            		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString(SERVER_IP_ADDRESS, autoCompleteTextViewIP.getText().toString()).apply();
            	}            		
                
                final BluetoothDevice device = BLeDevicesList.getInstance().getBLeDevice(position);
                if (device == null)
                	return;

                Intent myIntent = new Intent(BluetoothScanActivity.this, MainActivity.class);
                myIntent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
                startActivity(myIntent);               
            }
        });
    }

    public void addListenerOnimageButtonRescan() {
        mRescan = (ImageButton) findViewById(R.id.imageButtonRescan);
        mRescan.setVisibility(View.GONE);
        mRescan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {                
                updateReScanUis(false);
                scanLeDevice(true);
                mCircularProgressTimer.start();
            }
        });
    }    

    @SuppressLint("NewApi") private void initBluetooth(){
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        BLeDevicesList.getInstance().removeAllBLeDevices();
        updateList();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanLeDevice(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                    if(BLeDevicesList.getInstance().getNumberOfDevices() > 0){
                    	updateReScanUis(true);
                    	scanLeDevice(false);
                    	updateList();
                    }
                    else {
                        Toast.makeText(getApplication(), R.string.no_ble_device, Toast.LENGTH_SHORT).show();
                        updateReScanUis(true);
                    }
                }
            }, SCAN_PERIOD);
            mBluetoothLeScanner.startScan(mScanCallback);
        } else {
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
    }

    public void callingDeviceListActivity(){
        /*Intent myIntent = new Intent(BluetoothScanActivity.this, DeviceListActivity.class);
        startActivity(myIntent);*/
    }    
    
    private void updateList() {
    	// Assign an adapter to the list
    	//Log.w(TAG, "updateList: " + BLeDevicesList.getInstance().getBLeDevicesList().size());
        mListView.setAdapter(new LeDeviceListAdapter(BluetoothScanActivity.this, BLeDevicesList.getInstance().getBLeDevicesList()));
        //mListView.invalidate();
    }

    private void updateReScanUis(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Visible/Gone
                if (enable) {
                    //Log.w(TAG, "updateReScanUis-enable");
                    mtextView.setText(getResources().getString(R.string.rescan));
                    mCircularImageView.setVisibility(View.GONE);
                    mRescan.setVisibility(View.VISIBLE);

                    RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    p.addRule(RelativeLayout.BELOW, R.id.imageButtonRescan);
                    findViewById(R.id.relativeLayoutScanningText).setLayoutParams(p);
                } else {
                    mtextView.setText(getResources().getString(R.string.scanning));
                    mRescan.setVisibility(View.GONE);
                    mCircularImageView.setVisibility(View.VISIBLE);

                    RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    p.addRule(RelativeLayout.BELOW, R.id.imageviewscan);
                    findViewById(R.id.relativeLayoutScanningText).setLayoutParams(p);
                }
            }
        });
    }

    // Device scan callback.
    private ScanCallback mScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    //Log.w(TAG, "onScanResult");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BLeDevicesList.getInstance().addBLeDevice(result.getDevice());
                            //Log.w(TAG, "Adding: " + status);
                            //Log.w(TAG, "onScanResult-run & Device name: " + result.getDevice().getName());
                            //Log.w(TAG, "Number of Devices: " +  BLeDevicesList.getInstance().getNumberOfDevices());
                        }
                    });
                }
            };

    private void addingBluetoothScanProgress(){
        // Get a reference of our ImageView layout component to be used
        // to display our circular progress timer.
        mCircularImageView = (ImageView) findViewById(R.id.imageviewscan);

        // Create an instance of a drawable circular progress timer
        mCircularProgressTimer = new CircularProgressDrawable(START_TIME,
                MAX_TIME, CircularProgressDrawable.Order.DESCENDING, getApplication());

        // Set a callback to update our circular progress timer
        mCircularProgressTimer.setCallback(mPieDrawableCallback);

        // Set a drawable object for our Imageview
        mCircularImageView.setImageDrawable(mCircularProgressTimer);
    }

    private Drawable.Callback mPieDrawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            // Redraw our image with updated progress timer
            mCircularImageView.setImageDrawable(who);
        }

        // Empty placeholder
        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
        }

        // Empty placeholder
        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
        }
    };
    
    private static final class LeDeviceListAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private ArrayList<BluetoothDevice> mLeDevices;

        // Provide a suitable constructor (depends on the kind of dataset)
        public LeDeviceListAdapter(Context context, ArrayList<BluetoothDevice> dataset) {
        	super();
            mInflater = LayoutInflater.from(context);
            mLeDevices = dataset;
        }

        // Provide a reference to the type of views you're using
        public static class ItemViewHolder{
            private TextView textViewDeviceName;
            private TextView textViewDeviceAddress;
        }

        // Create new views for list items
        // (invoked by the WearableListView's layout manager)
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ItemViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflater.inflate(R.layout.listitem_device, null);
                viewHolder = new ItemViewHolder();
                viewHolder.textViewDeviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.textViewDeviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ItemViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.textViewDeviceName.setText(deviceName);
            else
                viewHolder.textViewDeviceName.setText(R.string.unknown_device);
            viewHolder.textViewDeviceAddress.setText(device.getAddress());

            return view;
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }
    }
    
}
