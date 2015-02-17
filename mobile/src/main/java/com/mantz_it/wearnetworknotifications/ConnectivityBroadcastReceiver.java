package com.mantz_it.wearnetworknotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.mantz_it.common.CommonPaths;
import com.mantz_it.common.WearableApiHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Created by dennis on 12/02/15.
 */
public class ConnectivityBroadcastReceiver extends BroadcastReceiver {
	private static final String LOGTAG = "ConnectivityBroadcastReceiver";
	private long lastMessageSentTimestamp = 0;

	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.d(LOGTAG, "onReceive: " + intent.toString());
		if(!intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")){
			Log.w(LOGTAG, "onReceive: received unknown intent: " + intent.getAction());
			return;
		}
		final Bundle extras = intent.getExtras();
//		long currentTime = System.currentTimeMillis();
//		if(currentTime - lastMessageSentTimestamp < 10000) {
//			Log.d(LOGTAG, "onReceive: Last notification was sent less than ten seconds ago!");
//			return;
//		}
//		lastMessageSentTimestamp = currentTime;

		// send message in the background:
		new Thread() {
			public void run() {
				Log.d(LOGTAG, "onReceive: Thread " + this.getName() + " started!");

				// create and connect the googleApiClient:
				GoogleApiClient googleApiClient = WearableApiHelper
						.createAndConnectGoogleApiClient(context, 1000);
				if(googleApiClient == null) {
					Log.e(LOGTAG, "onReceive (Thread="+this.getName()+"): Can't connect the google api client! stop.");
					return;
				}

				// Enumerate nodes to find wearable node:
				Node wearableNode = WearableApiHelper.getOpponentNode(googleApiClient, 1000);
				if(wearableNode == null) {
					Log.e(LOGTAG, "onReceive (Thread="+this.getName()+"): Can't get the wearable node! stop.");
					googleApiClient.disconnect();
					return;
				}

				// Gather connection data:
				Bundle data = PhoneWearableListenerService.gatherConnectionData(context);
				Parcel dataParcel = Parcel.obtain();
				data.writeToParcel(dataParcel, 0);

				// Debug: write event to file
				try {
					File file = new File(context.getFilesDir(), "NetworkEvents.txt" );
					System.out.println("FILE: " + file.getAbsolutePath());
					FileWriter fileWriter = new FileWriter(file, true);
					fileWriter.write("\n" + (new Date()).toString() + "\n");
					fileWriter.write("CON-DATA: " + data.toString() + "\n");
					fileWriter.write("EVENT-DATA: \n");
					if(extras != null) {
						for(String key: extras.keySet()) {
							Object tmp = extras.get(key);
							fileWriter.write("    " + key + "=" + tmp.toString() + "\n");
						}
					}
					fileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// send the message:
				if(!WearableApiHelper.sendMessage(googleApiClient, wearableNode.getId(), CommonPaths.CONNECTIVITY_CHANGED,
						dataParcel.marshall(), 1000))
					Log.e(LOGTAG, "onReceive (Thread="+this.getName()+"): Failed to send Message");

				// disconnect the api client:
				googleApiClient.disconnect();

				Log.d(LOGTAG, "onReceive: Thread " + this.getName() + " stopped!");
			}
		}.start();

	}
}
