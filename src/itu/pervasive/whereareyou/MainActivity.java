package itu.pervasive.whereareyou;
//prototype v2.0
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.metaio.tools.io.AssetsManager;

public class MainActivity extends Activity {

	private BluetoothAdapter mBluetoothAdapter;
	private int REQUEST_ENABLE_BT = 1;
	private List<Map<String,String>> names_present_devices;
//	private List<Map<String,String>> devices;
	private List<Map<String,String>> paired_devices;
	private List<Map<String,String>> present_devices;
	private String[] device_to_search;
	private ListView lv_devices;
	private SimpleAdapter simpleAdpt;
	private boolean indoor = true;



	@Override
 	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// extract all the assets
		try {
//			devices = new ArrayList<Map<String,String>>();
			paired_devices = new ArrayList<Map<String,String>>();
			lv_devices = (ListView) findViewById(R.id.listView1);
			//metaio assets
			AssetsManager.extractAllAssets(this, true);
			//start bluetooth
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) { // Device does not support Bluetooth
				alert("Device does not support Bluetooth",null);
			}else if (!mBluetoothAdapter.isEnabled()) {//bluetooth is not enabled
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
			//load friends in screen
			loadFriends();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.w("error", e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * onClick button FindAll!
	 * @param v
	 */
	public void btnFindAll_OnClick(View v){
		//start metaio intent
		device_to_search[0] = "Pervasive_758";
		device_to_search[1] = "08:37:3D:C7:2B:FD";
		startMetaioIntent();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		menu.add(0, 2, 0,"Refresh List");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case 2:
			loadFriends();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 * destroy method, unregister everything if needed
	 * disable bluetooth adapter
	 */
	public void onDestroy(){
		super.onDestroy();
		try{
			Log.w("destroy","destroooy");
			unregisterReceiver(getPresentPairedDevices);
			mBluetoothAdapter.cancelDiscovery();
			mBluetoothAdapter.disable();			
		}catch(Exception e){
			Log.w("onDestroy", e.toString());
		}
	}


	/*
	 * 
	 * loadFriends fills the initial list with the available friends
	 * 
	 */
	private void loadFriends(){
		//get paired and present devices of the device
		present_devices = new ArrayList<Map<String,String>>();
		names_present_devices = new ArrayList<Map<String,String>>();
		simpleAdpt = new SimpleAdapter(this, names_present_devices, android.R.layout.simple_list_item_1
				, new String[] {"device"}, new int[] {android.R.id.text1});
		lv_devices.setAdapter(simpleAdpt);
		lv_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				TextView clickedView = (TextView) view;
				String device_name = clickedView.getText().toString();
				for (Map<String, String> device : present_devices)
				{
					//08:37:3D:C7:2B:FD
					String mac_address = device.get(device_name);
					if(mac_address != null){
						Toast.makeText(MainActivity.this, "Look for the red cross in \n the radar and click it!", Toast.LENGTH_LONG).show();
						device_to_search = new String[]{ device_name,mac_address };
					}
				}

				//activity = Class.forName(getPackageName()+".ARActivity");
				//Intent i  = new Intent(getApplicationContext(), activity);
				startMetaioIntent();
			}
		});
		getPairedDevices();
	}

	/**
	 * Create HashMap with key and string
	 * @param key
	 * @param name
	 * @return HashMap
	 */
	private HashMap<String,String> newName(String key, String name){
		HashMap<String, String> test = new HashMap<String, String>();
		test.put(key, name);
		return test;
	}

	/*
	 * 
	 */
	private void startMetaioIntent(){
		try{
			unregisterReceiver(getPresentPairedDevices);
			mBluetoothAdapter.cancelDiscovery();
		}catch(Exception e){}
		Intent intent  = new Intent(getApplicationContext(), ARActivity.class);
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		intent.putExtra("device", device_to_search);
		intent.putExtra("indoor", indoor);
		startActivity(intent);
	}

	private void alert(String left, String right){
		AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
		// Add the buttons
		if(left != null){
			builder.setPositiveButton(left, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User clicked OK button
				}
			});
		}
		if(right != null){
			builder.setNegativeButton(right, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User cancelled the dialog
				}
			});
		}

		// Create the AlertDialog
		AlertDialog dialog = builder.create();
	}

	private void getPairedDevices(){
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices) {
				// Add the name and address to an array
//				Log.w("paired","device - "+device.getName());
				paired_devices.add(newName(device.getName(),device.getAddress()));
				//				names_devices.add(newName("device",device.getName()));
				//				devices.add(newName(device.getName(),device.getAddress()));
			}
		}
		//get present devices from paired devices
		mBluetoothAdapter.startDiscovery();
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(getPresentPairedDevices, filter);
	}
	/*
	 *  Create a BroadcastReceiver for ACTION_FOUND
	 *  when device is found it is processed if it is in the list
	 *  of known devices - NOT ON PAIRED DEVICES
	 */
	private final BroadcastReceiver getPresentPairedDevices = new BroadcastReceiver() {
		@Override		
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.w("found","found - "+device.getName());
				if(paired_devices.contains(newName(device.getName(),device.getAddress())) ){
					//present device
					present_devices.add(newName(device.getName(),device.getAddress()));
					names_present_devices.add(newName("device",device.getName()));
					simpleAdpt.notifyDataSetChanged();
					Log.w("BT","device found - "+device.getName());
				}
			}
		}
	};

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 * Intent for enabling Bluetooth
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == REQUEST_ENABLE_BT){
			if(resultCode == RESULT_OK){
				// Register the BroadcastReceiver
				loadFriends();
			}else {
				//				toast("result NOT_OK - request_enable_bt \n");
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}
	
	/*
	 * onToggleButton click
	 * set environment for the app
	 */
	public void onToggleClicked(View view) {
	    // Is the toggle on?
	    boolean on = ((Switch) view).isChecked();
	    if (on) {
	    	indoor = false;
	    } else {
	    	indoor = true;
	    }
	}
}
