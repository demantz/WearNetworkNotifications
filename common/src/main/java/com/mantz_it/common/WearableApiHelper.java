package com.mantz_it.common;

import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
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
}
