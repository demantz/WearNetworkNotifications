package com.mantz_it.wearnetworknotifications;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mantz_it.common.ConnectionData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.mantz_it.wearnetworknotifications.NetworkNotificationService.ACTION_REQUEST_UPDATE;

public class MainActivity extends WearableActivity implements NetworkDataStore.Callback {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private static final String LOGTAG = "MainActivity";

    private ScrollView sv_container;
    private TextView tv_clock;
    private ImageView iv_topImage;
    private TextView tv_wifi;
    private TextView tv_cellular;
    private ProgressBar pb_wifi;
    private ProgressBar pb_cellular;
    private PlotSurface plotSurface;

    private long[] timestamps = null;
    private int[] wifiData = null;
    private int[] cellularData = null;

    private int updateRate = 10000;
    private long lastUpdate = 0;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            Log.d(LOGTAG, "timerRunnable");
            if(System.currentTimeMillis() - lastUpdate > updateRate) {
                Log.d(LOGTAG, "timerRunnable: Request new data!");
                Intent updateIntent = new Intent(ACTION_REQUEST_UPDATE);
                sendBroadcast(updateIntent);
                lastUpdate = System.currentTimeMillis();
            } else {
                if(!isAmbient()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateDisplay();
                        }
                    });
                }
            }
            timerHandler.postDelayed(this, updateRate/10);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        sv_container = (ScrollView) findViewById(R.id.sv_container);
        tv_clock = (TextView) findViewById(R.id.clock);
        iv_topImage = (ImageView) findViewById(R.id.iv_top_image);
        tv_wifi = (TextView) findViewById(R.id.tv_wifi);
        tv_cellular = (TextView) findViewById(R.id.tv_cellular);
        pb_wifi = (ProgressBar) findViewById(R.id.pb_wifi);
        pb_cellular = (ProgressBar) findViewById(R.id.pb_cellular);
        plotSurface = (PlotSurface) findViewById(R.id.plotSurface);
    }

    @Override
    protected void onStart() {
        super.onStart();
        NetworkDataStore.getInstance().setCallback(this);

        // invoke the NetworkNotification service:
        Intent intent = new Intent(this, NetworkNotificationService.class);
        intent.setAction(NetworkNotificationService.ACTION_SHOW_APP);
        startService(intent);

        // start the timer
        timerHandler.postDelayed(timerRunnable, 0);
        Log.d(LOGTAG, "onStart");

        updateDisplay();
    }

    @Override
    protected void onStop() {
        NetworkDataStore.getInstance().setCallback(null);

        // stop the NetworkNotification service:
        Intent updateIntent = new Intent(NetworkNotificationService.ACTION_DISMISS_APP);
        sendBroadcast(updateIntent);

        // stop timer
        timerHandler.removeCallbacks(timerRunnable);
        super.onStop();
        Log.d(LOGTAG, "onStop");
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        plotSurface.setAmbient(true);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        plotSurface.setAmbient(false);
        updateDisplay();
        super.onExitAmbient();
    }

    @Override
    public void onDataUpdated(final NetworkDataStore networkDataStore) {
        Log.d(LOGTAG, "onDataUpdated");

        // update data
        timestamps = new long[networkDataStore.getDataCount()];
        wifiData = new int[networkDataStore.getDataCount()];
        cellularData = new int[networkDataStore.getDataCount()];
        for(int i = 0; i < networkDataStore.getDataCount(); i++) {
            ConnectionData data = networkDataStore.getData(i);
            timestamps[i] = data.getTimestamp();
            wifiData[i] = data.getWifiSignalStrengthPercentage();
            cellularData[i] = data.getCellularSignalStrenghPercentage();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDisplay();
            }
        });
    }

    private void updateDisplay() {
        if (isAmbient()) {
            iv_topImage.setVisibility(View.GONE);
            tv_clock.setVisibility(View.VISIBLE);
            tv_clock.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            tv_clock.setVisibility(View.GONE);
            iv_topImage.setVisibility(View.VISIBLE);
        }

        NetworkDataStore networkDataStore = NetworkDataStore.getInstance();
        ConnectionData data0 = networkDataStore.getData(0);
        if(data0 != null) {
            iv_topImage.setImageResource(data0.getIndicatorIconRes());
            tv_wifi.setText(data0.getWifiSsid());
            tv_cellular.setText(data0.getCellularNetworkOperator());
            pb_wifi.setProgress(data0.getWifiSignalStrengthPercentage());
            pb_cellular.setProgress(data0.getCellularSignalStrenghPercentage());
        }

        if(timestamps != null && wifiData != null && cellularData != null)
            plotSurface.draw(timestamps, wifiData, cellularData);
    }
}
