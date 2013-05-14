/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.majorkernelpanic.spydroid.ui;

import java.util.Locale;

import net.majorkernelpanic.http.TinyHttpServer;
import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import net.majorkernelpanic.spydroid.Utilities;
import net.majorkernelpanic.spydroid.api.CustomHttpServer;
import net.majorkernelpanic.spydroid.api.CustomRtspServer;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HandsetFragment extends Fragment {

    private TextView mDescription1, mDescription2, mLine1, mLine2, mVersion, mSignWifi, mSignStreaming;
    private LinearLayout mSignInformation;
    private Animation mPulseAnimation;
    
    private SpydroidApplication mApplication;
    private CustomHttpServer mHttpServer;
    private RtspServer mRtspServer;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	mApplication  = (SpydroidApplication) getActivity().getApplication();
    }
    
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.main,container,false);
        mLine1 = (TextView)rootView.findViewById(R.id.line1);
        mLine2 = (TextView)rootView.findViewById(R.id.line2);
        mDescription1 = (TextView)rootView.findViewById(R.id.line1_description);
        mDescription2 = (TextView)rootView.findViewById(R.id.line2_description);
        mVersion = (TextView)rootView.findViewById(R.id.version);
        mSignWifi = (TextView)rootView.findViewById(R.id.advice);
        mSignStreaming = (TextView)rootView.findViewById(R.id.streaming);
        mSignInformation = (LinearLayout)rootView.findViewById(R.id.information);
        mPulseAnimation = AnimationUtils.loadAnimation(mApplication.getApplicationContext(), R.anim.pulse);
        return rootView ;
    }
	
	@Override
    public void onStart() {
    	super.onStart();
    	
    	// Print version number
    	Context mContext = mApplication.getApplicationContext();
        try {
			mVersion.setText("v"+mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0 ).versionName);
		} catch (Exception e) {
			mVersion.setText("v???");
		}
    	
    }
    
	@Override
    public void onPause() {
    	super.onPause();
    	update();
    	getActivity().unregisterReceiver(mWifiStateReceiver);
    	getActivity().unbindService(mHttpServiceConnection);
    	getActivity().unbindService(mRtspServiceConnection);
    }
	
	@Override
    public void onResume() {
    	super.onResume();
		getActivity().bindService(new Intent(getActivity(),CustomHttpServer.class), mHttpServiceConnection, Context.BIND_AUTO_CREATE);
		getActivity().bindService(new Intent(getActivity(),CustomRtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
    	getActivity().registerReceiver(mWifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }
	
	public void update() {
		getActivity().runOnUiThread(new Runnable () {
			@Override
			public void run() {
				if (mDescription1 != null) {
					if (mHttpServer != null && mRtspServer != null) {
						if (!mHttpServer.isHttpEnabled() && !mHttpServer.isHttpsEnabled()) {
							mDescription1.setVisibility(View.INVISIBLE);
							mLine1.setVisibility(View.INVISIBLE);
						} else {
							mDescription1.setVisibility(View.VISIBLE);
							mLine1.setVisibility(View.VISIBLE);
						}
						if (!mRtspServer.isEnabled()) {
							mDescription2.setVisibility(View.INVISIBLE);
							mLine2.setVisibility(View.INVISIBLE);
						} else {
							mDescription2.setVisibility(View.VISIBLE);
							mLine2.setVisibility(View.VISIBLE);
						}
						if (!mHttpServer.isStreaming() && !mRtspServer.isStreaming()) displayIpAddress();
						else streamingState(1);
					}		
				}
			}
		});
	}
	
	private void streamingState(int state) {
		if (state==0) {
			// Not streaming
			mSignStreaming.clearAnimation();
			mSignWifi.clearAnimation();
			mSignStreaming.setVisibility(View.GONE);
			mSignInformation.setVisibility(View.VISIBLE);
			mSignWifi.setVisibility(View.GONE);
		} else if (state==1) {
			// Streaming
			mSignWifi.clearAnimation();
			mSignStreaming.setVisibility(View.VISIBLE);
			mSignStreaming.startAnimation(mPulseAnimation);
			mSignInformation.setVisibility(View.INVISIBLE);
			mSignWifi.setVisibility(View.GONE);
		} else if (state==2) {
			// No wifi !
			mSignStreaming.clearAnimation();
			mSignStreaming.setVisibility(View.GONE);
			mSignInformation.setVisibility(View.INVISIBLE);
			mSignWifi.setVisibility(View.VISIBLE);
			mSignWifi.startAnimation(mPulseAnimation);
		}
	}
	
    private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) mApplication.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
		String ipaddress = null;
		String id = (Secure.getString(getActivity().getContentResolver(), Secure.ANDROID_ID));
        String uri = "/devices/" + id + "/camera";
    	if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	        String ip = String.format(Locale.ENGLISH,"%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
	    	mLine1.setText(mHttpServer.isHttpsEnabled()?"https://":"http://");
	    	mLine1.append(ip);
	    	mLine1.append(":"+mHttpServer.getHttpPort());
	    	mLine2.setText("rtsp://");
	    	mLine2.append(ip);
	    	mLine2.append(":"+mRtspServer.getPort());
	    	new Post().execute(uri, mLine2.getText().toString());
	    	
	    	streamingState(0);
	    	
    	} else if((ipaddress = Utilities.getLocalIpAddress(true)) != null) {
    		mLine1.setText(mHttpServer.isHttpsEnabled()?"https://":"http://");
	    	mLine1.append(ipaddress);
	    	mLine1.append(":"+mHttpServer.getHttpPort());
	    	mLine2.setText("rtsp://");
	    	mLine2.append(ipaddress);
	    	mLine2.append(":"+mRtspServer.getPort());
	    	streamingState(0);
    	} else {
    		streamingState(2);
    	}
    	
    }
    
    public class Post extends AsyncTask <String, Void, Void>
    {

        @Override
        protected Void doInBackground(String... array) {
        	Log.i("GOT TO doInBackground", "NOTICE");
            CoapClientCustom ccc = new CoapClientCustom();
            ccc.post(array[0], array[1]);
            return null;
        }
       
    }

    private final ServiceConnection mRtspServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRtspServer = (RtspServer) ((RtspServer.LocalBinder)service).getService();
			update();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}
		
	};
    
    private final ServiceConnection mHttpServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mHttpServer = (CustomHttpServer) ((TinyHttpServer.LocalBinder)service).getService();
			update();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}
		
	};
    
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	// This intent is also received when app resumes even if wifi state hasn't changed :/
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		update();
        	}
        } 
    };
	
}
