package com.mantz_it.wearnetworknotifications;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.mantz_it.common.CommonPaths;
import com.mantz_it.common.WearableApiHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by dennis on 12/02/15.
 */
public class WatchWearableListenerService extends WearableListenerService {

	private static final String LOGTAG = "WatchWearableListenerService";

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOGTAG, "onCreate");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOGTAG, "onDestroy");
	}

	@Override
	public void onDataChanged(DataEventBuffer dataEvents) {
		Log.d(LOGTAG, "onDataChanged");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		for(DataEvent dataEvent: dataEvents) {
			if(dataEvent.getDataItem().getUri().getPath().equals(CommonPaths.SHARED_PREFERENCES)) {
				DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
				SharedPreferences.Editor edit = sharedPreferences.edit();

				String key = getString(R.string.pref_showNotifications);
				edit.putBoolean(key, dataMap.getBoolean(key));

				key = getString(R.string.pref_vibration);
				edit.putBoolean(key, dataMap.getBoolean(key));

				key = getString(R.string.pref_wearableOffline);
				edit.putBoolean(key, dataMap.getBoolean(key));

				key = getString(R.string.pref_wearableOnline);
				edit.putBoolean(key, dataMap.getBoolean(key));

				key = getString(R.string.pref_showNetworkName);
				edit.putBoolean(key, dataMap.getBoolean(key));

				key = getString(R.string.pref_showSignalStrength);
				edit.putBoolean(key, dataMap.getBoolean(key));

				key = getString(R.string.pref_cellularSignalStrengthUnit);
				edit.putString(key, dataMap.getString(key));

				key = getString(R.string.pref_wifiSignalStrengthUnit);
				edit.putString(key, dataMap.getString(key));

				edit.apply();
			} if(dataEvent.getDataItem().getUri().getPath().equals(CommonPaths.CONNECTION_DATA)) {
				DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
				NetworkNotificationService.updateNotification(this, dataMap.toBundle(), false, sharedPreferences);
			} else
				Log.w(LOGTAG, "onDataChanged: Unknown path: " + dataEvent.getDataItem().getUri().getPath());
		}
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {

		if(messageEvent.getPath().equals(CommonPaths.CONNECTIVITY_CHANGED)) {
			Bundle data = new Bundle();
			Parcel dataParcel = Parcel.obtain();
			dataParcel.unmarshall(messageEvent.getData(), 0, messageEvent.getData().length);
			dataParcel.setDataPosition(0);
			data.readFromParcel(dataParcel);
			Intent intent = new Intent(this, NetworkNotificationService.class);
			intent.setAction(NetworkNotificationService.ACTION_SHOW_NOTIFICATION);
			intent.putExtra(CommonPaths.CONNECTION_DATA, data);
			startService(intent);
		} else if (messageEvent.getPath().equals(CommonPaths.GET_LOG_MESSAGE_PATH)) {
			try {
				// Read the log:
				Process process = Runtime.getRuntime().exec("logcat -d");
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(process.getInputStream()));

				StringBuilder log=new StringBuilder();
				String line = "";
				String newline = System.getProperty("line.separator");
				while ((line = bufferedReader.readLine()) != null) {
					log.append(line);
					log.append(newline);
				}

				// Send it to the handheld device:
				// create and connect the googleApiClient:
				GoogleApiClient googleApiClient = WearableApiHelper
						.createAndConnectGoogleApiClient(this, 1000);
				if(googleApiClient == null) {
					Log.e(LOGTAG, "onMessageReceived: Can't connect the google api client! stop.");
					return;
				}

				// send the message:
				if(!WearableApiHelper.sendMessage(googleApiClient, messageEvent.getSourceNodeId(),
						CommonPaths.GET_LOG_RESPONSE_MESSAGE_PATH, log.toString().getBytes("UTF-8"), 1000))
					Log.e(LOGTAG, "onMessageReceived: Failed to send Message");

				// disconnect the api client:
				googleApiClient.disconnect();
			}
			catch (IOException e) {
				Log.e(LOGTAG, "onMessageReceived: Error while reading log: " + e.getMessage());
			}
		}
	}

	@Override
	public void onPeerConnected(Node peer) {
		Log.d(LOGTAG, "onPeerConnected");
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if(preferences.getBoolean(getString(R.string.pref_wearableOnline), true)
				&& preferences.getBoolean(getString(R.string.pref_showNotifications), true)) {
			Intent intent = new Intent(this, NetworkNotificationService.class);
			intent.setAction(NetworkNotificationService.ACTION_NOW_ONLINE);
			startService(intent);
		}
	}

	@Override
	public void onPeerDisconnected(Node peer) {
		Log.d(LOGTAG, "onPeerDisconnected");
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if(preferences.getBoolean(getString(R.string.pref_wearableOffline), true)
				&& preferences.getBoolean(getString(R.string.pref_showNotifications), true)) {
			Intent intent = new Intent(this, NetworkNotificationService.class);
			intent.setAction(NetworkNotificationService.ACTION_NOW_OFFLINE);
			startService(intent);
		}
	}
}
