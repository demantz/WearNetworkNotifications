package com.mantz_it.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * <h1>Wear Network Notifications - Wearable API Helper</h1>
 *
 * Module:      WearableApiHelper.java
 * Description: Collection of helper functions to create and connect google Api clients, retrieve
 *              opponent node id, send messages, ...
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
public class WearableApiHelper {

	/**
	 * Will create a Google Api Client and connect it. This method will block!
	 *
	 * @param context	application context
	 * @param timeout	max time this method will block before giving up
	 * @return the connected api client or null on error
	 */
	public static GoogleApiClient createAndConnectGoogleApiClient(Context context, int timeout) {
		GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();
		googleApiClient.blockingConnect(timeout, TimeUnit.MILLISECONDS);
		if(googleApiClient.isConnected())
			return googleApiClient;
		else
			return null;
	}

	/**
	 * Will return the node instance of the opponent device (wearable or phone). This method will block!
	 *
	 * @param googleApiClient		connected GoogleApiClient instance
	 * @param timeout				max time this method will block before giving up
	 * @return node instance or null on error
	 */
	public static Node getOpponentNode(GoogleApiClient googleApiClient, int timeout) {
		NodeApi.GetConnectedNodesResult getConnectedNodesResult = Wearable.NodeApi
				.getConnectedNodes(googleApiClient).await(timeout, TimeUnit.MILLISECONDS);
		if(getConnectedNodesResult == null || !getConnectedNodesResult.getStatus().isSuccess())
			return null;

		if(!getConnectedNodesResult.getNodes().isEmpty())
			return getConnectedNodesResult.getNodes().get(0);
		else
			return null;
	}

	/**
	 * Will send a message to the given recipient. This method will block!
	 * @param googleApiClient	connected GoogleApiClient instance
	 * @param nodeId			node ID of the recipient
	 * @param path				message path
	 * @param payload			message payload
	 * @param timeout			max time this method will block before giving up
	 * @return true on success and false on error.
	 */
	public static boolean sendMessage(GoogleApiClient googleApiClient, String nodeId, String path, byte[] payload, int timeout) {
		MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(googleApiClient,
				nodeId, path, payload).await(timeout, TimeUnit.MILLISECONDS);
		return sendMessageResult.getStatus().isSuccess();
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
		dataMap.putLong("TIMESTAMP", System.currentTimeMillis());

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

		Bundle connectionData = ConnectionData.gatherConnectionData(context).toBundle();
		putDataMapReq.getDataMap().putAll(DataMap.fromBundle(connectionData));

		PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
		PendingResult<DataApi.DataItemResult> pendingResult =
				Wearable.DataApi.putDataItem(googleApiClient, putDataReq);
	}
}
