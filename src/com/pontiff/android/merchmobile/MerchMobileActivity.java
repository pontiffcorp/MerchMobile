package com.pontiff.android.merchmobile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ReplicationCommand;
import org.ektorp.UpdateConflictException;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.couchbase.touchdb.TDDatabase;
import com.couchbase.touchdb.TDServer;
import com.couchbase.touchdb.TDView;
import com.couchbase.touchdb.TDViewMapBlock;
import com.couchbase.touchdb.TDViewMapEmitBlock;
import com.couchbase.touchdb.ektorp.TouchDBHttpClient;
import com.couchbase.touchdb.router.TDURLStreamHandlerFactory;

public class MerchMobileActivity extends Activity implements OnItemClickListener, OnItemLongClickListener, OnKeyListener {


	static final String TAG = "merch-mobile";
	public static final String dDocId = "_design/stores";
	//constants
	
	public static final String DATABASE_NAME = "catalog";
	public static final String dDocName = "stores";
	public static final String viewName = "storelist";
	

	//main screen
	protected EditText addItemEditText;
	protected ListView itemListView;
	protected MerchMobileListAdapter itemListViewAdapter;

	//couch internals
	protected static TDServer server;
	protected static HttpClient httpClient;

	//ektorp impl
	protected CouchDbInstance dbInstance;
	protected CouchDbConnector couchDbConnector;
	protected ReplicationCommand pushReplicationCommand;
	protected ReplicationCommand pullReplicationCommand;

    //static inializer to ensure that touchdb:// URLs are handled properly
    {
        TDURLStreamHandlerFactory.registerSelfIgnoreError();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        //connect items from layout
        addItemEditText = (EditText)findViewById(R.id.addItemEditText);
        itemListView = (ListView)findViewById(R.id.itemListView);

        //connect listeners
		addItemEditText.setOnKeyListener(this);

		startTouchDB();
        startEktorp();

        
        TDDatabase dBase = server.getDatabaseNamed(DATABASE_NAME);
        
        TDView viewAll = dBase.getViewNamed(String.format("%s/%s", "dataBase", "allData"));
	    viewAll.setMapReduceBlocks(new TDViewMapBlock() {

            @Override
            public void map(Map<String, Object> document, TDViewMapEmitBlock emitter) {

            	for (Map.Entry<String, Object> entry : document.entrySet())
            	{
            		emitter.emit(entry.getKey(), entry.getValue());
            	}         	
            	
            }
        }, null, "1.0");

        
    
        ViewQuery queryData = new ViewQuery().designDocId("_design/" + "dataBase").viewName("allData");
	    //ViewResult viewResult = couchDbConnector.queryView(viewQuery);
	    
        List<Row> tickle = couchDbConnector.queryView(queryData).getRows();
	    for (Row result : tickle) {
	    	Log.i("HELLO", result.getValue());  
	    }
		
	    
    
    
    
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

	protected void startTouchDB() {
	    String filesDir = getFilesDir().getAbsolutePath();
	    try {
            server = new TDServer(filesDir);
        } catch (IOException e) {
            Log.e(TAG, "Error starting TDServer", e);
        }

	    //install a view definition needed by the application
	    TDDatabase db = server.getDatabaseNamed(DATABASE_NAME);
	    TDView view = db.getViewNamed(String.format("%s/%s", dDocName, viewName));
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

	protected void startEktorp() {
		Log.v(TAG, "starting ektorp");

		if(httpClient != null) {
			httpClient.shutdown();
		}

		httpClient = new TouchDBHttpClient(server);
		dbInstance = new StdCouchDbInstance(httpClient);

		MerchMobileEktorpAsyncTask startupTask = new MerchMobileEktorpAsyncTask() {

			@Override
			protected void doInBackground() {
				couchDbConnector = dbInstance.createConnector(DATABASE_NAME, true);
			}

			@Override
			protected void onSuccess() {
				//attach list adapter to the list and handle clicks
				ViewQuery viewQuery = new ViewQuery().designDocId(dDocId).viewName(viewName).descending(true);
				itemListViewAdapter = new MerchMobileListAdapter(MerchMobileActivity.this, couchDbConnector, viewQuery);
				itemListView.setAdapter(itemListViewAdapter);
				itemListView.setOnItemClickListener(MerchMobileActivity.this);
				itemListView.setOnItemLongClickListener(MerchMobileActivity.this);

				startReplications();
			}
		};
		startupTask.execute();
	}

	public void startReplications() {

		pushReplicationCommand = new ReplicationCommand.Builder()
			.source(DATABASE_NAME)
			.target("http://10.0.2.2:5984/catalog")
			.continuous(true)
			.build();

		MerchMobileEktorpAsyncTask pushReplication = new MerchMobileEktorpAsyncTask() {

			@Override
			protected void doInBackground() {
				dbInstance.replicate(pushReplicationCommand);
			}
		};

		pushReplication.execute();

		pullReplicationCommand = new ReplicationCommand.Builder()
			.source("http://10.0.2.2:5984/catalog")
			.target(DATABASE_NAME)
			.continuous(true)
			.build();

		MerchMobileEktorpAsyncTask pullReplication = new MerchMobileEktorpAsyncTask() {

			@Override
			protected void doInBackground() {
				dbInstance.replicate(pullReplicationCommand);
			}
		};

		pullReplication.execute();
	}

	public void stopEktorp() {
	}


	/**
	 * Handle typing item text
	 */
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if ((event.getAction() == KeyEvent.ACTION_DOWN)
				&& (keyCode == KeyEvent.KEYCODE_ENTER)) {

			String inputText = addItemEditText.getText().toString();
			if(!inputText.equals("")) {
				createGroceryItem(inputText);
			}
			addItemEditText.setText("");
			return true;
		}
		return false;
	}

	/**
	 * Handle click on item in list
	 */
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Row row = (Row)parent.getItemAtPosition(position);
        JsonNode item = row.getValueAsNode();
		toggleItemChecked(item);
	}

