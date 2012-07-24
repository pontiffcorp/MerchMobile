package com.pontiff.android.merchmobile;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;
import org.ektorp.android.util.CouchbaseViewListAdapter;

import com.pontiff.android.merchmobile.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;


public class MerchMobileGalleryAdapter extends BaseAdapter{

	        
        private Context context; // needed to create the view
        private List<String> dataSet;
        public MerchMobileGalleryAdapter(Context context, List<String> dataSet) {
            this.context = context;
            this.dataSet = dataSet;
           
        }
 
        //set to three for this example
        //for normal use this should be set to the length of the data structure that contains
        //the items to be displayed
        
        public int getCount() {
            return dataSet.size();
        }
 
        public Object getItem(int position) {
            return position;
        }
 
        public long getItemId(int position) {
            return position;
        }
 
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            //just a simple optimiztaion - 
            //we only inflate a new layout if we don't have one to reuse
            if(convertView == null)
                v = LayoutInflater.from(context).inflate(R.layout.gallery_item, parent, false);
            else
                v = convertView;
            
             
            //Now that we have our new custom View, find out widgets in the view and set them
            
            
            TextView tv = (TextView) v.findViewById(R.id.textView);
            tv.setText(dataSet.get(position));
            		
            CheckBox cb = (CheckBox) v.findViewById(R.id.checkBox);
                        
                        
            
            //this just alternates what the checkbox states are
            if(position % 2 == 0)
                cb.setChecked(true);
            else
                cb.setChecked(false);
            
            return v;
        }
    
     
 
	
	
	
}
