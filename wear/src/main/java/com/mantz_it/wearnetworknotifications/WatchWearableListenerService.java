package com.mantz_it.wearnetworknotifications;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.mantz_it.common.CommonPaths;

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
		for(DataEvent dataEvent: dataEvents) {
			if(dataEvent.getDataItem().getUri().getPath().equals(CommonPaths.CONNECTION_DATA)) {
				DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
				NetworkNotificationService.updateNotification(this, dataMap.toBundle(), false);
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
		}
	}

	@Override
	public void onPeerConnected(Node peer) {
		Log.d(LOGTAG, "onPeerConnected");
		Intent intent = new Intent(this, NetworkNotificationService.class);
		intent.setAction(NetworkNotificationService.ACTION_NOW_ONLINE);
		startService(intent);
	}

	@Override
	public void onPeerDisconnected(Node peer) {
		Log.d(LOGTAG, "onPeerDisconnected");
		Intent intent = new Intent(this, NetworkNotificationService.class);
		intent.setAction(NetworkNotificationService.ACTION_NOW_OFFLINE);
		startService(intent);
	}
}
