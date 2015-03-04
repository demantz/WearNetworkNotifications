package com.mantz_it.wearnetworknotifications;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.mantz_it.common.CommonPaths;
import com.mantz_it.common.WearableApiHelper;

/**
 * <h1>Wear Network Notifications - Phone Wearable Listener Service</h1>
 *
 * Module:      PhoneWearableListenerService.java
 * Description: This service gets invoked by events related to the wearable API.
 *              Tasks:
 *              - Responding to REQUEST_UPDATE messages by sending updated connection data.
 *              - Sending lasted shared preferences to the wearable when it connects.
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
public class PhoneWearableListenerService extends WearableListenerService {

	private static final String LOGTAG = "PhoneWearableLS";

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOGTAG, "onCreate");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		super.onMessageReceived(messageEvent);
		if(messageEvent.getPath().equals(CommonPaths.REQUEST_UPDATE)) {
			// The wearable has requested updated connection data. collect and send data in a
			// separate thread:
			new Thread() {
				public void run() {
					Log.d(LOGTAG, "onMessageReceived: Thread " + this.getName() + " started!");

					// create and connect the googleApiClient:
					GoogleApiClient googleApiClient = WearableApiHelper
							.createAndConnectGoogleApiClient(PhoneWearableListenerService.this, 1000);
					if(googleApiClient == null) {
						Log.e(LOGTAG, "onMessageReceived (Thread="+this.getName()+"): Can't connect the google api client! stop.");
						return;
					}

					// Enumerate nodes to find wearable node:
					Node wearableNode = WearableApiHelper.getOpponentNode(googleApiClient, 1000);
					if(wearableNode == null) {
						Log.e(LOGTAG, "onMessageReceived (Thread="+this.getName()+"): Can't get the wearable node! stop.");
						googleApiClient.disconnect();
						return;
					}

					// sync the data:
					WearableApiHelper.updateConnectionData(googleApiClient, PhoneWearableListenerService.this);

					// disconnect the api client:
					googleApiClient.disconnect();

					Log.d(LOGTAG, "onMessageReceived: Thread " + this.getName() + " stopped!");
				}
			}.start();
		}
	}

	/**
	 * Gets called after a wearable node connects to the phone. Will synchronize the
	 * shared preferences to the new node.
	 *
	 * @param peer		node ID of the connected wearable
	 */
	@Override
	public void onPeerConnected(Node peer) {
		Log.d(LOGTAG, "onPeerConnected: update preferences...");

		// create and connect the googleApiClient:
		GoogleApiClient googleApiClient = WearableApiHelper
				.createAndConnectGoogleApiClient(PhoneWearableListenerService.this, 1000);
		if(googleApiClient == null) {
			Log.e(LOGTAG, "onPeerConnected: Can't connect the google api client! stop.");
			return;
		}

		// update the preferences:
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		WearableApiHelper.updateSharedPreferences(googleApiClient, PhoneWearableListenerService.this, preferences);

		// disconnect the api client:
		googleApiClient.disconnect();
	}
}
