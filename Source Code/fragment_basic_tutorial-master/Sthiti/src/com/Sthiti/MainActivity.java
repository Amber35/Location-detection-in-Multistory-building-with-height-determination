package com.Sthiti;





import android.app.Activity;
import android.os.Bundle;

import com.Sthiti_tracking.GPStracker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity {
GPStracker mGPs;
double lat;
double lng;
private GoogleMap googleMap;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	mGPs=new GPStracker(MainActivity.this);
		try {
			if(googleMap==null)
			{
				googleMap=((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
				
			}
			if(mGPs.canGetLocation)
			{
				 mGPs.getLocation();
		           lat =  mGPs.getLatitude();
		          lng = mGPs.getLongitude();
		            }else{
		              
		                mGPs.showSettingsAlert();
		            }
			 Marker marker = googleMap.addMarker(new MarkerOptions()
	         .position(new LatLng(lat, lng))
	         .title("M  here")
	         .snippet("Current location"));

	         marker.showInfoWindow();

	        
	        	    CameraUpdate zoom=CameraUpdateFactory.zoomTo(12);

	        	  googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 13));
	        	  googleMap.animateCamera(zoom);	
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
}
