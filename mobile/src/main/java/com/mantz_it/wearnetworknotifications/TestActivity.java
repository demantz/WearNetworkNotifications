package com.mantz_it.wearnetworknotifications;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by dennis on 12/02/15.
 */
public class TestActivity extends Activity {
	TextView tv_output;
	TelephonyManager tm;
	ConnectivityManager cm;
	WifiManager wm;
	MyPhoneStateListener myPhoneStateListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.test_layout);
		tv_output = (TextView) findViewById(R.id.tv_output);
		tv_output.setMovementMethod(new ScrollingMovementMethod());	// make it scroll!
		tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		//myPhoneStateListener = new MyPhoneStateListener();
		//tm.listen(myPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS + PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
		System.out.println("TestActivity created!");
	}

	@Override
	protected void onDestroy() {
		//tm.listen(myPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		super.onDestroy();
	}

	public void bt_loadEventsClicked(View view) {
		loadLog();
	}

	public void bt_deleteEventsClicked(View view) {
		File file = new File(getFilesDir(), "NetworkEvents.txt");
		file.delete();
	}

	public void bt_showInfoClicked(View view) {
		tv_output.append("\n\nCLICK!\n");

		NetworkInfo[] networkInfos = cm.getAllNetworkInfo();
		for(int i = 0; i < 2; i++) {
			printNetworkInfo(networkInfos[i]);
			print("\n");
		}

		print("Network Operator: " + tm.getNetworkOperator());
		print("Network Operator Name: " + tm.getNetworkOperatorName());
		print("Sim Operator Name: " + tm.getSimOperatorName());
		print("Phone Type: " + tm.getPhoneType());
		print("Cell State: " + tm.getCallState());
		print("Network Type: " + tm.getNetworkType());
		print("Data State: " + tm.getDataState());
		CellInfo cellInfo = tm.getAllCellInfo().get(0);
		print("Cell toString: " + cellInfo.toString());
		int dbm = 0;
		int level = 0;
		int asuLevel = 0;
		if(cellInfo instanceof CellInfoGsm) {
			CellInfoGsm cellInfoDetail = (CellInfoGsm) cellInfo;
			print("Cell is GSM!");
			dbm = cellInfoDetail.getCellSignalStrength().getDbm();
			level = cellInfoDetail.getCellSignalStrength().getLevel();
			asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
		}
		if(cellInfo instanceof CellInfoCdma) {
			CellInfoCdma cellInfoDetail = (CellInfoCdma) cellInfo;
			print("Cell is CDMA!");
			dbm = cellInfoDetail.getCellSignalStrength().getDbm();
			level = cellInfoDetail.getCellSignalStrength().getLevel();
			asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
		}
		if(cellInfo instanceof CellInfoLte) {
			CellInfoLte cellInfoDetail = (CellInfoLte) cellInfo;
			print("Cell is LTE!");
			dbm = cellInfoDetail.getCellSignalStrength().getDbm();
			level = cellInfoDetail.getCellSignalStrength().getLevel();
			asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
		}
		if(cellInfo instanceof CellInfoWcdma) {
			CellInfoWcdma cellInfoDetail = (CellInfoWcdma) cellInfo;
			print("Cell is WCDMA!");
			dbm = cellInfoDetail.getCellSignalStrength().getDbm();
			level = cellInfoDetail.getCellSignalStrength().getLevel();
			asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
		}
		print("dBm: " + dbm);
		print("Level: " + level);
		print("ASU Level: " + asuLevel);

		print("\n");
		//print("Frequency: " + wm.getConnectionInfo().getFrequency());
		print("Rssi: " + wm.getConnectionInfo().getRssi());
		print("ToString: " + wm.getConnectionInfo().toString());
	}

	public void print(String msg) {
		tv_output.append(msg + "\n");
	}

	public void printNetworkInfo(NetworkInfo info) {
		print("Type=" + info.getTypeName() + "(" + info.getType() + ")");
		print("SubType=" + info.getSubtypeName() + "(" + info.getSubtype() + ")");
		print("State=" + info.getState().toString());
		print("Reason=" + info.getReason());
		print("Available=" + info.isAvailable());
		print("Connected=" + info.isConnected());
		print("Roaming=" + info.isRoaming());
		print("Failover=" + info.isFailover());
		print("DetailedState=" + info.getDetailedState().toString());
		print("ExtraInfo=" + info.getExtraInfo());
		print("toString=" + info.toString());
	}

	private class MyPhoneStateListener extends PhoneStateListener {
		public MyPhoneStateListener() {
			super();
		}

		@Override
		public void onServiceStateChanged(ServiceState serviceState) {
			System.out.println("onServiceStateChanged: " + serviceState.toString());
		}

		@Override
		public void onMessageWaitingIndicatorChanged(boolean mwi) {
			System.out.println("onMessageWaitingIndicatorChanged: " + mwi);
		}

		@Override
		public void onCallForwardingIndicatorChanged(boolean cfi) {
			System.out.println("onCallForwardingIndicatorChanged: " + cfi);
		}

		@Override
		public void onCellLocationChanged(CellLocation location) {
			System.out.println("onCellLocationChanged: " + location.toString());
		}

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			System.out.println("onCallStateChanged: " + state + " " + incomingNumber);
		}

		@Override
		public void onDataConnectionStateChanged(int state) {
			System.out.println("onDataConnectionStateChanged: " + state);
		}

		@Override
		public void onDataConnectionStateChanged(int state, int networkType) {
			System.out.println("onDataConnectionStateChanged: " + state + " networktype=" + networkType);
		}

		@Override
		public void onDataActivity(int direction) {
			System.out.println("onDataActivity: direction=" + direction);
		}

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			System.out.println("onSignalStrengthsChanged: " + signalStrength.toString());
		}

		@Override
		public void onCellInfoChanged(List<CellInfo> cellInfo) {
			System.out.println("onCellInfoChanged: ");
			for(CellInfo c: cellInfo) {
				int dbm = 0;
				int level = 0;
				int asuLevel = 0;
				if(c instanceof CellInfoGsm) {
					CellInfoGsm cellInfoDetail = (CellInfoGsm) c;
					print("Cell is GSM!");
					dbm = cellInfoDetail.getCellSignalStrength().getDbm();
					level = cellInfoDetail.getCellSignalStrength().getLevel();
					asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
				}
				if(c instanceof CellInfoCdma) {
					CellInfoCdma cellInfoDetail = (CellInfoCdma) c;
					print("Cell is CDMA!");
					dbm = cellInfoDetail.getCellSignalStrength().getDbm();
					level = cellInfoDetail.getCellSignalStrength().getLevel();
					asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
				}
				if(c instanceof CellInfoLte) {
					CellInfoLte cellInfoDetail = (CellInfoLte) c;
					print("Cell is LTE!");
					dbm = cellInfoDetail.getCellSignalStrength().getDbm();
					level = cellInfoDetail.getCellSignalStrength().getLevel();
					asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
				}
				if(c instanceof CellInfoWcdma) {
					CellInfoWcdma cellInfoDetail = (CellInfoWcdma) c;
					print("Cell is WCDMA!");
					dbm = cellInfoDetail.getCellSignalStrength().getDbm();
					level = cellInfoDetail.getCellSignalStrength().getLevel();
					asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
				}
				System.out.println("  dbm="+dbm + " level="+level + " asuLevel="+asuLevel);
			}
		}
	}

	public void loadLog() {
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
			tv_output.setText(log);
			int scrollAmount = tv_output.getLayout().getLineTop(tv_output.getLineCount()) - tv_output.getHeight();
			tv_output.scrollTo(0, scrollAmount);
		} catch (IOException e) {
			Log.e("TestActivity", "loadLog: Couldn't read log: " + e.getMessage());
			return;
		}
	}
}
