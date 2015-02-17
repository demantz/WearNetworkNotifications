package com.mantz_it.wearnetworknotifications;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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
 * Created by dennis on 13/02/15.
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
			// update data in the background:
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

	public static void updateConnectionData(GoogleApiClient googleApiClient, Context context) {
		PutDataMapRequest putDataMapReq = PutDataMapRequest.create(CommonPaths.CONNECTION_DATA);

		putDataMapReq.getDataMap().putAll(DataMap.fromBundle(gatherConnectionData(context)));

		PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
		PendingResult<DataApi.DataItemResult> pendingResult =
				Wearable.DataApi.putDataItem(googleApiClient, putDataReq);
	}

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
