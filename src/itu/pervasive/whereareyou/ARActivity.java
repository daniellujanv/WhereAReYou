package itu.pervasive.whereareyou;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.SensorsComponentAndroid;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.IRadar;
import com.metaio.sdk.jni.LLACoordinate;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;

public class ARActivity extends ARViewActivity implements SensorsComponentAndroid.Callback {



	/**
	 * Geometries
	 */
	//		private IGeometry mGeometrySouth;
	//		private IGeometry mGeometryWest;
	//		private IGeometry mGeometryNorth;
	private List<IGeometry> friends;
	float[] previous_orientation = new float[]{0,0,0};
	private IRadar mRadar;
	private String[] device_to_search = new String[2];
	private BluetoothAdapter mBluetoothAdapter;
	private SearchingManager searchManager;
	private int REQUEST_ENABLE_BT = 1;
	private BtPositioningAll btPositioningAll;
	private boolean goRead = true;
	private ProgressBar wholeProgress;
	private ProgressBar individualProgress;
	private List<String> devices;
	private Device device;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		wholeProgress = (ProgressBar) this.mGUIView.findViewById(R.id.wholeprogressbar);
		individualProgress = (ProgressBar) this.mGUIView.findViewById(R.id.individualprogressbar);
		devices = new ArrayList<String>();

		Intent intent = getIntent();
		boolean allDevices = intent.getBooleanExtra("devices", true);
		if(!allDevices){
			device_to_search = intent.getStringArrayExtra("device");
		}else{
			device_to_search[0] = "Pervasive_758";
			device_to_search[1] = "08:37:3D:C7:2B:FD";
		}
		devices.add(device_to_search[1]);
		device = new Device(device_to_search[0],device_to_search[1]);
		//TO-DO if users wants to search all devices add them to the list