	/**
	 * Handle long-click on item in list
	 */
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Row row = (Row)parent.getItemAtPosition(position);
        final JsonNode item = row.getValueAsNode();
		JsonNode textNode = item.get("text");
		String itemText = "";
		if(textNode != null) {
			itemText = textNode.getTextValue();
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(MerchMobileActivity.this);
		AlertDialog alert = builder.setTitle("Delete Item?")
			   .setMessage("Are you sure you want to delete \"" + itemText + "\"?")
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   deleteGroceryItem(item);
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // Handle Cancel
		           }
		       })
		       .create();

		alert.show();

		return true;
	}

	
	/**
	 * Add settings item to the menu
	 */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, 0, "Settings");
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Launch the settings activity
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
         
                return true;
        }
        return false;
    }

    public void createGroceryItem(String name) {
        final JsonNode item = MerchMobileItemUtils.createWithText(name);
        MerchMobileEktorpAsyncTask createItemTask = new MerchMobileEktorpAsyncTask() {

			@Override
			protected void doInBackground() {
				couchDbConnector.create(item);
			}

			@Override
			protected void onSuccess() {
				Log.d(TAG, "Document created successfully");
			}

			@Override
			protected void onUpdateConflict(
					UpdateConflictException updateConflictException) {
				Log.d(TAG, "Got an update conflict for: " + item.toString());
			}
		};
	    createItemTask.execute();
    }

    public void toggleItemChecked(final JsonNode item) {
        MerchMobileItemUtils.toggleCheck(item);

        MerchMobileEktorpAsyncTask updateTask = new MerchMobileEktorpAsyncTask() {

			@Override
			protected void doInBackground() {
				couchDbConnector.update(item);
			}

			@Override
			protected void onSuccess() {
				Log.d(TAG, "Document updated successfully");
			}

			@Override
			protected void onUpdateConflict(
					UpdateConflictException updateConflictException) {
				Log.d(TAG, "Got an update conflict for: " + item.toString());
			}
		};
	    updateTask.execute();
    }

    public void deleteGroceryItem(final JsonNode item) {
        MerchMobileEktorpAsyncTask deleteTask = new MerchMobileEktorpAsyncTask() {

			@Override
			protected void doInBackground() {
				couchDbConnector.delete(item);
			}

			@Override
			protected void onSuccess() {
				Log.d(TAG, "Document deleted successfully");
			}

			@Override
			protected void onUpdateConflict(
					UpdateConflictException updateConflictException) {
				Log.d(TAG, "Got an update conflict for: " + item.toString());
			}
		};
	    deleteTask.execute();
    }

}