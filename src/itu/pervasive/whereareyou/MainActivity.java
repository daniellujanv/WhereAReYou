package itu.pervasive.whereareyou;
//prototype v2.0
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.metaio.tools.io.AssetsManager;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// extract all the assets
		try {
			AssetsManager.extractAllAssets(this, true);
			loadFriends();
//			startMetaioIntent();

		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		startMetaioIntent();
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/*
	 * 
	 * loadFriends fills the initial list with the available friends
	 * 
	 */
	private void loadFriends(){

		final ListView listview = (ListView) findViewById(R.id.listView1);
		List<Map<String,String>> list_names = new ArrayList<Map<String,String>>();

		list_names.add(newName("friend","David"));
		list_names.add(newName("friend","Find All!"));

		SimpleAdapter simpleAdpt = new SimpleAdapter(this, list_names, android.R.layout.simple_list_item_1
				, new String[] {"friend"}, new int[] {android.R.id.text1});
		listview.setAdapter(simpleAdpt);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
//				TextView clickedView = (TextView) view;
//				Toast.makeText(MainActivity.this, "Item with id ["+id+"] - Position ["+position+"] - Planet ["+clickedView.getText()+"]", Toast.LENGTH_SHORT).show();
				//					activity = Class.forName(getPackageName()+".ARActivity");
				//					Intent i  = new Intent(getApplicationContext(), activity);
				startMetaioIntent();
			}
		});
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
		Intent i  = new Intent(getApplicationContext(), ARActivity.class);
		i.addCategory(Intent.CATEGORY_BROWSABLE);
		startActivity(i);
	}
}
