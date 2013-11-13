package itu.pervasive.whereareyou;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.os.Vibrator;
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
	final private float AWAY_FROM_SCREEN = 15000f; 
	private IRadar mRadar;
	private IGeometry cross;
	private String[] device_to_search = new String[2];
	private BluetoothAdapter mBluetoothAdapter;
	private SearchingManager searchManager;
	private int REQUEST_ENABLE_BT = 1;
	private BtPositioning btPositioning;
	private boolean goRead = true; 
	private ProgressBar individualProgress;
	private List<String> devices;
	private Device device;
	private long timeNow;
	private long timeLastReading;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		individualProgress = (ProgressBar) this.mGUIView.findViewById(R.id.individualprogressbar);
		devices = new ArrayList<String>();
		btPositioning = new BtPositioning();
		timeLastReading = System.currentTimeMillis();
		Intent intent = getIntent();
		device_to_search = intent.getStringArrayExtra("device");
		devices.add(device_to_search[1]);
		device = new Device(device_to_search[0],device_to_search[1]);
		setTitle("Looking for "+ device.name);
		//TO-DO if users wants to search all devices add them to the list
		//initialize friend list
		friends = new ArrayList<IGeometry>();
		// Set GPS tracking configuration
		// The GPS tracking configuration must be set on user-interface thread
		boolean result = metaioSDK.setTrackingConfiguration("GPS");  
		initializeBluetooth();
		//		MetaioDebug.log("Tracking data loaded: " + result);  
		//		Log.w("Tracking data loaded", (result == true)? "true":"false");   
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
			String filepath_cross = AssetsManager.getAssetPath("Media/cross.png");
			cross = metaioSDK.createGeometryFromImage(filepath_cross);
			cross.setScale(new Vector3d( 80f, 80f, 80f));
			cross.setName("cross");

			String filepath = AssetsManager.getAssetPath("Media/metaioman.md2");
			friends.add(metaioSDK.createGeometry(filepath));
			friends.get(0).startAnimation("idle", true);
			friends.get(0).setScale(new Vector3d( 0f, 0f, 0f));
			//						friends.get(2).startAnimation("idle", true);
			//						friends.get(2).setScale(new Vector3d(50f,50f,50f));
			// create radar
			mRadar = metaioSDK.createRadar();
			mRadar.setBackgroundTexture(AssetsManager.getAssetPath("Media/radar.png"));
			mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath("Media/yellow.png"));
			mRadar.setObjectTexture(cross, AssetsManager.getAssetPath("Media/red.png"));

			mRadar.setRelativeToScreen(IGeometry.ANCHOR_BC);
			// add geometries to the radar
			//			mRadar.add(friends.get(0));
			//				mRadar.add(friends.get(2));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		//		updateGeometriesLocation();
		changeUserOrientation(0);
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
			String filepath = AssetsManager.getAssetPath("Media/POI_bg@2x.png");
			Bitmap mBackgroundImage = BitmapFactory.decodeFile(filepath);
			billboard = mBackgroundImage.copy(Bitmap.Config.ARGB_8888, true);
			//			billboard = Bitmap.createScaledBitmap(mBackgroundImage.copy(Bitmap.Config.ARGB_8888, true),
			//					600, 200,false);
			Canvas c = new Canvas(billboard);
			mPaint.setColor(Color.WHITE);
			mPaint.setTextSize(18);
			mPaint.setTypeface(Typeface.DEFAULT);
			float y = 25;
			float x = 15;
			//			c.drawText(billBoardTitle, x, y, mPaint);
			String[] title=  billBoardTitle.split("\n");
			int i=0;
			// Draw POI name
			while (i < title.length)
			{
				//				String n = billBoardTitle.trim();

				//				final int maxWidth = 40;
				//				int i = mPaint.breakText(n, true, maxWidth, null);
				c.drawText(title[i], x, y, mPaint);
				// Draw second line if valid
				//				if (i < n.length())
				//				{
				//					n = n.substring(i);
				y += 20;
				i = i + 1;
				//					i = mPaint.breakText(n, true, maxWidth, null);
				//
				//					if (i < n.length())
				//					{
				//						i = mPaint.breakText(n, true, maxWidth - 20, null);
				//						c.drawText(n.substring(0, i) + "...", x, y, mPaint);
				//					} else
				//					{
				//						c.drawText(n.substring(0, i), x, y, mPaint);
				//					}
				//				}
			}
			// writing file
			try
			{
				FileOutputStream out = new FileOutputStream(texturepath);
				billboard.compress(Bitmap.CompressFormat.PNG, 90, out);
				//				MetaioDebug.log("Texture file is saved to "+texturepath);
				//				Log.w("ImageCreated","Texture file is saved to "+texturepath);
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
			//25 mts away
			/* x =  1 -> east
			 * x = -1 -> west
			 * y =  1 -> north
			 * y = -1 -> south
			 */
			mRadar.remove(cross);
			cross.delete();
			friends.get(0).setRotation(new Rotation( 0f, 0f, getRotation(device.orientation[0],device.orientation[1])),true); //radians, PI == half turn
			friends.get(0).setScale(new Vector3d( 80f, 80f, 80f));
			//			friends.get(0).setTranslation(new Vector3d(AWAY_FROM_SCREEN,AWAY_FROM_SCREEN, -7250f),false);
			friends.get(0).setTranslation(new Vector3d((device.orientation[0])*AWAY_FROM_SCREEN,(device.orientation[1])*AWAY_FROM_SCREEN, -7250f),false);
			friends.get(0).setRenderOrder(0);
			String deviceInfo = device.name+" \n "+ device.distance +" meters \n Orientation: "+device.s_orientation;
			if(!device.accurate){
				deviceInfo += "\n *Orientation not Accurate* ";
			}
			//billboard
			friends.add(metaioSDK.createGeometryFromImage(createBillboardTexture(deviceInfo)));
			friends.get(1).setScale(new Vector3d(26f,30f,26f));
			friends.get(1).setRotation(new Rotation((float)(Math.PI/2),0f,getRotation(device.orientation[0],device.orientation[1])),true);
			//			friends.get(1).setTranslation(new Vector3d(AWAY_FROM_SCREEN,AWAY_FROM_SCREEN, 775f),false);
			friends.get(1).setTranslation(new Vector3d((device.orientation[0])*(AWAY_FROM_SCREEN-1000),(device.orientation[1])*(AWAY_FROM_SCREEN-1000), 2000f),false);
			friends.get(1).setRenderOrder(1);

			mRadar.add(friends.get(0));
		}
	}

	@Override
	protected void onGeometryTouched(final IGeometry geometry) 
	{
		String name = geometry.getName();
		if(name.equals("cross")){
			geometry.setTranslation(new Vector3d(0f,0f,0f));
			mRadar.remove(geometry);
			Log.w("onGeometryTouched", "geometry "+ name);
			int run = btPositioning.readings.size()/6;
			Log.w("geometryTouched","run "+run);
			toast("Searching for device!\n" +
					"Point to and click the next cross when it appears \n" +
					(7-run)+" steps to go",true, false);
			searchManager = new SearchingManager(); 
			searchManager.execute(null, null, null);
		}
		//		MetaioDebug.log("Geometry selected: "+geometry);
		//		Log.w("geometryTouched",geometry.getName());
		//		mSurfaceView.queueEvent(new Runnable()
		//		{
		//			@Override
		//			public void run() 
		//			{
		//				mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath("Media/yellow.png"));
		//				mRadar.setObjectTexture(geometry, AssetsManager.getAssetPath("Media/red.png"));
		//			}
		//		});
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
		//		MetaioDebug.log("Location changed: "+location);
		//		Log.w("LocationChange", "sensor changed - Location changed: "+location);
		//		updateGeometriesLocation(location);
	}

	/**
	 * Methods for bluetooth search
	 */

	protected float getRotation(int x, int y){
		/*  0, 1 => (0,0,0) -
		 *  1, 1 => (0,0,-pi/4) 
		 *  1, 0 => (0,0,-pi/2) -
		 *  1,-1 => (0,0,-3pi/4) -
		 *  0,-1 => (0,0, pi) -
		 * -1,-1 => (0,0, 3pi/4) -
		 * -1, 0 => (0,0, pi/2) -
		 * -1, 1 => (0,0, pi/4) -
		 */
		if(x == 0){
			if(y == 1){
				return 0; //radians, PI == half turn
			}else if(y == -1){
				return (float)Math.PI; //radians, PI == half turn
			}
		}else if(x == 1){
			if(y == 0){
				return ((-(float)Math.PI)/2); //radians, PI == half turn
			}else if(y == 1){
				return ((-(float)Math.PI)/4); //radians, PI == half turn
			}else if(y == -1){
				return ((-(float)Math.PI*3)/4); //radians, PI == half turn
			}
		}else if(x == -1){
			if(y == 0){
				return (((float)Math.PI)/2); //radians, PI == half turn
			}else if(y == 1){
				return (((float)Math.PI)/4); //radians, PI == half turn
			}else if(y == -1){
				return (((float)Math.PI*3)/4); //radians, PI == half turn
			}

		}
		return 0;
	}

	private void changeUserOrientation(int orientation){
		if(orientation == 0){
			toast("Search for the red cross and click it!",true, true);			
		}else{
			toast("Search for the red cross and click it!",true, false);
		}
		int x;
		int y;
		switch(orientation){
		case 0:
			x = 0;
			y = 1;
			break;
		case 1:
			x = 1;
			y = 1;
			break;
		case 2: 
			x = 1;
			y = 0;
			break;
		case 3: 
			x = 1;
			y = -1;
			break;
		case 4: 
			x = 0;
			y = -1;
			break;
		case 5:
			x = -1;
			y = -1;
			break;
		case 6: 
			x = -1;
			y = 0;
			break;
		case 7: 
			x = -1;
			y = 1;
			break;
		default: 
			x = 0;
			y = 0;
			break;
		}
		Log.w("changeUserOrientation","direction "+orientation+" x:"+x+" y:"+y+ " rotation: "+(getRotation(x,y)*-1));
		cross.setTranslation(new Vector3d((x)*AWAY_FROM_SCREEN,(y)*AWAY_FROM_SCREEN, -15000f),false);
		cross.setRotation(new Rotation( 0f, 0f, getRotation(x,y)*-1),false); //radians, PI == half turn

		mRadar.add(cross);
		mRadar.setObjectTexture(cross, AssetsManager.getAssetPath("Media/red.png"));
	}

	/*
	 * Start the search for devices
	 */
	private void initializeBluetooth(){
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) { // Device does not support Bluetooth
			toast("Device does not support Bluetooth",true,false);
		}else if (!mBluetoothAdapter.isEnabled()) {//bluetooth is not enabled
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
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
				//				Log.w("broadcastReceiver","received " + device.getName());
				if(devices.contains(device.getAddress()) ){
					//					Log.w("broadcastReceiver","received");
					timeLastReading = System.currentTimeMillis();
					short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short)0);
					btPositioning.addReading((double)rssi);
					//					Log.w("device",device.getName() + " rssi: "+ rssi + " -- dstnc:");
					undiscoverDevices();
					if((btPositioning.readings.size() % 6) != 0 && btPositioning.readings.size() != 60 ){ //taking 6 readings on each side
						discoverDevices();
						goRead = true;
					}else{
						timeLastReading = System.currentTimeMillis();
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
	private class SearchingManager extends AsyncTask<Void, Integer, Integer> {

		protected Integer doInBackground(Void... runNumber) {
			//			Log.w("searchManager", "searchingManagerStarted");
			int i = btPositioning.readings.size()/6; //0<= i <= 7
			try {
				goRead = true;
				Log.w("searchManager", "run "+(i+1));
				discoverDevices();
				while(goRead){
					Thread.sleep(4000); //timer instead of thread sleep
					timeNow = System.currentTimeMillis();
					Log.w("searchManager", " listSize:"+btPositioning.readings.size()+ " time:"+ (timeNow-timeLastReading));
//					 
					if(goRead && (timeNow - timeLastReading)  > 4000){//if goRead is true it means the readings.size() is still less than 6 and possibly frozen 
						try{
							Log.w("searchManager","more than 5 seconds and still need readings");
							undiscoverDevices();								
						}catch(Exception e){}
						discoverDevices();
					}
					int individualprogress = (int)((((double)btPositioning.readings.size()%6)/6)*100);
					publishProgress(-1, individualprogress);
				}
				publishProgress(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return i;
		}

		protected void onProgressUpdate(Integer... progress){
			if(progress[0] == -1){
				individualProgress.setProgress(progress[1]);
				//				Log.w("progress", "ind:"+ progress[1]);
			}else 
				if(progress[0] == 1000){				
					buzz(1);
				}
		}

		protected void onPostExecute(final Integer result){
			//			Log.w("postexec", "post execute!!!!");
			if(result < 7){
				mSurfaceView.queueEvent(new Runnable(){
					@Override
					public void run() {
						// TODO Auto-generated method stub
						changeUserOrientation((int)result + 1);
						Log.w("onPostExecute", "call changeUserOrientation("+((int)result + 1)+")");
					}
				});
			}else{
				btPositioning.calculateDistances();//calculating distances & applying mean filter for all the readings
				btPositioning.calculateOrientation();
				device.setDistance(btPositioning.getCalculatedDistance());
				device.setIOrientation(btPositioning.getOrientation());
				device.setSOrientation(btPositioning.getSOrientation());
				device.setFound(true);
				mSurfaceView.queueEvent(new Runnable(){
					@Override
					public void run() {
						// TODO Auto-generated method stub
						buzz(3);
						updateGeometriesLocation();
						Log.w("onPostExecute", "finished 8 readings");
						toast(btPositioning.getCalculatedDistances() + "\n "
								+ btPositioning.getOrientation() +" \n******DONE*******\n",true, false);

					}
				});
			}
		}
	}

	/*
	 * print to screen
	 */
	private void toast(String message, boolean time_long, boolean looper){
		if(looper){
			Looper.prepare();
		}
		if(time_long){
			//			Toast toast = new Toast(getApplicationContext());
			//			toast.setText(message);
			//			toast.setDuration(10);
			//			toast.show();
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
		}else{
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
		}
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
		timeNow = System.currentTimeMillis();
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
	 * vibrate 
	 */
	private void buzz(final int seconds) {
		mSurfaceView.queueEvent(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					v.vibrate(seconds*1000);
				} catch (Exception e) {}
			}

		});
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
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
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
		Log.w("destroy","destroooy");
		try{
			unregisterReceiver(mReceiver);
			mBluetoothAdapter.cancelDiscovery();
			mBluetoothAdapter.disable();
			searchManager.cancel(true);
		}catch(Exception e){
			Log.w("onDestroy", e.toString());
		}
	}
}