		searchDevices();
		//initialize friend list
		friends = new ArrayList<IGeometry>();
		// Set GPS tracking configuration
		// The GPS tracking configuration must be set on user-interface thread
		boolean result = metaioSDK.setTrackingConfiguration("GPS");  
		MetaioDebug.log("Tracking data loaded: " + result);  
		Log.w("Tracking data loaded", (result == true)? "true":"false");    
	}

	@Override
	protected void onPause() 
	{
		super.onPause();
		// remove callback
		if (mSensors != null)
		{
			mSensors.registerCallback(null);
			//	mSensorsManager.pause();
		}
	}

	@Override
	protected void onResume() 
	{
		super.onResume();
		// Register callback to receive sensor updates
		if (mSensors != null)
		{
			mSensors.registerCallback(this);
			//mSensorsManager.resume();
		}
	}

	public void onButtonClick(View v)
	{
		finish();
	}

	@Override
	protected int getGUILayout() 
	{
		return R.layout.activity_ar;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() 
	{
		return null;
	}

	@Override
	protected void loadContents() 
	{
		try
		{ 
			//billboard
			friends.add(metaioSDK.createGeometryFromImage(createBillboardTexture(device_to_search[0])));
			friends.get(0).setScale(new Vector3d(15f,15f,15f));
			
			String filepath = AssetsManager.getAssetPath("Media/metaioman.md2");
			if (filepath != null) 
			{
				friends.add(metaioSDK.createGeometry(filepath));
				friends.get(1).startAnimation("idle", true);
				friends.get(1).setScale(new Vector3d( 80f, 80f, 80f));
				//						friends.get(2).startAnimation("idle", true);
				//						friends.get(2).setScale(new Vector3d(50f,50f,50f));
			}
			// create radar
			mRadar = metaioSDK.createRadar();
			mRadar.setBackgroundTexture(AssetsManager.getAssetPath("Media/radar.png"));
			mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath("Media/yellow.png"));
			mRadar.setRelativeToScreen(IGeometry.ANCHOR_BC);
			// add geometries to the radar
			//			mRadar.add(friends.get(0));
			mRadar.add(friends.get(1));
			//				mRadar.add(friends.get(2));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private String createBillboardTexture(String billBoardTitle)
	{
		try
		{
			final String texturepath = getCacheDir() + "/" + billBoardTitle + ".png";
			Paint mPaint = new Paint();
			// Load background image (256x128), and make a mutable copy
			Bitmap billboard = null;
			//reading billboard background
			String filepath = AssetsManager.getAssetPath("Media/POI_bg.png");
			Bitmap mBackgroundImage = BitmapFactory.decodeFile(filepath);
			billboard = mBackgroundImage.copy(Bitmap.Config.ARGB_8888, true);
			Canvas c = new Canvas(billboard);
			mPaint.setColor(Color.WHITE);
			mPaint.setTextSize(24);
			mPaint.setTypeface(Typeface.DEFAULT);
			float y = 30;
			float x = 30;
			// Draw POI name
			if (billBoardTitle.length() > 0)
			{
				String n = billBoardTitle.trim();
				final int maxWidth = 2000;
				int i = mPaint.breakText(n, true, maxWidth, null);
				c.drawText(n.substring(0, i), x, y, mPaint);
				// Draw second line if valid
				if (i < n.length())
				{
					n = n.substring(i);
					y += 20;
					i = mPaint.breakText(n, true, maxWidth, null);

					if (i < n.length())
					{
						i = mPaint.breakText(n, true, maxWidth - 20, null);
						c.drawText(n.substring(0, i) + "...", x, y, mPaint);
					} else
					{
						c.drawText(n.substring(0, i), x, y, mPaint);
					}
				}
			}
			// writing file
			try
			{
				FileOutputStream out = new FileOutputStream(texturepath);
				billboard.compress(Bitmap.CompressFormat.PNG, 90, out);
				MetaioDebug.log("Texture file is saved to "+texturepath);
				Log.w("ImageCreated","Texture file is saved to "+texturepath);
				return texturepath;
			} catch (Exception e) {
				MetaioDebug.log("Failed to save texture file");
				e.printStackTrace();
			}
			billboard.recycle();
			billboard = null;
		} catch (Exception e)
		{
			MetaioDebug.log("Error creating billboard texture: " + e.getMessage());
			MetaioDebug.printStackTrace(Log.DEBUG, e);
			e.printStackTrace();
			return null;
		}
		return null;
	}

	private void updateGeometriesLocation()
	{
		if(device.found){
			friends.get(0).setRotation(new Rotation((float)(Math.PI/2),0f,(float)Math.PI),true);
			friends.get(0).setTranslation(new Vector3d((device.orientation[0])*25000f,(device.orientation[1])*25000f, 1000f),false);
			friends.get(0).setRenderOrder(1);
			
			//25 mts away
			/* x =  1 -> east
			 * x = -1 -> west
			 * y =  1 -> north
			 * y = -1 -> south
			 */
			if((device.orientation[0]) == 0 && (device.orientation[1] == 0 )){
				dialog("Orientation not definitive, try again!","Ok", null);
			}
			friends.get(1).setTranslation(new Vector3d((device.orientation[0])*25000f,(device.orientation[1])*25000f, -7500f),false);
			friends.get(1).setRotation(new Rotation( 0f, 0f, (float)Math.PI)); //radians, PI == half turn
			friends.get(1).setRenderOrder(0);

		}
		
		
		//			// (1,0,0) east (0,1,0) north (0,0,1) down/hell?? //1000f == 1m
		//			if (friends.get(0) != null)
		//			{
		////				Vector3d vector_location = new Vector3d(0f,-10000f,-5500f);
		//				location.setLongitude(location.getLongitude()-OFFSET); //draw to west
		//				friends.get(0).setTranslationLLA(location);
		//				Log.w("LocationChange","0 - "+friends.get(0).getTranslationLLA());
		//				location.setLongitude(location.getLongitude()+OFFSET);
		////				friends.get(0).setTranslation(new Vector3d(-1000f,0f,0f),false);
		//			}
		//			
	}

	@Override
	protected void onGeometryTouched(final IGeometry geometry) 
	{
		MetaioDebug.log("Geometry selected: "+geometry);
		mSurfaceView.queueEvent(new Runnable()
		{
			@Override
			public void run() 
			{
				mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath("Media/yellow.png"));
				mRadar.setObjectTexture(geometry, AssetsManager.getAssetPath("Media/red.png"));
			}
		});
	}

	@Override
	public void onGravitySensorChanged(float[] gravity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onHeadingSensorChanged(float[] orientation) {
		// TODO Auto-generated method stub
		//		TextView text = (TextView)findViewById(R.id.sensorInfo);
		//		float orientation_0 = (float) Math.floor(orientation[0]);
		//		float orientation_1 = (float) Math.floor(orientation[1]);
		//		float orientation_2 = (float) Math.floor(orientation[2]);
		//		text.setText("[0x]" + orientation_0  + "[1y]" + orientation_1 + "[2z]" + orientation_2);
		////		try{
		////			friends.get(0).setTranslation(new Vector3d(/*orientation[0]-previous_orientation[0]*/ 0 ,orientation_1-previous_orientation[1],orientation_2-previous_orientation[2]), true);
		////			Log.w("LocationChange","0 - "+friends.get(0).getTranslation().toString());
		////		}catch(IndexOutOfBoundsException e){}
		//
		//		previous_orientation[0] = orientation_0;
		//		previous_orientation[1] = orientation_1;
		//		previous_orientation[2] = orientation_2;

	}

	@Override
	public void onLocationSensorChanged(LLACoordinate location)
	{
		MetaioDebug.log("Location changed: "+location);
		Log.w("LocationChange", "sensor changed - Location changed: "+location);
		//		updateGeometriesLocation(location);
	}

	/**
	 * Methods for bluetooth search
	 */

	/*
	 * Start the search for devices
	 */
	private void searchDevices(){
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		searchManager = new SearchingManager(); 
		if (mBluetoothAdapter == null) { // Device does not support Bluetooth
			toast("Device does not support Bluetooth");
		}else if (!mBluetoothAdapter.isEnabled()) {//bluetooth is not enabled
			toast("initializing BT");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}else{
			//everything good, search for devices
			searchManager.execute(null, null, null); 
		}
	}

	/*
	 *  Create a BroadcastReceiver for ACTION_FOUND
	 *  when device is found it is processed if it is in the list
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override		
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(devices.contains(device.getAddress()) ){
					//					Log.w("broadcastReceiver","received");
					short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short)0);
					btPositioningAll.addReading((double)rssi);
					//					Log.w("device",device.getName() + " rssi: "+ rssi + " -- dstnc: "+ String.format("%.6f",distance)+"\n");
					undiscoverDevices();
					if((btPositioningAll.readings.size() % 6) != 0){ //taking 6 readings on each side
						discoverDevices();
					}else{
						//						btPositioning.calculateDistances();//calculating distances & applying mean filter for every 8 readings
						//						appendScreen(btPositioning.getCalculatedDistances() + "\n*************\n");
						toast("Turn 45 degrees to the right");
						goRead = false;
					}

				}
			}
		}
	};


	/*
	 * Manager fo Bluetooth devices search 
	 * puts thread to sleep 3 seconds after reading 6 RSSIs
	 */
	private class SearchingManager extends AsyncTask<Void, Integer, Void> {

		protected Void doInBackground(Void... _void) {
			//			Log.w("ready", "searchingManagerStarted");
			Looper.prepare();
			try {
				btPositioningAll = new BtPositioningAll();
				for(int i=0; i< 8; i++){
					Log.w("run", ""+(i+1));
					Thread.sleep(3000);
					//					btPositioning = new BtPositioning();
					discoverDevices();
					while(goRead){
						Thread.sleep(3000);
						if(goRead && btPositioningAll.readings.size() < 48){//if goRead is true it means the readings.size() is still less than 6 and possibly frozen 
							try{
								undiscoverDevices();								
							}catch(Exception e){}
							discoverDevices();
						}
						int wholeprogress = ((i*100)/8);
						int individualprogress = (((btPositioningAll.readings.size()%6))*10);
						publishProgress(-1, wholeprogress,individualprogress);
					}
					goRead = true;
				}
				btPositioningAll.calculateDistances();//calculating distances & applying mean filter for all the readings
				btPositioningAll.calculateOrientation();
				device.setDistance(btPositioningAll.getCalculatedDistance());
				device.setIOrientation(btPositioningAll.getOrientation());
				device.setFound(true);
				publishProgress(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		protected void onProgressUpdate(Integer... progress){
			if(progress[0] < 0){
				individualProgress.setProgress(progress[2]);
				wholeProgress.setProgress(progress[1]);
				Log.w("progress", "ind:"+ progress[2] + " whole:"+ progress[1]);
			}else if(progress[0] == 1000){				
				wholeProgress.setProgress(100);
				toast(btPositioningAll.getCalculatedDistances() + "\n "
						+ btPositioningAll.getOrientation() +" \n******DONE*******\n");
				beep();
				beep();
				updateGeometriesLocation();
			}else {
				toast("\n RUN -> #"+progress[0]+"\n");
				beep();
			}
		}
	}

	/*
	 * print to screen
	 */
	private void toast(String message){
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}

	/*
	 * start discovery of devices
	 * initialize intent for ACTION_FOUND
	 * register receiver for intent
	 * 
	 */
	private void discoverDevices() {
		//		Log.w("discoverDevices","register");
		mBluetoothAdapter.startDiscovery();
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
	}

	/*
	 * stop discovering devices
	 * unregister receiver for intent ACTION_FOUND
	 * cancelDiscovery on bluetoothAdapter
	 * 
	 */
	private void undiscoverDevices(){
		//		Log.w("broadcastReceiver","UNregister");
		try{
			unregisterReceiver(mReceiver);
		}catch(Exception e){}
		mBluetoothAdapter.cancelDiscovery();
	}

	/*
	 * beep sound 
	 */
	private void beep() {
		try {
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
			r.play();
		} catch (Exception e) {}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 * Intent for enabling Bluetooth
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == REQUEST_ENABLE_BT){
			if(resultCode == RESULT_OK){
				// Register the BroadcastReceiver
				searchManager.execute(null, null, null);
			}else {
				toast("result NOT_OK - request_enable_bt \n");
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}

	private void dialog(String message, String left, String right){
		AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
		builder.setMessage(message);
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
		builder.create();
	}
}
