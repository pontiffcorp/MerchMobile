package com.pontiff.android.merchmobile;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;


public class ButtonClickListener implements View.OnClickListener {

	int screenId;
	HorizontalPager mPager;
	HorizontalScrollView scrollView;
	LinearLayout layout;
	Context ctx;
	int screenWidth;
	
	
	public ButtonClickListener (int screenId, int screenWidth, HorizontalPager mPager, HorizontalScrollView scrollView, LinearLayout layout, Context ctx) {
		this.screenId = screenId;
		this.screenWidth = screenWidth;
		this.mPager = mPager;
		this.scrollView = scrollView;
		this.layout = layout;
		this.ctx = ctx;
	
	}
	   
	@Override
	public void onClick(View v) {
		
    LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(ctx.LAYOUT_INFLATER_SERVICE);        
    
    
		mPager.setCurrentScreen(screenId, true);

		for(int i=0; i<((ViewGroup)layout).getChildCount(); ++i) 
		    			((ViewGroup)layout).getChildAt(i).setBackgroundColor(Color.LTGRAY);
		
		v.setBackgroundColor(Color.YELLOW);
		int scrollTo = v.getLeft() + (v.getWidth() - screenWidth) / 2;
		
		scrollView.scrollTo(scrollTo, 0);
		
		TableLayout table = (TableLayout) mPager
				.getChildAt(screenId)
				.findViewById(R.id.page_view) 
				.findViewById(R.id.table_view);	
		
        for (int i = 0; i < 6; i++) {

        	table.addView(inflater.inflate(R.layout.table_row,  null));
        }
        

		
	
	}

}
