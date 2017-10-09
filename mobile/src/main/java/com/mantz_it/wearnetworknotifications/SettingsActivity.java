package com.mantz_it.wearnetworknotifications;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.mantz_it.common.CommonPaths;
import com.mantz_it.common.ConnectionData;
import com.mantz_it.common.WearableApiHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * <h1>Wear Network Notifications - Settings Activity</h1>
 *
 * Module:      SettingsActivity.java
 * Description: This is the main activity of the mobile app. It holds the SettingsFragment
 *              which enables the user to change preferences. This activity registers a
 *              listener for changed preferences and syncs them to the wearable.
 *              The action bar provides actions to trigger a test notification,
 *              show network information and logs.
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
public class SettingsActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener, NodeApi.NodeListener,
		SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String LOGTAG = "SettingsActivity";
	private static final int COARSE_LOCATION_PERMISSION_REQ = 100;
	private static final int READ_PHONE_STATE_PERMISSION_REQ = 101;
	private GoogleApiClient googleApiClient;
	private Node wearableNode;
	private ProgressDialog progressDialog;
	private SharedPreferences preferences;
	private String versionName = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get version name:
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		// create the SettingsFragment and place it inside the activity:
		SettingsFragment settingsFragment = new SettingsFragment();
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(android.R.id.content, settingsFragment);
		fragmentTransaction.commit();

		// Create a google api client
		googleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Wearable.API)
				.build();

		// Get reference to the shared preferences:
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		// connect the google api client
		googleApiClient.connect();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Check if permissions are granted:
		if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			Log.d(LOGTAG, "onResume: Request permission ACCESS_COARSE_LOCATION...");
			ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, COARSE_LOCATION_PERMISSION_REQ);
		}
		if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
			Log.w(LOGTAG, "onResume: Request permission READ_PHONE_STATE...");
			ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE_PERMISSION_REQ);
		}
	}

	@Override
	protected void onStop() {
		// disconnect the google api client
		if (googleApiClient != null && googleApiClient.isConnected()) {
			Wearable.MessageApi.removeListener(googleApiClient, this);
			googleApiClient.disconnect();
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// unregister change listener:
		if(preferences != null)
			preferences.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		if (id == R.id.action_showHandheldLog) {
			showHandheldLog();
			return true;
		} else if (id == R.id.action_showWearableLog) {
			queryWearableLog();
			return true;
		} else if (id == R.id.action_showNetworkInformation) {
			showNetworkInformation();
			return true;
		} else if (id == R.id.action_sendTestNotification) {
			sendTestNotification();
			return true;
		} else if (id == R.id.action_about) {
			showAboutDialog();
		}

		return super.onOptionsItemSelected(item);
	}


	/**
	 * (ConnectionCallbacks)
	 * Gets called after googleApiClient.connect() was executed successfully
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		Log.d(LOGTAG, "onConnected: googleApiClient connected!");

		// Enumerate nodes:
		Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
			@Override
			public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
				for (Node node : getConnectedNodesResult.getNodes()) {
					Log.i(LOGTAG, "onConnected: Found node: " + node.getDisplayName() + " (" + node.getId() + ")");
					wearableNode = node;	// for now we just expect one single node to be found..
				}
			}
		});

		// Register message and node listener:
		Wearable.MessageApi.addListener(googleApiClient, this);		// will execute onMessageReceived() if a message arrives
		Wearable.NodeApi.addListener(googleApiClient, this);		// will execute onPeerConnected() and onPeerDisconnected()
	}

	/**
	 * (ConnectionCallbacks)
	 * Gets called after googleApiClient.connect() was executed successfully and the api connection is suspended again
	 */
	@Override
	public void onConnectionSuspended(int cause) {
		Log.d(LOGTAG, "onConnectionSuspended: googleApiClient suspended: " + cause);
	}

	/**
	 * (OnConnectionFailedListener)
	 * Gets called after googleApiClient.connect() was executed and failed
	 */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(LOGTAG, "onConnectionFailed: googleApiClient connection failed: " + result.toString());
	}

	/**
	 * (NodeListener)
	 * Gets called if a new node (a wearable) is connected to the phone
	 */
	@Override
	public void onPeerConnected(Node node) {
		Log.i(LOGTAG, "onPeerConnected: Node " + node.getId() + " connected!");
		wearableNode = node;
	}

	/**
	 * (NodeListener)
	 * Gets called if a node (a wearable) disconnects from the phone
	 */
	@Override
	public void onPeerDisconnected(Node node) {
		Log.i(LOGTAG, "onPeerDisconnected: Node " + node.getId() + " has disconnected!");
		if(wearableNode.getId().equals(node.getId())) {
			Log.i(LOGTAG, "onPeerDisconnected: Setting wearable node to null!");
			wearableNode = null;
		}
	}

	/**
	 * (MessageListener)
	 * will receive the response of the get log request and show an alert dialog containing the
	 * wearable log data
	 */
	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		Log.i(LOGTAG, "onMessageReceived: received a message (" + messageEvent.getPath() + ") from "
				+ messageEvent.getSourceNodeId());

		if(messageEvent.getPath().equals(CommonPaths.GET_LOG_RESPONSE_MESSAGE_PATH)) {
			try {
				final String log = new String(messageEvent.getData(), "UTF-8");
				// Show dialog:
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)
								.setTitle(getString(R.string.wearable_log))
								.setMessage(log)
								.setPositiveButton(getString(R.string.share), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										// Invoke email app:
										Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "dennis.mantz@googlemail.com", null));
										intent.putExtra(Intent.EXTRA_SUBJECT, "Wear Network Notifications "
												+ versionName + " log report (wearable)");
										intent.putExtra(Intent.EXTRA_TEXT, log);
										startActivity(Intent.createChooser(intent, getString(R.string.chooseMailApp)));
									}
								})
								.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										// do nothing
									}
								})
								.create();
						dialog.setOnShowListener(new DialogInterface.OnShowListener() {
							@Override
							public void onShow(DialogInterface dialog) {
								// dismiss process dialog:
								if(progressDialog != null)
									progressDialog.dismiss();
							}
						});
						dialog.show();
					}
				});
			} catch (UnsupportedEncodingException e) {
				Log.e(LOGTAG, "onMessageReceived: unsupported Encoding (UTF-8): " + e.getMessage());
				if(progressDialog != null)
					progressDialog.dismiss();
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		sendPreferences(sharedPreferences);
	}

	/**
	 * will use the connected googleApiClient to send a request message to the wearable node asking
	 * for the log data.
	 */
	public void queryWearableLog() {
		if(!googleApiClient.isConnected()) {
			Log.e(LOGTAG, "queryWearableLog: google api client not connected!");
			Toast.makeText(this, getString(R.string.googleApiClient_not_connected), Toast.LENGTH_LONG).show();
			return;
		}
		if(wearableNode == null) {
			Log.e(LOGTAG, "queryWearableLog: wearable node not connected!");
			Toast.makeText(this, getString(R.string.wearable_node_not_connected), Toast.LENGTH_LONG).show();
			return;
		}

		// show a progress dialog
		if(progressDialog == null)
			progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(getString(R.string.loading));
		progressDialog.setMessage(getString(R.string.querying_wearable_for_log));
		progressDialog.show();

		// Send it to the wearable device:
		Wearable.MessageApi.sendMessage(googleApiClient, wearableNode.getId(),
				CommonPaths.GET_LOG_MESSAGE_PATH, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
			@Override
			public void onResult(MessageApi.SendMessageResult sendMessageResult) {
				if (!sendMessageResult.getStatus().isSuccess()) {
					Log.e(LOGTAG, "queryWearableLog: Failed to query log from the wearable: "
							+ sendMessageResult.getStatus().getStatusMessage());
					Toast.makeText(SettingsActivity.this, getString(R.string.failed_to_query_wearable_for_log)
							+ sendMessageResult.getStatus().getStatusMessage(), Toast.LENGTH_LONG).show();
					// dismiss process dialog:
					if (progressDialog != null)
						progressDialog.dismiss();
				} else
					Log.d(LOGTAG, "queryWearableLog: message (" + sendMessageResult.getRequestId() + ") was sent!");
			}
		});
	}

	/**
	 * will show a alert dialog containing the log data of the smartphone
	 */
	public void showHandheldLog() {
		// Read the log:
		StringBuilder log = new StringBuilder();
		try {
			Process process = Runtime.getRuntime().exec("logcat -d");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));

			String line = "";
			String newline = System.getProperty("line.separator");
			while ((line = bufferedReader.readLine()) != null) {
				log.append(line);
				log.append(newline);
			}
		} catch (IOException e) {
			Log.e(LOGTAG, "showHandheldLog: Couldn't read log: " + e.getMessage());
			return;
		}
		final String logString = log.toString();
		new AlertDialog.Builder(SettingsActivity.this)
				.setTitle(getString(R.string.handheld_log))
				.setMessage(log)
				.setPositiveButton(getString(R.string.share), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Invoke email app:
						Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "dennis.mantz@googlemail.com", null));
						intent.putExtra(Intent.EXTRA_SUBJECT, "Wear Network Notifications " + versionName +" log report (handheld)");
						intent.putExtra(Intent.EXTRA_TEXT, logString);
						startActivity(Intent.createChooser(intent, getString(R.string.chooseMailApp)));
					}
				})
				.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// do nothing
					}
				})
				.create()
				.show();
	}

	/**
	 * Will send a notification to the wearable containing the latest connection data.
	 */
	public void sendTestNotification() {
		if(!googleApiClient.isConnected()) {
			Log.e(LOGTAG, "sendTestNotification: google api client not connected!");
			Toast.makeText(this, getString(R.string.googleApiClient_not_connected), Toast.LENGTH_LONG).show();
			return;
		}
		if(wearableNode == null) {
			Log.e(LOGTAG, "sendTestNotification: wearable node not connected!");
			Toast.makeText(this, getString(R.string.wearable_node_not_connected), Toast.LENGTH_LONG).show();
			return;
		}
		new Thread() {
			public void run() {
				// Gather connection data:
				Parcel dataParcel = Parcel.obtain();
				ConnectionData.gatherConnectionData(SettingsActivity.this).toBundle().writeToParcel(dataParcel, 0);

				// send the message:
				if (!WearableApiHelper.sendMessage(googleApiClient, wearableNode.getId(), CommonPaths.CONNECTIVITY_CHANGED,
						dataParcel.marshall(), 1000))
					Log.e(LOGTAG, "sendTestNotification: Failed to send Message");
			}
		}.start();
	}

	/**
	 * Will show a dialog that presents the current connection information
	 */
	public void showNetworkInformation() {
		String networkInformationString = getNetworkInformation();
		ConnectionData conData = ConnectionData.gatherConnectionData(this);
		ImageView indicator = new ImageView(this);
		indicator.setBackgroundColor(Color.BLACK);
		indicator.setMinimumWidth(400);
		indicator.setMinimumHeight(400);
		indicator.setPadding(50,50,50,50);
		indicator.setImageResource(conData.getIndicatorIconRes());
		new AlertDialog.Builder(SettingsActivity.this)
				.setCustomTitle(indicator)
				.setMessage(networkInformationString)
				.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// do nothing
					}
				})
				.create()
				.show();
		Log.d(LOGTAG, "showNetworkInformation: " + networkInformationString.replace("\n", "    "));
	}

	// DEBUG
	public String getNetworkInformation() {
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		StringBuilder stringBuilder = new StringBuilder();

		NetworkInfo[] networkInfos = cm.getAllNetworkInfo();
		for(int i = 0; i < 2; i++) {
			printNetworkInfo(networkInfos[i], stringBuilder);
			stringBuilder.append("\n");
		}

		stringBuilder.append("Network Operator: " + tm.getNetworkOperator() + "\n");
		stringBuilder.append("Network Operator Name: " + tm.getNetworkOperatorName() + "\n");
		stringBuilder.append("Sim Operator Name: " + tm.getSimOperatorName() + "\n");
		stringBuilder.append("Phone Type: " + tm.getPhoneType() + "\n");
		stringBuilder.append("Cell State: " + tm.getCallState() + "\n");
		stringBuilder.append("Network Type: " + tm.getNetworkType() + "\n");
		stringBuilder.append("Data State: " + tm.getDataState() + "\n");
		CellInfo cellInfo = null;
		if(tm.getAllCellInfo() != null && !tm.getAllCellInfo().isEmpty()) {
			cellInfo = tm.getAllCellInfo().get(0);
			stringBuilder.append("Cell toString: " + cellInfo.toString() + "\n");
		}
		int dbm = Integer.MIN_VALUE;
		int level = Integer.MIN_VALUE;
		int asuLevel = Integer.MIN_VALUE;
		if(cellInfo instanceof CellInfoGsm) {
			CellInfoGsm cellInfoDetail = (CellInfoGsm) cellInfo;
			stringBuilder.append("Cell is GSM!");
			dbm = cellInfoDetail.getCellSignalStrength().getDbm();
			level = cellInfoDetail.getCellSignalStrength().getLevel();
			asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
		}
		if(cellInfo instanceof CellInfoCdma) {
			CellInfoCdma cellInfoDetail = (CellInfoCdma) cellInfo;
			stringBuilder.append("Cell is CDMA!");
			dbm = cellInfoDetail.getCellSignalStrength().getDbm();
			level = cellInfoDetail.getCellSignalStrength().getLevel();
			asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
		}
		if(cellInfo instanceof CellInfoLte) {
			CellInfoLte cellInfoDetail = (CellInfoLte) cellInfo;
			stringBuilder.append("Cell is LTE!");
			dbm = cellInfoDetail.getCellSignalStrength().getDbm();
			level = cellInfoDetail.getCellSignalStrength().getLevel();
			asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
		}
		if(cellInfo instanceof CellInfoWcdma) {
			CellInfoWcdma cellInfoDetail = (CellInfoWcdma) cellInfo;
			stringBuilder.append("Cell is WCDMA!");
			dbm = cellInfoDetail.getCellSignalStrength().getDbm();
			level = cellInfoDetail.getCellSignalStrength().getLevel();
			asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
		}
		stringBuilder.append("dBm: " + dbm + "\n");
		stringBuilder.append("Level: " + level + "\n");
		stringBuilder.append("ASU Level: " + asuLevel + "\n");

		stringBuilder.append("\n");
		//print("Frequency: " + wm.getConnectionInfo().getFrequency());
		stringBuilder.append("Rssi: " + wm.getConnectionInfo().getRssi() + "\n");
		stringBuilder.append("ToString: " + wm.getConnectionInfo().toString() + "\n");

		return stringBuilder.toString();
	}

	/**
	 * Will print the data from the NetworkInfo into the given StringBuilder instance
	 *
	 * @param info				NetworkInfo object
	 * @param stringBuilder		StringBuilder instance
	 */
	public void printNetworkInfo(NetworkInfo info, StringBuilder stringBuilder) {
		stringBuilder.append("Type=" + info.getTypeName() + "(" + info.getType() + ")\n");
		stringBuilder.append("SubType=" + info.getSubtypeName() + "(" + info.getSubtype() + ")\n");
		stringBuilder.append("State=" + info.getState().toString() + "\n");
		stringBuilder.append("Reason=" + info.getReason() + "\n");
		stringBuilder.append("Available=" + info.isAvailable() + "\n");
		stringBuilder.append("Connected=" + info.isConnected() + "\n");
		stringBuilder.append("Roaming=" + info.isRoaming() + "\n");
		stringBuilder.append("Failover=" + info.isFailover() + "\n");
		stringBuilder.append("DetailedState=" + info.getDetailedState().toString() + "\n");
		stringBuilder.append("ExtraInfo=" + info.getExtraInfo() + "\n");
		stringBuilder.append("toString=" + info.toString() + "\n");
	}

	/**
	 * Will trigger an update of the shared preferences to the wearable
	 *
	 * @param sharedPreferences		Shared Preferences instance
	 */
	public void sendPreferences(SharedPreferences sharedPreferences) {
		if(!googleApiClient.isConnected()) {
			Log.e(LOGTAG, "sendPreferences: google api client not connected!");
			return;
		}
		if(wearableNode == null) {
			Log.e(LOGTAG, "sendPreferences: wearable node not connected!");
			return;
		}
		WearableApiHelper.updateSharedPreferences(googleApiClient, this, sharedPreferences);
	}

	public void showAboutDialog() {

		AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)
				.setTitle(Html.fromHtml(getString(R.string.about_title)))
				.setMessage(Html.fromHtml(getString(R.string.about_msg_body, versionName)))
				.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Do nothing
					}
				})
				.create();
		dialog.show();

		// make links clickable:
		((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
	}
}
