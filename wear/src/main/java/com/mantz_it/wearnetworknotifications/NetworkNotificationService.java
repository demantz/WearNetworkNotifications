package com.mantz_it.wearnetworknotifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.mantz_it.common.CommonKeys;
import com.mantz_it.common.CommonPaths;
import com.mantz_it.common.WearableApiHelper;

/**
 * Created by dennis on 12/02/15.
 */
public class NetworkNotificationService extends Service {
	private static final String LOGTAG = "NetworkNotificationService";
	public static final String ACTION_SHOW_NOTIFICATION = "com.mantz_it.wearnetworknotifications.ACTION_SHOW_NOTIFICATION";
	public static final String ACTION_NOW_ONLINE = "com.mantz_it.wearnetworknotifications.ACTION_NOW_ONLINE";
	public static final String ACTION_NOW_OFFLINE = "com.mantz_it.wearnetworknotifications.ACTION_NOW_OFFLINE";
	public static final String ACTION_DISMISS_NOTIFICATION = "com.mantz_it.wearnetworknotifications.ACTION_DISMISS_NOTIFICATION";
	public static final String ACTION_REQUEST_UPDATE = "com.mantz_it.wearnetworknotifications.ACTION_REQUEST_UPDATE";
	private static final int NOTIFICATION_ID = 1;
	private static long lastNotificatonTimestamp = 0;
	private static boolean notificationShowing = false;
	private static Bitmap largeIcon = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);	// Large background icon for the notification
	private static Canvas largeIconCanvas = new Canvas(largeIcon);	// Will be used to draw the current icon on the large bitmap
	private static Paint paint = new Paint();						// Will be used to draw the current icon on the large bitmap
	private BroadcastReceiver wakeUpBroadcastReceiver;
	private boolean connected = true;	// true if we have a connection to the phone

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOGTAG, "onCreate");
		wakeUpBroadcastReceiver = new BroadcastReceiver(){
			private long lastUpdateRequestTimestamp = 0;
			@Override
			public void onReceive(final Context context, Intent intent) {
				if(intent.getAction().equals("android.intent.action.SCREEN_ON")
						|| intent.getAction().equals("android.intent.action.DREAMING_STOPPED")
						|| intent.getAction().equals(ACTION_REQUEST_UPDATE)) {

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
					Log.d(LOGTAG, "wakeUpBroadcastReceiver.onReceive: Received DISMISS_NOTIFICATION action. Stop NetworkNotification service");
					// Also make sure the Notification is canceled:
					NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
					notificationManager.cancel(NOTIFICATION_ID);
					NetworkNotificationService.this.stopSelf();
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		intentFilter.addAction(Intent.ACTION_DREAMING_STOPPED);
		intentFilter.addAction(ACTION_REQUEST_UPDATE);
		intentFilter.addAction(ACTION_DISMISS_NOTIFICATION);
		registerReceiver(wakeUpBroadcastReceiver, intentFilter);

		notificationShowing = true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOGTAG, "onDestroy");
		unregisterReceiver(wakeUpBroadcastReceiver);

		notificationShowing = false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(LOGTAG, "onStartCommand: " + intent.getAction());
		Bundle data = intent.getBundleExtra(CommonPaths.CONNECTION_DATA);
		if(intent.getAction().equals(ACTION_SHOW_NOTIFICATION)) {
			// nothing special with this one
		} else if(intent.getAction().equals(ACTION_NOW_OFFLINE)) {
			connected = false;
		} else if(intent.getAction().equals(ACTION_NOW_ONLINE)) {
			connected = true;
		}
		createNotification(this, data, connected, true);

		return START_STICKY;
	}

	public static void createNotification(Context context, Bundle data, boolean connected, boolean vibrate) {
//		long currentTime = System.currentTimeMillis();
//		if(currentTime - lastNotificatonTimestamp < 5000) {
//			Log.d(LOGTAG, "createNotification: Last notification was less than five second ago!");
//			return;
//		}
//		lastNotificatonTimestamp = currentTime;

		if(data != null)
			updateNotification(context, data, vibrate);
		else {
			int iconRes = connected ? R.drawable.ic_launcher : R.drawable.offline;
			String title = connected ? "loading..." : "offline";
			final Notification.Builder notificationBuilder = new Notification.Builder(context)
					.setSmallIcon(iconRes)		// you have to include this
					.setContentTitle(title)
					.setCategory(Notification.CATEGORY_STATUS);
			final Notification.WearableExtender wearableExtender = new Notification.WearableExtender()
					.setContentIcon(iconRes)
					.setContentIconGravity(Gravity.END)
					.setHintHideIcon(true);
			notificationBuilder.extend(wearableExtender);

			if(vibrate)
				notificationBuilder.setVibrate(new long[] {0, 50, 100, 50, 100});

			Intent deleteIntent = new Intent(ACTION_DISMISS_NOTIFICATION);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);
			notificationBuilder.setDeleteIntent(pendingIntent);

			NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

			// Build the notification and issues it with notification manager.
			notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

			// Request an update of the network data (only if we are online):
			if(connected) {
				Intent intent = new Intent(ACTION_REQUEST_UPDATE);
				context.sendBroadcast(intent);
			}
		}
	}

	public static void updateNotification(Context context, Bundle data, boolean vibrate) {
		if(!notificationShowing) {
			Log.d(LOGTAG, "updateNotification: Notification is not visible. Don't update!");
			return;
		}
		boolean wifiConnected = data.getInt(CommonKeys.WIFI_SPEED) > 0;
		int iconRes = getIndicatorIconRes(wifiConnected, data.getInt(CommonKeys.WIFI_RSSI),
				data.getInt(CommonKeys.CELLULAR_NETWORK_TYPE), data.getInt(CommonKeys.CELLULAR_ASU_LEVEL));
		if(iconRes < 0)
			iconRes = R.drawable.ic_launcher;
		String title;
		if(wifiConnected)
			title = data.getInt(CommonKeys.WIFI_RSSI) + " " + data.getString(CommonKeys.WIFI_SSID).replace("\"", "");
		else {
			if(data.getInt(CommonKeys.CELLULAR_DBM) == Integer.MIN_VALUE)
				title = "no service";
			else
				title = data.getInt(CommonKeys.CELLULAR_DBM) + " " + data.getString(CommonKeys.CELLULAR_NETWORK_OPERATOR);
		}
		String logMessage = "";
		logMessage += CommonKeys.WIFI_SSID + "=" + data.getString(CommonKeys.WIFI_SSID) + "  ";
		logMessage += CommonKeys.WIFI_RSSI + "=" + data.getInt(CommonKeys.WIFI_RSSI) + "  ";
		logMessage += CommonKeys.WIFI_SPEED + "=" + data.getInt(CommonKeys.WIFI_SPEED) + "  ";
		logMessage += CommonKeys.CELLULAR_NETWORK_OPERATOR + "=" + data.getString(CommonKeys.CELLULAR_NETWORK_OPERATOR) + "  ";
		logMessage += CommonKeys.CELLULAR_NETWORK_TYPE + "=" + data.getInt(CommonKeys.CELLULAR_NETWORK_TYPE) + "  ";
		logMessage += CommonKeys.CELLULAR_DBM + "=" + data.getInt(CommonKeys.CELLULAR_DBM) + "  ";
		logMessage += CommonKeys.CELLULAR_ASU_LEVEL + "=" + data.getInt(CommonKeys.CELLULAR_ASU_LEVEL);
		Log.d(LOGTAG, "updateNotification: " + logMessage);

		// Prepare the large icon:
		largeIconCanvas.drawColor(Color.BLACK);
		Bitmap icon = BitmapFactory.decodeResource(context.getResources(), iconRes);
		largeIconCanvas.drawBitmap(icon, new Rect(0, 0, icon.getWidth(), icon.getHeight()), new Rect(130, 70, 270, 210), paint);

		final Notification.Builder notificationBuilder = new Notification.Builder(context)
				.setLargeIcon(largeIcon)
				.setSmallIcon(iconRes)		// you have to include this
				.setContentTitle(title)
				//.setContentText(logMessage)	// DEBUG
				.setPriority(2)
				.setCategory(Notification.CATEGORY_STATUS);
		if(vibrate)
			notificationBuilder.setVibrate(new long[] {0, 50, 100, 50, 100});
		final Notification.WearableExtender wearableExtender = new Notification.WearableExtender()
				.setContentIcon(iconRes)
				.setContentIconGravity(Gravity.END)
				.setHintHideIcon(true);
		notificationBuilder.extend(wearableExtender);

		Intent deleteIntent = new Intent(ACTION_DISMISS_NOTIFICATION);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);
		notificationBuilder.setDeleteIntent(pendingIntent);

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

		// Build the notification and issues it with notification manager.
		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
	}

	public static int getIndicatorIconRes(boolean wifiConnected, int wifiRssi, int networkType, int asuLevel) {
		if(wifiConnected) {
			int cellularStrength = getCellularSignalStrengh(networkType, asuLevel);
			switch (getWifiSignalStrength(wifiRssi)) {
				case 0:	//todo
				case 1:
					switch (cellularStrength) {
						case 0:		return R.drawable.wifi_1_cellular_no_signal;
						case 1:		return R.drawable.wifi_1_cellular_1;
						case 2:		return R.drawable.wifi_1_cellular_2;
						case 3:		return R.drawable.wifi_1_cellular_3;
						case 4:		return R.drawable.wifi_1_cellular_4;
						case 5:		return R.drawable.wifi_1_cellular_5;
						default:	return -1;
					}
				case 2:
					switch (cellularStrength) {
						case 0:		return R.drawable.wifi_2_cellular_no_signal;
						case 1:		return R.drawable.wifi_2_cellular_1;
						case 2:		return R.drawable.wifi_2_cellular_2;
						case 3:		return R.drawable.wifi_2_cellular_3;
						case 4:		return R.drawable.wifi_2_cellular_4;
						case 5:		return R.drawable.wifi_2_cellular_5;
						default:	return -1;
					}
				case 3:
					switch (cellularStrength) {
						case 0:		return R.drawable.wifi_3_cellular_no_signal;
						case 1:		return R.drawable.wifi_3_cellular_1;
						case 2:		return R.drawable.wifi_3_cellular_2;
						case 3:		return R.drawable.wifi_3_cellular_3;
						case 4:		return R.drawable.wifi_3_cellular_4;
						case 5:		return R.drawable.wifi_3_cellular_5;
						default:	return -1;
					}
				case 4:
					switch (cellularStrength) {
						case 0:		return R.drawable.wifi_4_cellular_no_signal;
						case 1:		return R.drawable.wifi_4_cellular_1;
						case 2:		return R.drawable.wifi_4_cellular_2;
						case 3:		return R.drawable.wifi_4_cellular_3;
						case 4:		return R.drawable.wifi_4_cellular_4;
						case 5:		return R.drawable.wifi_4_cellular_5;
						default:	return -1;
					}
				case 5:
					switch (cellularStrength) {
						case 0:		return R.drawable.wifi_5_cellular_no_signal;
						case 1:		return R.drawable.wifi_5_cellular_1;
						case 2:		return R.drawable.wifi_5_cellular_2;
						case 3:		return R.drawable.wifi_5_cellular_3;
						case 4:		return R.drawable.wifi_5_cellular_4;
						case 5:		return R.drawable.wifi_5_cellular_5;
						default:	return -1;
					}
				default:	return -1;
			}
		} else {
			switch (networkType) {
				case 0:	// GSM
					switch (getCellularSignalStrengh(networkType, asuLevel)) {
						case 0:		return R.drawable.cellular_no_signal;
						case 1:		return R.drawable.cellular_1;
						case 2:		return R.drawable.cellular_2;
						case 3:		return R.drawable.cellular_3;
						case 4:		return R.drawable.cellular_4;
						case 5:		return R.drawable.cellular_5;
						default:	return -1;
					}
				case TelephonyManager.NETWORK_TYPE_GPRS:
					switch (getCellularSignalStrengh(networkType, asuLevel)) {
						case 0:		return R.drawable.cellular_no_signal;
						case 1:		return R.drawable.cellular_1_g;
						case 2:		return R.drawable.cellular_2_g;
						case 3:		return R.drawable.cellular_3_g;
						case 4:		return R.drawable.cellular_4_g;
						case 5:		return R.drawable.cellular_5_g;
						default:	return -1;
					}
				case TelephonyManager.NETWORK_TYPE_EDGE:
					switch (getCellularSignalStrengh(networkType, asuLevel)) {
						case 0:		return R.drawable.cellular_no_signal;
						case 1:		return R.drawable.cellular_1_e;
						case 2:		return R.drawable.cellular_2_e;
						case 3:		return R.drawable.cellular_3_e;
						case 4:		return R.drawable.cellular_4_e;
						case 5:		return R.drawable.cellular_5_e;
						default:	return -1;
					}
				case TelephonyManager.NETWORK_TYPE_HSDPA:
				case TelephonyManager.NETWORK_TYPE_HSPA:
				case TelephonyManager.NETWORK_TYPE_HSPAP:
				case TelephonyManager.NETWORK_TYPE_HSUPA:
				case TelephonyManager.NETWORK_TYPE_UMTS:
					switch (getCellularSignalStrengh(networkType, asuLevel)) {
						case 0:		return R.drawable.cellular_no_signal;
						case 1:		return R.drawable.cellular_1_h;
						case 2:		return R.drawable.cellular_2_h;
						case 3:		return R.drawable.cellular_3_h;
						case 4:		return R.drawable.cellular_4_h;
						case 5:		return R.drawable.cellular_5_h;
						default:	return -1;
					}
				case TelephonyManager.NETWORK_TYPE_LTE:
					switch (getCellularSignalStrengh(networkType, asuLevel)) {
						case 0:		return R.drawable.cellular_no_signal;
						case 1:		return R.drawable.cellular_1_lte;
						case 2:		return R.drawable.cellular_2_lte;
						case 3:		return R.drawable.cellular_3_lte;
						case 4:		return R.drawable.cellular_4_lte;
						case 5:		return R.drawable.cellular_5_lte;
						default:	return -1;
					}
				default:
					return -1;
			}
		}
	}

	public static int getCellularSignalStrengh(int networkType, int asuLevel) {
		// http://www.lte-anbieter.info/technik/asu.php
		switch (networkType) {
			case 0:	// GSM
			case TelephonyManager.NETWORK_TYPE_GPRS:
			case TelephonyManager.NETWORK_TYPE_EDGE:
				if(asuLevel < 1)
					return 0;
				if(asuLevel < 6)
					return 1;
				if(asuLevel < 11)
					return 2;
				if(asuLevel < 16)
					return 3;
				if(asuLevel < 27)
					return 4;
				return 5;
			case TelephonyManager.NETWORK_TYPE_HSDPA:
			case TelephonyManager.NETWORK_TYPE_HSPA:
			case TelephonyManager.NETWORK_TYPE_HSPAP:
			case TelephonyManager.NETWORK_TYPE_HSUPA:
			case TelephonyManager.NETWORK_TYPE_UMTS:
				if(asuLevel < 1)
					return 0;
				if(asuLevel < 6)
					return 1;
				if(asuLevel < 11)
					return 2;
				if(asuLevel < 16)
					return 3;
				if(asuLevel < 24)
					return 4;
				return 5;
			case TelephonyManager.NETWORK_TYPE_LTE:
				if(asuLevel < 10)
					return 0;
				if(asuLevel < 15)
					return 1;
				if(asuLevel < 30)
					return 2;
				if(asuLevel < 47)
					return 3;
				if(asuLevel < 71)
					return 4;
				return 5;
		}
		Log.e(LOGTAG, "getCellularSignalStrengh: unknown network type: " + networkType);
		return -1;
	}

	public static int getWifiSignalStrength(int rssi) {
		if(rssi < -100)
			return 0;
		if(rssi < -80)
			return 1;
		if(rssi < -68)
			return 2;
		if(rssi < -56)
			return 3;
		if(rssi < -50)
			return 4;
		return 5;
	}
}
