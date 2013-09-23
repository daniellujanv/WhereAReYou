package itu.pervasive.whereareyou;

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

import com.metaio.sdk.MetaioDebug;
import com.metaio.tools.io.AssetsManager;

public class MainActivity extends Activity {

	/**
	 * Task that will extract all the assets
	 */
//	AssetsExtracter mTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// extract all the assets
		try {
			AssetsManager.extractAllAssets(this, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		mTask = new AssetsExtracter();
//		mTask.execute(0);
		loadFriends();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private HashMap<String,String> newName(String key, String name){
		HashMap<String, String> test = new HashMap<String, String>();
		test.put(key, name);
		return test;
	}
	
	/*
	 * 
	 * loadFriends fills the initial list with the available friends
	 * 
	 */
	private void loadFriends(){

		final ListView listview = (ListView) findViewById(R.id.listView1);
		List<Map<String,String>> list_names = new ArrayList<Map<String,String>>();

		list_names.add(newName("friend","Friend 1"));
		list_names.add(newName("friend","Friend 2"));

		SimpleAdapter simpleAdpt = new SimpleAdapter(this, list_names, android.R.layout.simple_list_item_1
				, new String[] {"friend"}, new int[] {android.R.id.text1});
		listview.setAdapter(simpleAdpt);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {

				TextView clickedView = (TextView) view;

				Toast.makeText(MainActivity.this, "Item with id ["+id+"] - Position ["+position+"] - Planet ["+clickedView.getText()+"]", Toast.LENGTH_SHORT).show();
				final Class<?> activity;
				//					activity = Class.forName(getPackageName()+".ARActivity");
				//					Intent i  = new Intent(getApplicationContext(), activity);
				Intent i  = new Intent(getApplicationContext(), ARActivity.class);
				i.addCategory(Intent.CATEGORY_BROWSABLE);
				startActivity(i);

			}
		});
	}
}
