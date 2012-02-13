package com.yeung.mallguide;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import com.yeung.mallguide.R;
import com.yeung.mallguide.graph.Graph;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources.NotFoundException;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class MallguideActivity extends Activity {
	private Graph g;
	MapView map;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        map = (MapView)this.findViewById(R.id.mall);
        g = new Graph();
        try{
        	g.loadGraphFromXML(this.getResources().getXml(R.xml.mall1));
        }
        catch (NotFoundException e) {
			longToast("Error: resource not found:\n\n" + e);
		} catch (XmlPullParserException e) {
			longToast("Error: xml error:\n\n" + e);
		} catch (IOException e) {
			longToast("Error: io error:\n\n" + e);
		}
        
        map.loadMap(g);
        
    }
    
    
    
    @Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
		Log.i("restore", "yes");
		
	}



	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		Log.i("restore", "yes");
		
	}


	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.i("onPause", "yes");
		Editor e = getSharedPreferences("View", 0).edit();
		map.save(e);
	}



	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.i("onresume", "yes");
		//SharedPreferences s = getSharedPreferences("View", 0);
		//map.resume(s);
	}



	@Override
    public void onAttachedToWindow(){
    	super.onAttachedToWindow();
    	getWindow().setFormat(PixelFormat.RGBA_8888);
    }
    
    private void longToast(String s) {
		Toast.makeText(this, s, Toast.LENGTH_LONG).show();
	}
}