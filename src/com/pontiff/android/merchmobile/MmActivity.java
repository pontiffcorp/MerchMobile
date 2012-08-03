package com.pontiff.android.merchmobile;


import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ReplicationCommand;
import org.ektorp.http.HttpClient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.couchbase.touchdb.TDServer;
import com.couchbase.touchdb.router.TDURLStreamHandlerFactory;

public class MmActivity extends Activity implements
		OnItemClickListener, OnItemLongClickListener, OnKeyListener {

	
	{
		TDURLStreamHandlerFactory.registerSelfIgnoreError();
	}	
		
	public LinearLayout ll;
	public HorizontalPager mPager;
	public HorizontalScrollView hs;

	protected EditText addItemEditText;
	protected ListView itemListView;
	protected MerchMobileListAdapter itemListViewAdapter;
	
	final static String TAG = "merch-mobile";
	public static final String dDocId = "_design/stores";

	// constants
	public static final String DATABASE_NAME = "catalog";
	public static final String dDocName = "stores";
	public static final String viewName = "storelist";
	
	// couch internals
	protected static TDServer server;
	protected static HttpClient httpClient;

	// ektorp impl
	protected static CouchDbInstance dbInstance;
	protected static CouchDbConnector couchDbConnector;
	protected static ReplicationCommand pushReplicationCommand;
	protected static ReplicationCommand pullReplicationCommand;
   

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MmUtils.setActivityScreenLayout(this);
		MmUtils.startTouchDB(this);
		MmUtils.startEktorp();
		
	}

	protected void onDestroy() {
		Log.v(TAG, "onDestroy");

		//need to stop the async task thats following the changes feed
		itemListViewAdapter.cancelContinuous();

		//clean up our http client connection manager
		if(httpClient != null) {
			httpClient.shutdown();
		}

		if(server != null) {
		    server.close();
		}

		super.onDestroy();
	}
	
	
	
	@Override
	public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub

	}
}
