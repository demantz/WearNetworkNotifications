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
 * <h1>Wear Network Notifications - Connectivity Broadcast Receiver</h1>
 *
 * Module:      ConnectivityBroadcastReceiver.java
 * Description: This BroadcastReceiver will receive events related to connectivity and
 *              network changes. It will analyze the situation and send a notification
 *              message to the wearable if a state change was detected.
 *
 * @author Dennis Mantz
 *
 * Copyright (C) 2015 Dennis Mantz
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
public class ConnectivityBroadcastReceiver extends BroadcastReceiver {
	private static final String LOGTAG = "ConnectivityBroadcastReceiver";
	private static final int INVALID	= 0;
	private static final int OFFLINE	= 1;
	private static final int MOBILE		= 2;
	private static final int WIFI		= 3;
	private static final String[] STATE_NAMES = {"INVALID", "OFFLINE", "MOBILE", "WIFI"};
	private static int lastConnectivityState = INVALID;

	/**
	 * Gets called after an CONNECTIVITY_CHANGE event. Will evaluate the necessary actions
	 * and might send a message to notify the wearable.
	 *
	 * @param context	application context
	 * @param intent	CONNECTIVITY_CHANGE intent
	 */
	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.d(LOGTAG, "onReceive: " + intent.toString());
		if(!intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")){
			Log.w(LOGTAG, "onReceive: received unknown intent: " + intent.getAction());
			return;
		}
		final Bundle extras = intent.getExtras();
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

		// only look into current and last state if notifications are enabled.
		if(preferences.getBoolean(context.getString(R.string.pref_showNotifications), true)) {
			// do evaluation and message sending in a separate thread:
			new Thread() {
				public void run() {
					Log.d(LOGTAG, "onReceive: Thread " + this.getName() + " started!");

					// Gather connection data:
					Bundle data = PhoneWearableListenerService.gatherConnectionData(context);
					Parcel dataParcel = Parcel.obtain();
					data.writeToParcel(dataParcel, 0);

					// determine the current state:
					int currentConnectivityState = INVALID;
					if (data.getInt(CommonKeys.WIFI_SPEED) < 0) {
						// WIFI is disconnected
						if (data.getString(CommonKeys.CELLULAR_NETWORK_OPERATOR).length() == 0
								|| data.getInt(CommonKeys.CELLULAR_NETWORK_TYPE) == 0) {
							// CELLULAR is also disconnected. we are offline.
							currentConnectivityState = OFFLINE;
						} else {
							// CELLULAR is connected
							currentConnectivityState = MOBILE;
						}
					} else {
						// WIFI is connected
						currentConnectivityState = WIFI;
					}

					Log.i(LOGTAG, "onReceive (Thread=" + this.getName() + "): " + (new Date()).toString() + " Event=" + extras.get("networkInfo"));
					Log.i(LOGTAG, "onReceive (Thread=" + this.getName() + "): ConnectifityState: " + STATE_NAMES[lastConnectivityState]
							+ " ==> " + STATE_NAMES[currentConnectivityState]);

					// check if we have to send an notification according to the current settings:
					boolean sendNotification = false;
					if(lastConnectivityState == WIFI && currentConnectivityState == OFFLINE
							&& preferences.getBoolean(context.getString(R.string.pref_wifiOffline), true))
						sendNotification = true;
					else if(lastConnectivityState == WIFI && currentConnectivityState == MOBILE
							&& preferences.getBoolean(context.getString(R.string.pref_wifiCellular), true))
						sendNotification = true;
					else if(lastConnectivityState == MOBILE && currentConnectivityState == OFFLINE
								&& preferences.getBoolean(context.getString(R.string.pref_cellularOffline), true))
						sendNotification = true;
					else if(lastConnectivityState == MOBILE && currentConnectivityState == WIFI
							&& preferences.getBoolean(context.getString(R.string.pref_cellularWifi), true))
						sendNotification = true;
					else if(lastConnectivityState == OFFLINE && currentConnectivityState == WIFI
							&& preferences.getBoolean(context.getString(R.string.pref_offlineWifi), true))
						sendNotification = true;
					else if(lastConnectivityState == OFFLINE && currentConnectivityState == MOBILE
							&& preferences.getBoolean(context.getString(R.string.pref_offlineCellular), true))
						sendNotification = true;

					lastConnectivityState = currentConnectivityState;
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
