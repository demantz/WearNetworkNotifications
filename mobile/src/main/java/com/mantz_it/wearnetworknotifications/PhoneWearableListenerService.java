package com.mantz_it.wearnetworknotifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.mantz_it.common.CommonKeys;
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

	private static final String LOGTAG = "PhoneWearableListenerService";

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
					updateConnectionData(googleApiClient, PhoneWearableListenerService.this);

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
		updateSharedPreferences(googleApiClient, PhoneWearableListenerService.this, preferences);

		// disconnect the api client:
		googleApiClient.disconnect();
	}

	/**
	 * Will put all relevant shared preferences into a DataItem to be synced to the wearable.
	 *
	 * @param googleApiClient		connected google API client
	 * @param context				application context
	 * @param sharedPreferences		shared preferences instance
	 */
	public static void updateSharedPreferences(GoogleApiClient googleApiClient, Context context,
											   SharedPreferences sharedPreferences) {
		PutDataMapRequest putDataMapReq = PutDataMapRequest.create(CommonPaths.SHARED_PREFERENCES);
		DataMap dataMap = putDataMapReq.getDataMap();

		String key = context.getString(R.string.pref_showNotifications);
		dataMap.putBoolean(key, sharedPreferences.getBoolean(key, true));

		key = context.getString(R.string.pref_vibration);
		dataMap.putBoolean(key, sharedPreferences.getBoolean(key, true));

		key = context.getString(R.string.pref_wearableOffline);
		dataMap.putBoolean(key, sharedPreferences.getBoolean(key, true));

		key = context.getString(R.string.pref_wearableOnline);
		dataMap.putBoolean(key, sharedPreferences.getBoolean(key, true));

		key = context.getString(R.string.pref_showNetworkName);
		dataMap.putBoolean(key, sharedPreferences.getBoolean(key, true));

		key = context.getString(R.string.pref_showSignalStrength);
		dataMap.putBoolean(key, sharedPreferences.getBoolean(key, true));

		key = context.getString(R.string.pref_cellularSignalStrengthUnit);
		dataMap.putString(key, sharedPreferences.getString(key, "0"));

		key = context.getString(R.string.pref_wifiSignalStrengthUnit);
		dataMap.putString(key, sharedPreferences.getString(key, "0"));

		// also add a timestamp:
		dataMap.putLong(CommonKeys.TIMESTAMP, System.currentTimeMillis());

		PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
		PendingResult<DataApi.DataItemResult> pendingResult =
				Wearable.DataApi.putDataItem(googleApiClient, putDataReq);

		// also send fresh connection data:
		updateConnectionData(googleApiClient, context);
	}

	/**
	 * Will gather the connection data and put them into a DataItem to be synced to the wearable
	 *
	 * @param googleApiClient	connected google API client
	 * @param context			application context
	 */
	public static void updateConnectionData(GoogleApiClient googleApiClient, Context context) {
		PutDataMapRequest putDataMapReq = PutDataMapRequest.create(CommonPaths.CONNECTION_DATA);

		putDataMapReq.getDataMap().putAll(DataMap.fromBundle(gatherConnectionData(context)));

		PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
		PendingResult<DataApi.DataItemResult> pendingResult =
				Wearable.DataApi.putDataItem(googleApiClient, putDataReq);
	}

	/**
	 * Will gather all relevant connection data from Wifi- and TelephonyManager.
	 *
	 * @param context	application context
	 * @return Bundle containing the connection data
	 */
	public static Bundle gatherConnectionData(Context context) {
		Bundle data = new Bundle();

		// Wifi data
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		data.putString(CommonKeys.WIFI_SSID, wm.getConnectionInfo().getSSID());
		data.putInt(CommonKeys.WIFI_RSSI, wm.getConnectionInfo().getRssi());
		data.putInt(CommonKeys.WIFI_SPEED, wm.getConnectionInfo().getLinkSpeed());

		// Cellular data
		TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
		data.putString(CommonKeys.CELLULAR_NETWORK_OPERATOR, tm.getNetworkOperatorName());
		data.putInt(CommonKeys.CELLULAR_NETWORK_TYPE, tm.getNetworkType());
		// Get the signal strength by looking at the first connected cell (if it exists):
		int dbm = Integer.MIN_VALUE;
		int asuLevel = Integer.MIN_VALUE;
		if(tm.getAllCellInfo() != null && !tm.getAllCellInfo().isEmpty()) {
			CellInfo cellInfo = tm.getAllCellInfo().get(0);
			if (cellInfo instanceof CellInfoGsm) {
				CellInfoGsm cellInfoDetail = (CellInfoGsm) cellInfo;
				dbm = cellInfoDetail.getCellSignalStrength().getDbm();
				asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
			} else if (cellInfo instanceof CellInfoCdma) {
				CellInfoCdma cellInfoDetail = (CellInfoCdma) cellInfo;
				dbm = cellInfoDetail.getCellSignalStrength().getDbm();
				asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
			} else if (cellInfo instanceof CellInfoLte) {
				CellInfoLte cellInfoDetail = (CellInfoLte) cellInfo;
				dbm = cellInfoDetail.getCellSignalStrength().getDbm();
				asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
			} else if (cellInfo instanceof CellInfoWcdma) {
				CellInfoWcdma cellInfoDetail = (CellInfoWcdma) cellInfo;
				dbm = cellInfoDetail.getCellSignalStrength().getDbm();
				asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
			}
		}
		data.putInt(CommonKeys.CELLULAR_DBM, dbm);
		data.putInt(CommonKeys.CELLULAR_ASU_LEVEL, asuLevel);

		// also add a timestamp:
		data.putLong(CommonKeys.TIMESTAMP, System.currentTimeMillis());

		return data;
	}
}
