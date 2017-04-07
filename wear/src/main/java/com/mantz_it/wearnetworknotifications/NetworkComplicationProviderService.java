package com.mantz_it.wearnetworknotifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import com.mantz_it.common.ConnectionData;

/**
 * Created by dennis on 4/7/17.
 */

public class NetworkComplicationProviderService extends ComplicationProviderService {
    private static final String LOGTAG = "WNNComplicationService";
    private static int debugCounter = 0;
    private static NetworkComplicationProviderService serviceInstance = null;
    private static ComplicationManager complicationManager = null;
    private static int complicationId = -1;

    @Override
    public void onComplicationActivated(int complicationId, int type, ComplicationManager complicationManager) {
        super.onComplicationActivated(complicationId, type, complicationManager);
        NetworkComplicationProviderService.serviceInstance = this;
        NetworkComplicationProviderService.complicationManager = complicationManager;
        NetworkComplicationProviderService.complicationId = complicationId;

        // Intent for tap event (invoke the NetworkNotification service)
        Intent intent = new Intent(serviceInstance, StartActivity.class);
        intent.setAction(NetworkNotificationService.ACTION_SHOW_NOTIFICATION);

        ComplicationData complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText(getString(R.string.loading)))
                .setIcon(Icon.createWithResource(serviceInstance, R.drawable.ic_launcher))
                .setTapAction(PendingIntent.getActivity(serviceInstance, 0, intent, 0))
                .build();

        complicationManager.updateComplicationData(complicationId, complicationData);
    }

    @Override
    public void onComplicationDeactivated(int complicationId) {
        super.onComplicationDeactivated(complicationId);
    }

    @Override
    public void onComplicationUpdate(int complicationId, int dataType, ComplicationManager complicationManager) {
        if(NetworkComplicationProviderService.serviceInstance == null)
            NetworkComplicationProviderService.serviceInstance = this;
        if(NetworkComplicationProviderService.complicationManager == null)
            NetworkComplicationProviderService.complicationManager = complicationManager;
        if(NetworkComplicationProviderService.complicationId < 0)
            NetworkComplicationProviderService.complicationId = complicationId;
        if(NetworkComplicationProviderService.complicationManager != complicationManager)
            Log.w(LOGTAG, "onComplicationUpdate: ComplicationManager has changed!!!!");
        if(NetworkComplicationProviderService.complicationId != complicationId)
            Log.w(LOGTAG, "onComplicationUpdate: complicationId has changed!!!! was " + NetworkComplicationProviderService.complicationId + " and is now " + complicationId);
    }

    public static boolean updateData(Context context, Bundle data) {
        if(serviceInstance == null) {
            Log.i(LOGTAG, "updateData: serviceInstance is null. Do nothing.");
            return false;
        }

        ConnectionData conData = ConnectionData.fromBundle(context, data);
        Log.d(LOGTAG, "updateData: " + conData.toString());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showNetworkName = preferences.getBoolean(context.getString(R.string.pref_showNetworkName), true);
        boolean showSignalStrength = preferences.getBoolean(context.getString(R.string.pref_showSignalStrength), true);
        int cellularSignalStrengthUnit = Integer.valueOf(preferences.getString(context.getString(R.string.pref_cellularSignalStrengthUnit), "0"));
        int wifiSignalStrengthUnit = Integer.valueOf(preferences.getString(context.getString(R.string.pref_wifiSignalStrengthUnit), "0"));

        int iconRes = conData.getIndicatorIconRes();
        if (iconRes < 0)
            iconRes = R.drawable.ic_launcher;

        String title = "";
        if (showSignalStrength)
            title += conData.getPrimarySignalStrength(conData.getConnectionState() == ConnectionData.STATE_WIFI ?
                    wifiSignalStrengthUnit : cellularSignalStrengthUnit) + " ";
        if (showNetworkName)
            title += conData.getPrimaryNetworkName();
        if (title.length() == 0)
            title = "";

        Log.d(LOGTAG, "updateData: called, title=" + title + "  debugCounter=" + debugCounter);

        // Intent for tap event (invoke the NetworkNotification service)
        Intent intent = new Intent(serviceInstance, StartActivity.class);
        intent.setAction(NetworkNotificationService.ACTION_SHOW_NOTIFICATION);

        ComplicationData complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText(title))
                .setIcon(Icon.createWithResource(serviceInstance, iconRes))
                .setTapAction(PendingIntent.getActivity(serviceInstance, 0, intent, 0))
                .build();

        complicationManager.updateComplicationData(complicationId, complicationData);

        debugCounter++;
        return true;
    }
}
