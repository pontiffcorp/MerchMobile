package com.pontiff.android.merchmobile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ektorp.ReplicationCommand;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;
import org.ektorp.impl.StdCouchDbInstance;

import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.couchbase.touchdb.TDDatabase;
import com.couchbase.touchdb.TDServer;
import com.couchbase.touchdb.TDView;
import com.couchbase.touchdb.TDViewMapBlock;
import com.couchbase.touchdb.TDViewMapEmitBlock;
import com.couchbase.touchdb.ektorp.TouchDBHttpClient;

public class MmUtils {
	
	
	// static inializer to ensure that touchdb:// URLs are handled properly	
	
	static void setActivityScreenLayout(MmActivity mm){
		
		mm.setContentView(R.layout.master_layout);
		mm.ll = (LinearLayout) mm.findViewById(R.id.menu_group);
		mm.hs = (HorizontalScrollView) mm.findViewById(R.id.hsScroll_menu);

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);

		mm.mPager = (HorizontalPager) mm.findViewById(R.id.horizontal_pager);

		DisplayMetrics metrics = new DisplayMetrics();
		mm.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int screenWidth = metrics.widthPixels;

		LayoutInflater inflater = (LayoutInflater) mm.getSystemService(mm.LAYOUT_INFLATER_SERVICE);

		for (int id = 0; id < 8; id++) {

			View page = inflater.inflate(R.layout.pager_layout, null);

			mm.mPager.addView(page);

			Button myButton = new Button(mm.getApplicationContext());
			myButton.setText("Push Me");
			myButton.setId(id);
			myButton.setBackgroundColor(Color.LTGRAY);
			// myButton.setId(id);
			myButton.setOnClickListener(new ButtonClickListener(id,
					screenWidth, mm.mPager, mm.hs, mm.ll, mm.getApplicationContext()));
			mm.ll.addView(myButton, lp);
		}


		
	} 
	
	protected static void startTouchDB(MmActivity mm) {
	    String filesDir = mm.getFilesDir().getAbsolutePath();
	    try {
            MmActivity.server = new TDServer(filesDir);
        } catch (IOException e) {
            Log.e(MmActivity.TAG, "Error starting TDServer", e);
        }

	    //install a view definition needed by the application
	    TDDatabase db = MmActivity.server.getDatabaseNamed(MmActivity.DATABASE_NAME);
	    TDView view = db.getViewNamed(String.format("%s/%s", MmActivity.dDocName, MmActivity.viewName));
	    view.setMapReduceBlocks(new TDViewMapBlock() {

            @Override
            public void map(Map<String, Object> document, TDViewMapEmitBlock emitter) {
                Object storeName = document.get("store_name");
                if(storeName != null) {
                    emitter.emit(storeName.toString(), document);
                }

            }
        }, null, "1.0");

	}
	
	protected static void startEktorp() {
		Log.v(MmActivity.TAG, "starting ektorp");
		
		
		if(MmActivity.httpClient != null) {
		   MmActivity.httpClient.shutdown();
		}

		MmActivity.httpClient = new TouchDBHttpClient(MmActivity.server);
		MmActivity.dbInstance = new StdCouchDbInstance(MmActivity.httpClient);

		MerchMobileEktorpAsyncTask startupTask = new MerchMobileEktorpAsyncTask() {

			@Override
			protected void doInBackground() {
				
				MmActivity.couchDbConnector = MmActivity.dbInstance.createConnector(MmActivity.DATABASE_NAME, true);
			}

			@Override
			protected void onSuccess() {
				//attach list adapter to the list and handle clicks
				ViewQuery viewQuery = new ViewQuery().designDocId(MmActivity.dDocId).viewName(MmActivity.viewName).descending(true);


				ViewResult result = MmActivity.couchDbConnector.queryView(viewQuery);
//		        Gallery gallery = (Gallery) findViewById(R.id.gallery);

		        List<String> rowVals = new ArrayList<String>();
		        String item;

		        for (Row row : result.getRows()) {

		        	item = row.getValueAsNode().get("store_name").getTextValue();
		        	if (item != null) rowVals.add(item);

		        }


	//			gallery.setAdapter(new MerchMobileGalleryAdapter(ctx, rowVals));




				//				itemListViewAdapter = new MerchMobileListAdapter(MmActivity.this, couchDbConnector, viewQuery);
//				itemListView.setAdapter(itemListViewAdapter);
//				itemListView.setOnItemClickListener(MmActivity.this);
//				itemListView.setOnItemLongClickListener(MmActivity.this);



				startReplications();
			}
		};
		startupTask.execute();
	}
	
	public static void startReplications() {

			MmActivity.pushReplicationCommand = new ReplicationCommand.Builder()
			.source(MmActivity.DATABASE_NAME)
			.target("http://10.0.2.2:5984/catalog")
			.continuous(true)
			.build();

		MerchMobileEktorpAsyncTask pushReplication = new MerchMobileEktorpAsyncTask() {

			@Override
			protected void doInBackground() {
				MmActivity.dbInstance.replicate(MmActivity.pushReplicationCommand);
			}
		};

		pushReplication.execute();

		MmActivity.pullReplicationCommand = new ReplicationCommand.Builder()
			.source("http://10.0.2.2:5984/catalog")
			.target(MmActivity.DATABASE_NAME)
			.continuous(true)
			.build();

		MerchMobileEktorpAsyncTask pullReplication = new MerchMobileEktorpAsyncTask() {

			@Override
			protected void doInBackground() {
				MmActivity.dbInstance.replicate(MmActivity.pullReplicationCommand);
			}
		};

		pullReplication.execute();
	}
	
}
