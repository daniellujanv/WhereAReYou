package itu.pervasive.whereareyou;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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

	/**
	 * Offset from current location
	 */
	private static final double OFFSET = 0.00002;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

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
			//				String filepath = AssetsManager.getAssetPath("Media/POI_bg.png");
			//				if (filepath != null) 
			//				{					
			////					friends.add(metaioSDK.loadImageBillboard(createBillboardTexture("friend1")));
			friends.add(metaioSDK.createGeometryFromImage(createBillboardTexture("David")));
			friends.get(0).setScale(new Vector3d(15f,15f,15f));
			friends.get(0).setRotation(new Rotation((float)(Math.PI/2),0f,(float)Math.PI),true);
			friends.get(0).setTranslation(new Vector3d(-1000f,-25000f, 1000f),false);
			friends.get(0).setRenderOrder(1);
			////					Log.w("rotation", friends.get(1).getRotation().getEulerAngleDegrees().toString());
			////					friends.get(1).setRotation(new Rotation(0f,0f,91.5f),true);
			//					//rotate in X
			////					friends.get(1).setRotation(new Rotation(new float[]{1f,0f,0f/**/,0f,-0.4480f,-0.8939f/**/,0f,-0.8939f,-0.4480f}));
			////					Log.w("rotation", friends.get(1).getRotation().getEulerAngleDegrees().toString());
			//				}
			String filepath = AssetsManager.getAssetPath("Media/metaioman.md2");
			if (filepath != null) 
			{
				friends.add(metaioSDK.createGeometry(filepath));
				//						friends.get(2).startAnimation("idle", true);
				//						friends.get(2).setScale(new Vector3d(50f,50f,50f));
				friends.get(1).startAnimation("idle", true);
				friends.get(1).setScale(new Vector3d( 80f, 80f, 80f));
				//19 mts away
				friends.get(1).setTranslation(new Vector3d(0f,-25000f, -8000f),false);
				friends.get(1).setRotation(new Rotation( 0f, 0f, (float)Math.PI)); //radians, PI == half turn
				friends.get(1).setRenderOrder(0);
			}
//			updateGeometriesLocation(mSensors.getLocation());
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
				final int maxWidth = 200;
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

	private void updateGeometriesLocation(LLACoordinate location)
	{
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
		//			if (friends.get(1) != null)
		//			{
		//				//sign
		//				friends.get(1).setTranslation(new Vector3d(0f,-9999f,-5500f),false);
		//				Log.w("LocationChange","1 - "+friends.get(1).getTranslation().toString());
		////				friends.get(1).setRotation(new Rotation(0f,0f,180f),true);
		////				friends.get(1).setRotation(new Rotation(180f,0f,180f),true);
		////				Log.w("rotation", friends.get(1).getRotation().getEulerAngleDegrees().toString());
		//
		//			}
		//			if(friends.get(2) != null){
		//				//guy
		//				friends.get(2).setTranslation(new Vector3d(0f,-10000f,-5500f),false);
		//				Log.w("LocationChange","2 - "+friends.get(2).getTranslation().toString());
		//			}

		//			if (//mGeometryWest != null)
		//			{
		//				location.setLongitude(location.getLongitude()-OFFSET); //draw to west
		//				MetaioDebug.log("geometryWest.setTranslationLLA: "+location);
		//				Log.w("LocationChange","guy - guy.setTranslationLLA: "+location);
		//				//mGeometryWest.setTranslationLLA(location);
		//				location.setLongitude(location.getLongitude()+OFFSET);
		//			}
		if(friends.get(0) != null){
			//guy - north
			friends.get(0).setTranslation(new Vector3d(0f,-10000f, 0f),false);
			Log.w("LocationChange","0 - "+friends.get(0).getTranslation().toString());
		}
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
		TextView text = (TextView)findViewById(R.id.sensorInfo);
		float orientation_0 = (float) Math.floor(orientation[0]);
		float orientation_1 = (float) Math.floor(orientation[1]);
		float orientation_2 = (float) Math.floor(orientation[2]);
		text.setText("[0x]" + orientation_0  + "[1y]" + orientation_1 + "[2z]" + orientation_2);
//		try{
//			friends.get(0).setTranslation(new Vector3d(/*orientation[0]-previous_orientation[0]*/ 0 ,orientation_1-previous_orientation[1],orientation_2-previous_orientation[2]), true);
//			Log.w("LocationChange","0 - "+friends.get(0).getTranslation().toString());
//		}catch(IndexOutOfBoundsException e){}

		previous_orientation[0] = orientation_0;
		previous_orientation[1] = orientation_1;
		previous_orientation[2] = orientation_2;
	}

	@Override
	public void onLocationSensorChanged(LLACoordinate location)
	{
		MetaioDebug.log("Location changed: "+location);
		Log.w("LocationChange", "sensor changed - Location changed: "+location);
//		updateGeometriesLocation(location);
	}


}
