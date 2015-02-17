package com.mantz_it.common;

import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Created by dennis on 13/02/15.
 */
public class WearableApiHelper {

	public static GoogleApiClient createAndConnectGoogleApiClient(Context context, int timeout) {
		GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();
		googleApiClient.blockingConnect(timeout, TimeUnit.MILLISECONDS);
		if(googleApiClient.isConnected())
			return googleApiClient;
		else
			return null;
	}

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

	public static boolean sendMessage(GoogleApiClient googleApiClient, String nodeId, String path, byte[] payload, int timeout) {
		MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(googleApiClient,
				nodeId, path, payload).await(timeout, TimeUnit.MILLISECONDS);
		return sendMessageResult.getStatus().isSuccess();
	}
}
