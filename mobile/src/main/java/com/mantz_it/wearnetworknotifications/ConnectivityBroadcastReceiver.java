package com.mantz_it.wearnetworknotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.mantz_it.common.CommonKeys;
import com.mantz_it.common.CommonPaths;
import com.mantz_it.common.WearableApiHelper;

import java.util.Date;

/**
 * Created by dennis on 12/02/15.
 */
public class ConnectivityBroadcastReceiver extends BroadcastReceiver {
	private static final String LOGTAG = "ConnectivityBroadcastReceiver";
	private static final int INVALID	= 0;
	private static final int OFFLINE	= 1;
	private static final int MOBILE		= 2;
	private static final int WIFI		= 3;
	private static final String[] STATE_NAMES = {"INVALID", "OFFLINE", "MOBILE", "WIFI"};
	private static int lastConnectifityState = INVALID;

	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.d(LOGTAG, "onReceive: " + intent.toString());
		if(!intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")){
			Log.w(LOGTAG, "onReceive: received unknown intent: " + intent.getAction());
			return;
		}
		final Bundle extras = intent.getExtras();
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

		if(preferences.getBoolean(context.getString(R.string.pref_showNotifications), true)) {
			new Thread() {
				public void run() {
					Log.d(LOGTAG, "onReceive: Thread " + this.getName() + " started!");

					// Gather connection data:
					Bundle data = PhoneWearableListenerService.gatherConnectionData(context);
					Parcel dataParcel = Parcel.obtain();
					data.writeToParcel(dataParcel, 0);

					// determine the current state:
					int currentConnectifityState = INVALID;
					if (data.getInt(CommonKeys.WIFI_SPEED) < 0) {
						// WIFI is disconnected
						if (data.getString(CommonKeys.CELLULAR_NETWORK_OPERATOR).length() == 0
								|| data.getInt(CommonKeys.CELLULAR_NETWORK_TYPE) == 0) {
							// CELLULAR is also disconnected. we are offline.
							currentConnectifityState = OFFLINE;
						} else {
							// CELLULAR is connected
							currentConnectifityState = MOBILE;
						}
					} else {
						// WIFI is connected
						currentConnectifityState = WIFI;
					}

					Log.i(LOGTAG, "onReceive (Thread=" + this.getName() + "): " + (new Date()).toString() + " Event=" + extras.get("networkInfo"));
					Log.i(LOGTAG, "onReceive (Thread=" + this.getName() + "): ConnectifityState: " + STATE_NAMES[lastConnectifityState]
							+ " ==> " + STATE_NAMES[currentConnectifityState]);

					boolean sendNotification = false;
					if(lastConnectifityState == WIFI && currentConnectifityState == OFFLINE
							&& preferences.getBoolean(context.getString(R.string.pref_wifiOffline), true))
						sendNotification = true;
					else if(lastConnectifityState == WIFI && currentConnectifityState == MOBILE
							&& preferences.getBoolean(context.getString(R.string.pref_wifiCellular), true))
						sendNotification = true;
					else if(lastConnectifityState == MOBILE && currentConnectifityState == OFFLINE
								&& preferences.getBoolean(context.getString(R.string.pref_cellularOffline), true))
						sendNotification = true;
					else if(lastConnectifityState == MOBILE && currentConnectifityState == WIFI
							&& preferences.getBoolean(context.getString(R.string.pref_cellularWifi), true))
						sendNotification = true;
					else if(lastConnectifityState == OFFLINE && currentConnectifityState == WIFI
							&& preferences.getBoolean(context.getString(R.string.pref_offlineWifi), true))
						sendNotification = true;
					else if(lastConnectifityState == OFFLINE && currentConnectifityState == MOBILE
							&& preferences.getBoolean(context.getString(R.string.pref_offlineCellular), true))
						sendNotification = true;

					lastConnectifityState = currentConnectifityState;
					if(!sendNotification) {
						Log.d(LOGTAG, "onReceive (Thread=" + this.getName() + "): No notification will be send according to the prefs.");
						return;
					}
					Log.d(LOGTAG, "onReceive (Thread=" + this.getName() + "): Sending notification...");

					// create and connect the googleApiClient:
					GoogleApiClient googleApiClient = WearableApiHelper
							.createAndConnectGoogleApiClient(context, 1000);
					if (googleApiClient == null) {
						Log.e(LOGTAG, "onReceive (Thread=" + this.getName() + "): Can't connect the google api client! stop.");
						return;
					}

					// Enumerate nodes to find wearable node:
					Node wearableNode = WearableApiHelper.getOpponentNode(googleApiClient, 1000);
					if (wearableNode == null) {
						Log.e(LOGTAG, "onReceive (Thread=" + this.getName() + "): Can't get the wearable node! stop.");
						googleApiClient.disconnect();
						return;
					}

					// send the message:
					if (!WearableApiHelper.sendMessage(googleApiClient, wearableNode.getId(), CommonPaths.CONNECTIVITY_CHANGED,
							dataParcel.marshall(), 1000))
						Log.e(LOGTAG, "onReceive (Thread=" + this.getName() + "): Failed to send Message");

					// disconnect the api client:
					googleApiClient.disconnect();

					Log.d(LOGTAG, "onReceive: Thread " + this.getName() + " stopped!");
				}
			}.start();
		}
	}
}
