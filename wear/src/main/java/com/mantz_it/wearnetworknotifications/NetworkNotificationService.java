package com.mantz_it.wearnetworknotifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.mantz_it.common.CommonPaths;
import com.mantz_it.common.ConnectionData;
import com.mantz_it.common.WearableApiHelper;

/**
 * <h1>Wear Network Notifications - Network Notification Service</h1>
 *
 * Module:      NetworkNotificationService.java
 * Description: This service will show a notification on the wearable when invoked and live as
 *              long as the notification is visible. It registers a BroadcastReceiver to intercept
 *              wake-up events and initiate an update on each wake-up.
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
public class NetworkNotificationService extends Service {
	private static final String LOGTAG = "NetworkNotificationS";
	public static final String ACTION_SHOW_NOTIFICATION = "com.mantz_it.wearnetworknotifications.ACTION_SHOW_NOTIFICATION";
	public static final String ACTION_SHOW_COMPILATION = "com.mantz_it.wearnetworknotifications.ACTION_SHOW_COMPILATION";
	public static final String ACTION_NOW_ONLINE = "com.mantz_it.wearnetworknotifications.ACTION_NOW_ONLINE";
	public static final String ACTION_NOW_OFFLINE = "com.mantz_it.wearnetworknotifications.ACTION_NOW_OFFLINE";
	public static final String ACTION_DISMISS_NOTIFICATION = "com.mantz_it.wearnetworknotifications.ACTION_DISMISS_NOTIFICATION";
	public static final String ACTION_DISMISS_COMPILATION = "com.mantz_it.wearnetworknotifications.ACTION_DISMISS_COMPILATION";
	public static final String ACTION_REQUEST_UPDATE = "com.mantz_it.wearnetworknotifications.ACTION_REQUEST_UPDATE";
	private static final int NOTIFICATION_ID = 1;
	private static boolean notificationShowing = false;
	private static boolean compilationShowing = false;
	private static long notificationShowTime = 0;
	private static Bitmap largeIcon = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);	// Large background icon for the notification
	private static Canvas largeIconCanvas = new Canvas(largeIcon);	// Will be used to draw the current icon on the large bitmap
	private static Paint paint = new Paint();						// Will be used to draw the current icon on the large bitmap
	private BroadcastReceiver wakeUpBroadcastReceiver;
	private boolean connected = true;	// true if we have a connection to the phone
	private SharedPreferences sharedPreferences;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOGTAG, "onCreate");
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		// Create a BroadcastReceiver to receive 'wake up' / 'request update' intents
		// as well as 'dismiss notification' intents:
		wakeUpBroadcastReceiver = new BroadcastReceiver(){
			private long lastUpdateRequestTimestamp = 0;
			@Override
			public void onReceive(final Context context, Intent intent) {
				if(intent.getAction().equals("android.intent.action.SCREEN_ON")
						|| intent.getAction().equals("android.intent.action.DREAMING_STOPPED")
						|| intent.getAction().equals(ACTION_REQUEST_UPDATE)) {

					// First check if there is a notification showing, which should be auto-dismissed:
					Log.d(LOGTAG, "wakeUpBroadcastReceiver.onReceive: debug: autodismiss=" + sharedPreferences.getString(getString(R.string.pref_autoDismissNotification), "0"));
					if(notificationShowing && Integer.valueOf(sharedPreferences.getString(getString(R.string.pref_autoDismissNotification), "0")) > 0) {
						if(System.currentTimeMillis() - notificationShowTime > Integer.valueOf(sharedPreferences.getString(getString(R.string.pref_autoDismissNotification), "0")) * 1000) {
							Log.i(LOGTAG, "wakeUpBroadcastReceiver.onReceive: Auto-dismiss notification!");
							Intent dismissIntent = new Intent(ACTION_DISMISS_NOTIFICATION);
							sendBroadcast(dismissIntent);
							if(!compilationShowing)
								return;
						}
					}

					// check if we sent an update request less than five seconds ago:
					long currentTime = System.currentTimeMillis();
					if(currentTime - lastUpdateRequestTimestamp < 5000) {
						Log.d(LOGTAG, "wakeUpBroadcastReceiver.onReceive: Last request was less than five seconds ago!");
						return;
					}
					lastUpdateRequestTimestamp = currentTime;

					// check if we have a connection to the phone:
					if(!connected) {
						Log.d(LOGTAG, "wakeUpBroadcastReceiver.onReceive: We are disconnected from the phone!");
						return;
					}
					Log.d(LOGTAG, "wakeUpBroadcastReceiver.onReceive: Request update!");

					// send message in the background:
					new Thread() {
						public void run() {
							Log.d(LOGTAG, "wakeUpBroadcastReceiver.onReceive: Thread " + this.getName() + " started!");

							// create and connect the googleApiClient:
							GoogleApiClient googleApiClient = WearableApiHelper
									.createAndConnectGoogleApiClient(context, 1000);
							if(googleApiClient == null) {
								Log.e(LOGTAG, "wakeUpBroadcastReceiver.onReceive (Thread="+this.getName()+"): Can't connect the google api client! stop.");
								return;
							}

							// Enumerate nodes to find wearable node:
							Node wearableNode = WearableApiHelper.getOpponentNode(googleApiClient, 1000);
							if(wearableNode == null) {
								Log.e(LOGTAG, "wakeUpBroadcastReceiver.onReceive (Thread="+this.getName()+"): Can't get the wearable node! stop.");
								googleApiClient.disconnect();
								return;
							}

							// send the message:
							if(!WearableApiHelper.sendMessage(googleApiClient, wearableNode.getId(), CommonPaths.REQUEST_UPDATE, null, 1000))
								Log.e(LOGTAG, "wakeUpBroadcastReceiver.onReceive (Thread="+this.getName()+"): Failed to send Message");

							// disconnect the api client:
							googleApiClient.disconnect();

							Log.d(LOGTAG, "wakeUpBroadcastReceiver.onReceive: Thread " + this.getName() + " stopped!");
						}
					}.start();
				} else if(intent.getAction().equals(ACTION_DISMISS_NOTIFICATION)) {
					Log.d(LOGTAG, "wakeUpBroadcastReceiver.onReceive: Received DISMISS_NOTIFICATION action.");
					// make sure the Notification is canceled:
					NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
					notificationManager.cancel(NOTIFICATION_ID);

					// from this point on we are in state 'notification dismissed' and will not allow updates to be shown.
					notificationShowing = false;

                    if(!notificationShowing && !compilationShowing)
                        NetworkNotificationService.this.stopSelf();
				} else if(intent.getAction().equals(ACTION_DISMISS_COMPILATION)) {
					Log.d(LOGTAG, "wakeUpBroadcastReceiver.onReceive: Received DISMISS_COMPILATION action. Stop NetworkNotification service");
					// Also make sure the Notification is canceled:
					NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
					notificationManager.cancel(NOTIFICATION_ID);

					// from this point on we are in state 'compilation dismissed' and will not allow updates to be shown.
					compilationShowing = false;

					if(!notificationShowing && !compilationShowing)
						NetworkNotificationService.this.stopSelf();
				}
			}
		};

		// register the BroadcastReceiver:
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		intentFilter.addAction(Intent.ACTION_DREAMING_STOPPED);
		intentFilter.addAction(ACTION_REQUEST_UPDATE);
		intentFilter.addAction(ACTION_DISMISS_NOTIFICATION);
		intentFilter.addAction(ACTION_DISMISS_COMPILATION);
		registerReceiver(wakeUpBroadcastReceiver, intentFilter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOGTAG, "onDestroy");
		// unregister the receiver
		unregisterReceiver(wakeUpBroadcastReceiver);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(LOGTAG, "onStartCommand: " + (intent == null ? "intent==null" : intent.getAction()));
		Bundle data = null;
		if(intent != null)
			data = intent.getBundleExtra(CommonPaths.CONNECTION_DATA);

		// create a notification depending on the received intent
		if(intent == null || intent.getAction().equals(ACTION_SHOW_NOTIFICATION)) {
			// nothing special with this one
		} else if(intent.getAction().equals(ACTION_NOW_OFFLINE)) {
			connected = false;
		} else if(intent.getAction().equals(ACTION_NOW_ONLINE)) {
			connected = true;
		} else if(intent.getAction().equals(ACTION_SHOW_COMPILATION)) {
			compilationShowing = true;
		}

		// show notification if enabled
		if(sharedPreferences.getBoolean(getString(R.string.pref_showNotifications), true) && intent != null) {
            if(    (intent.getAction().equals(ACTION_NOW_ONLINE) && sharedPreferences.getBoolean(getString(R.string.pref_wearableOnline), true))
            	|| (intent.getAction().equals(ACTION_NOW_OFFLINE) && sharedPreferences.getBoolean(getString(R.string.pref_wearableOffline), true))
				|| intent.getAction().equals(ACTION_SHOW_NOTIFICATION)) {

				updateNotification(this, data, connected, false, sharedPreferences);
            }
        }

        Log.d(LOGTAG, "onStartCommand: calling updateData() on the ComplicationService...");
        NetworkComplicationProviderService.updateData(this, data, connected);

		// Request an update of the network data from the smartphone (only if we are online):
		if(data == null && connected) {
			Intent updateIntent = new Intent(ACTION_REQUEST_UPDATE);
			sendBroadcast(updateIntent);
		}

		return START_STICKY;
	}

	public static void updateData(Context context, Bundle data) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		Log.d(LOGTAG, "updateData: calling updateData() on the ComplicationService...");
		NetworkComplicationProviderService.updateData(context, data, true);

		if(notificationShowing)
			updateNotification(context, data, true, true, sharedPreferences);
	}

	/**
	 * Will show/update the notification with the latest connection data
	 *
	 * @param context		application context
	 * @param data			connection data (must not be null)
	 * @param connected		true if the handheld node is connected; false if no connection to smartphone
	 * @param suppressVibration true if no vibration should be done (e.g. when there is no significant connectivity change)
	 * @param preferences	shared preference instance
	 */
	private static void updateNotification(Context context, Bundle data, boolean connected, boolean suppressVibration, SharedPreferences preferences) {
		boolean showNetworkName = preferences.getBoolean(context.getString(R.string.pref_showNetworkName), true);
		boolean showSignalStrength = preferences.getBoolean(context.getString(R.string.pref_showSignalStrength), true);
		String signalIndicatorPosition = preferences.getString(context.getString(R.string.pref_signalIndicatorPosition), "left");
		int cellularSignalStrengthUnit = Integer.valueOf(preferences.getString(context.getString(R.string.pref_cellularSignalStrengthUnit), "0"));
		int wifiSignalStrengthUnit = Integer.valueOf(preferences.getString(context.getString(R.string.pref_wifiSignalStrengthUnit), "0"));
		boolean vibrate = preferences.getBoolean(context.getString(R.string.pref_vibration), true);
		int iconRes;
		String title;
		final Notification.Builder notificationBuilder = new Notification.Builder(context);
		if(data == null) {
			// we show 'loading' or 'offline':
			iconRes = connected ? R.drawable.ic_launcher : R.drawable.offline;
			title = connected ? context.getString(R.string.loading) : context.getString(R.string.offline);
			notificationBuilder.setSmallIcon(iconRes);
			notificationBuilder.setContentTitle(title);
			notificationBuilder.setCategory(Notification.CATEGORY_STATUS);

			final Notification.WearableExtender wearableExtender = new Notification.WearableExtender()
					.setContentIcon(iconRes)
					.setContentIconGravity(Gravity.END)
					.setHintHideIcon(true);
			notificationBuilder.extend(wearableExtender);

		} else {
			ConnectionData conData = ConnectionData.fromBundle(context, data);
			Log.d(LOGTAG, "updateNotification: " + conData.toString());

			iconRes = conData.getIndicatorIconRes();
			if (iconRes < 0)
				iconRes = R.drawable.ic_launcher;

			// Prepare the large icon:
			largeIconCanvas.drawColor(Color.BLACK);
			Bitmap icon = BitmapFactory.decodeResource(context.getResources(), iconRes);
			largeIconCanvas.drawBitmap(icon, new Rect(0, 0, icon.getWidth(), icon.getHeight()), new Rect(130, 70, 270, 210), paint);

			title = "";
			if (showSignalStrength)
				title += conData.getPrimarySignalStrength(conData.getConnectionState() == ConnectionData.STATE_WIFI ?
						wifiSignalStrengthUnit : cellularSignalStrengthUnit) + " ";
			if (showNetworkName)
				title += conData.getPrimaryNetworkName();
			if (title.length() == 0)
				title = "";

			// Main notification builder
            notificationBuilder.setLargeIcon(largeIcon);
            notificationBuilder.setSmallIcon(iconRes);
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText(conData.getPrimaryNetworkName());
            notificationBuilder.setPriority(Notification.PRIORITY_MAX);
            notificationBuilder.setCategory(Notification.CATEGORY_STATUS);

			// Create a wearable extender for the notification
			final Notification.WearableExtender wearableExtender = new Notification.WearableExtender()
					.setContentIcon(iconRes)
					.setContentIconGravity(signalIndicatorPosition.equals("left") ? Gravity.START : Gravity.END)
					.setHintHideIcon(true);

			// Create a builder for the cellular details page
			ConnectionData.MccMncListItem mccMncListItem = conData.lookupMccMnc();
			String cellularDetails = "Network: <b>" + conData.getCellularNetworkOperator()
					+ "</b><br />MCC: <b>" + conData.getCellularMCC() + "(" + (mccMncListItem != null ? mccMncListItem.getCountry() : "-")
					+ ")</b><br />MNC: <b>" + conData.getCellularMNC() + "(" + (mccMncListItem != null ? mccMncListItem.getNetwork() : "-")
					+ ")</b><br />LAC: <b>" + conData.getCellularLAC()
					+ "</b><br />CID: <b>" + conData.getCellularCID() + "</b>";
			final Notification.Builder cellularDetailsPageBuilder = new Notification.Builder(context)
					.setContentTitle("Mobile Data")
					.setContentText(Html.fromHtml(cellularDetails));

			// Create a builder for the cellular details page
			String frequency = conData.getWifiFrequency() > 0 ? "" + ((double) conData.getWifiFrequency() / 1000.0) + " GHz" : "-";
			String wifiDetails = "SSID: <b>" + conData.getWifiSsid()
					+ "</b><br />BSSID: <b>" + conData.getWifiBssid()
					+ "</b><br />Freq.: <b>" + frequency
					+ "</b><br />Channel: <b>" + conData.getWifiChannel()
					+ "</b><br />Speed: <b>" + conData.getWifiSpeed()
					+ " MBit/s</b><br /s>IP: <b>" + conData.getWifiIp() + "</b>";
			final Notification.Builder wifiDetailsPageBuilder = new Notification.Builder(context)
					.setContentTitle("Wifi Data")
					.setContentText(Html.fromHtml(wifiDetails));

			// Add pages and extend main notification by the extender
			wearableExtender.addPage(cellularDetailsPageBuilder.build());
			wearableExtender.addPage(wifiDetailsPageBuilder.build());
			notificationBuilder.extend(wearableExtender);
		}

		if(vibrate && !suppressVibration)
			notificationBuilder.setVibrate(new long[] {0, 50, 100, 50, 100});

		// Add a dismiss intent to the notification which will be received by the BroadcastReceiver
		Intent deleteIntent = new Intent(ACTION_DISMISS_NOTIFICATION);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);
		notificationBuilder.setDeleteIntent(pendingIntent);

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

		// Build the notification and issues it with notification manager.
		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

		// from this point on we are in state 'notification showing' and will allow updates for notifications
		if(!notificationShowing)
			notificationShowTime = System.currentTimeMillis();
		notificationShowing = true;
	}
}
