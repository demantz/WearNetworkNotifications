package com.mantz_it.common;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConnectionData {
	private static final String LOGTAG = "ConnectionData";
	public static final String STATE = "STATE";
	public static final String WIFI_BSSID = "WIFI_BSSID";
	public static final String WIFI_SSID = "WIFI_SSID";
	public static final String WIFI_RSSI = "WIFI_RSSI";
	public static final String WIFI_FREQUENCY = "WIFI_FREQUENCY";
	public static final String WIFI_IP = "WIFI_IP";
	public static final String WIFI_SPEED = "WIFI_SPEED";
	public static final String WIFI_RSSI_FROM_SCAN_RESULT = "WIFI_RSSI_FROM_SCAN_RESULT";
	public static final String CELLULAR_NETWORK_OPERATOR = "CELLULAR_NETWORK_OPERATOR";
	public static final String CELLULAR_NETWORK_TYPE = "CELLULAR_NETWORK_TYPE";
	public static final String CELLULAR_MCC = "CELLULAR_MCC";
	public static final String CELLULAR_MNC = "CELLULAR_MNC";
	public static final String CELLULAR_LAC = "CELLULAR_LAC";
	public static final String CELLULAR_CID = "CELLULAR_CID";
	public static final String CELLULAR_DBM = "CELLULAR_DBM";
	public static final String CELLULAR_ASU_LEVEL = "CELLULAR_ASU_LEVEL";
	public static final String TIMESTAMP = "TIMESTAMP";

	public static final int UNIT_PERCENT 	= 0;
	public static final int UNIT_DBM 		= 1;
	public static final int UNIT_RSSI 		= 2;
	public static final int UNIT_ASULEVEL 	= 2;
	public static final int STATE_INVALID	= 0;
	public static final int STATE_OFFLINE	= 1;
	public static final int STATE_MOBILE	= 2;
	public static final int STATE_WIFI		= 3;

	private Context context;
	private int state;
	private String wifiBssid;
	private String wifiSsid;
	private int wifiRssi;
	private int wifiFrequency;
	private String wifiIP;
	private boolean wifiRssiFromScanResult;
	private int wifiSpeed;
	private String cellularNetworkOperator;
	private int cellularNetworkType;
	private int cellularMCC;
	private int cellularMNC;
	private int cellularLAC;
	private int cellularCID;
	private int cellularDBm;
	private int cellularAsuLevel;
	private long timestamp;

	private static ArrayList<MccMncListItem> mccMncList = null;		// List of mcc's and mnc's and their corresponding countries and networks


	public static ConnectionData gatherConnectionData(Context context) {
		Bundle data = new Bundle();

		// Wifi data
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		String ssid = wm.getConnectionInfo().getSSID();
		String bssid = wm.getConnectionInfo().getBSSID();
		int frequency = - 1;
		if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			frequency = wm.getConnectionInfo().getFrequency();
		String wifiIP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
		int rssi = wm.getConnectionInfo().getRssi();
		boolean rssiFromScanResult = false;
		int speed = wm.getConnectionInfo().getLinkSpeed();

		// A little work-around.
		// Problem: After the phone connects to a wifi network while the screen is turned off,
		// the ConnectionInfo will report speed==-1 and rssi==-127 until the screen is turned on
		// once (Nexus 5 with 5.0.1)
		// Work-around: If this bug happens we get the rssi from the last scan results:
		if(speed < 0 && ssid != null && ssid.length() > 0) {
			List<ScanResult> scanResultList = wm.getScanResults();
			if(scanResultList != null) {
				for(ScanResult result: scanResultList) {
					if(result.BSSID.equals(bssid)) {
						rssi = result.level;
						Log.d(LOGTAG, "gatherConnectionData: SSID is set but Linkspeed is -1 and ScanResult Level is:"
								+ result.level);
						rssiFromScanResult = true;
					}
				}
				if(!rssiFromScanResult)
					Log.d(LOGTAG, "gatherConnectionData: SSID is set but Linkspeed is -1 and ScanResults don't contain BSSID ("
							+ wm.getConnectionInfo().getBSSID() + ")!");
			} else {
				Log.d(LOGTAG, "gatherConnectionData: SSID is set but Linkspeed is -1 and ScanResults are null!");
			}
		}
		data.putString(WIFI_BSSID, bssid);
		data.putString(WIFI_SSID, ssid);
		data.putInt(WIFI_RSSI, rssi);
		data.putInt(WIFI_FREQUENCY, frequency);
		data.putString(WIFI_IP, wifiIP);
		data.putBoolean(WIFI_RSSI_FROM_SCAN_RESULT, rssiFromScanResult);
		data.putInt(WIFI_SPEED, speed);

		// Cellular data
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		data.putString(CELLULAR_NETWORK_OPERATOR, tm.getNetworkOperatorName());
		data.putInt(CELLULAR_NETWORK_TYPE, tm.getNetworkType());
		// Get the signal strength by looking at the first connected cell (if it exists):
		int dbm = Integer.MIN_VALUE;
		int asuLevel = Integer.MIN_VALUE;
		int mcc = -1;
		int mnc = -1;
		int lac = -1;
		int cid = -1;
		if(tm.getAllCellInfo() != null && !tm.getAllCellInfo().isEmpty()) {
			CellInfo cellInfo = tm.getAllCellInfo().get(0);
			if (cellInfo instanceof CellInfoGsm) {
				CellInfoGsm cellInfoDetail = (CellInfoGsm) cellInfo;
				dbm = cellInfoDetail.getCellSignalStrength().getDbm();
				asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
				mcc = cellInfoDetail.getCellIdentity().getMcc();
				mnc = cellInfoDetail.getCellIdentity().getMnc();
				lac = cellInfoDetail.getCellIdentity().getLac();
				cid = cellInfoDetail.getCellIdentity().getCid();
			} else if (cellInfo instanceof CellInfoCdma) {
				CellInfoCdma cellInfoDetail = (CellInfoCdma) cellInfo;
				dbm = cellInfoDetail.getCellSignalStrength().getDbm();
				asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
			} else if (cellInfo instanceof CellInfoLte) {
				CellInfoLte cellInfoDetail = (CellInfoLte) cellInfo;
				dbm = cellInfoDetail.getCellSignalStrength().getDbm();
				asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
				mcc = cellInfoDetail.getCellIdentity().getMcc();
				mnc = cellInfoDetail.getCellIdentity().getMnc();
			} else if (cellInfo instanceof CellInfoWcdma) {
				CellInfoWcdma cellInfoDetail = (CellInfoWcdma) cellInfo;
				dbm = cellInfoDetail.getCellSignalStrength().getDbm();
				asuLevel = cellInfoDetail.getCellSignalStrength().getAsuLevel();
				mcc = cellInfoDetail.getCellIdentity().getMcc();
				mnc = cellInfoDetail.getCellIdentity().getMnc();
				lac = cellInfoDetail.getCellIdentity().getLac();
				cid = cellInfoDetail.getCellIdentity().getCid();
			}
		}
		data.putInt(CELLULAR_DBM, dbm);
		data.putInt(CELLULAR_ASU_LEVEL, asuLevel);
		data.putInt(CELLULAR_MCC, mcc);
		data.putInt(CELLULAR_MNC, mnc);
		data.putInt(CELLULAR_LAC, lac);
		data.putInt(CELLULAR_CID, cid);

		// Basic connectivity data:
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		int state = STATE_INVALID;
		if(networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
				&& networkInfo.isAvailable() && networkInfo.isConnected()) {
			state = STATE_WIFI;
		} else if(networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE
				&& networkInfo.isAvailable() && networkInfo.isConnected() && asuLevel >= 0
				&& tm.getNetworkOperatorName().length() > 0 && tm.getNetworkType() != 0) {
			state = STATE_MOBILE;
		} else {
			state = STATE_OFFLINE;
		}
		data.putInt(STATE, state);

		// also add a timestamp:
		data.putLong(TIMESTAMP, System.currentTimeMillis());

		return new ConnectionData(context, data);
	}

	public static ConnectionData fromBundle(Context context, Bundle data) {
		return new ConnectionData(context, data);
	}

	public Bundle toBundle() {
		Bundle bundle = new Bundle();
		bundle.putInt(STATE, state);
		bundle.putString(WIFI_BSSID, wifiBssid);
		bundle.putString(WIFI_SSID, wifiSsid);
		bundle.putInt(WIFI_RSSI, wifiRssi);
		bundle.putInt(WIFI_FREQUENCY, wifiFrequency);
		bundle.putString(WIFI_IP, wifiIP);
		bundle.putBoolean(WIFI_RSSI_FROM_SCAN_RESULT, wifiRssiFromScanResult);
		bundle.putInt(WIFI_SPEED, wifiSpeed);
		bundle.putString(CELLULAR_NETWORK_OPERATOR, cellularNetworkOperator);
		bundle.putInt(CELLULAR_NETWORK_TYPE, cellularNetworkType);
		bundle.putInt(CELLULAR_MCC, cellularMCC);
		bundle.putInt(CELLULAR_MNC, cellularMNC);
		bundle.putInt(CELLULAR_LAC, cellularLAC);
		bundle.putInt(CELLULAR_CID, cellularCID);
		bundle.putInt(CELLULAR_DBM, cellularDBm);
		bundle.putInt(CELLULAR_ASU_LEVEL, cellularAsuLevel);
		bundle.putLong(TIMESTAMP, timestamp);
		return bundle;
	}

	private ConnectionData(Context context, Bundle connectionData) {
		this.context = context;
		state = connectionData.getInt(STATE);
		wifiBssid = connectionData.getString(WIFI_BSSID);
		wifiSsid = connectionData.getString(WIFI_SSID);
		wifiRssi = connectionData.getInt(WIFI_RSSI);
		wifiFrequency = connectionData.getInt(WIFI_FREQUENCY);
		wifiIP = connectionData.getString(WIFI_IP);
		wifiRssiFromScanResult = connectionData.getBoolean(WIFI_RSSI_FROM_SCAN_RESULT);
		wifiSpeed = connectionData.getInt(WIFI_SPEED);
		cellularNetworkOperator = connectionData.getString(CELLULAR_NETWORK_OPERATOR);
		cellularNetworkType = connectionData.getInt(CELLULAR_NETWORK_TYPE);
		cellularMCC = connectionData.getInt(CELLULAR_MCC);
		cellularMNC = connectionData.getInt(CELLULAR_MNC);
		cellularLAC = connectionData.getInt(CELLULAR_LAC);
		cellularCID = connectionData.getInt(CELLULAR_CID);
		cellularDBm = connectionData.getInt(CELLULAR_DBM);
		cellularAsuLevel = connectionData.getInt(CELLULAR_ASU_LEVEL);
		timestamp = connectionData.getLong(TIMESTAMP);
	}

	public String getWifiBssid() {
		return wifiBssid.replace("\"", "");
	}

	public String getWifiSsid() {
		return wifiSsid.replace("\"", "");
	}

	public int getWifiRssi() {
		return wifiRssi;
	}

	public int getWifiFrequency() {
		return wifiFrequency;
	}

	public int getWifiChannel() {
		switch (wifiFrequency) {
			case 2412:	return 1;
			case 2417:	return 2;
			case 2422:	return 3;
			case 2427:	return 4;
			case 2432:	return 5;
			case 2437:	return 6;
			case 2442:	return 7;
			case 2447:	return 8;
			case 2452:	return 9;
			case 2457:	return 10;
			case 2462:	return 11;
			case 2467:	return 12;
			case 2472:	return 13;
			case 2484:	return 14;
			// TODO: Add 5Ghz channels!
			default: return -1;
		}
	}

	public String getWifiIp() {
		return wifiIP;
	}

	public boolean isWifiRssiFromScanResult() {
		return wifiRssiFromScanResult;
	}

	public int getWifiSpeed() {
		return wifiSpeed;
	}

	public String getCellularNetworkOperator() {
		return cellularNetworkOperator;
	}

	public int getCellularNetworkType() {
		return cellularNetworkType;
	}

	public int getCellularMCC() {
		return cellularMCC;
	}

	public int getCellularMNC() {
		return cellularMNC;
	}

	public int getCellularLAC() {
		return cellularLAC;
	}

	public int getCellularCID() {
		return cellularCID;
	}

	public int getCellularDBm() {
		return cellularDBm;
	}

	public int getCellularAsuLevel() {
		return cellularAsuLevel;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getConnectionState() {
		return state;
	}

	public static String getConnectionStateName(int connectionState) {
		final String[] STATE_NAMES = {"INVALID", "OFFLINE", "MOBILE", "WIFI"};
		return STATE_NAMES[connectionState];
	}

	public String getConnectionStateName() {
		return getConnectionStateName(getConnectionState());
	}

	public String getPrimaryNetworkName() {
		switch (state) {
			case STATE_WIFI:
				return getWifiSsid();
			case STATE_MOBILE:
				return getCellularNetworkOperator();
			case STATE_OFFLINE:
				if(cellularAsuLevel < 0)
					return context.getString(R.string.noService);
				else
					return context.getString(R.string.emergencyOnly);
			default:
				return "";
		}
	}

	public String getPrimarySignalStrength(int unit) {
		if(state == STATE_WIFI) {
			if(unit == UNIT_PERCENT)
				return "" + getWifiSignalStrengthPercentage();
			else if(unit == UNIT_DBM)
				return "dBm";		// TODO replace with actual calculation
			else if(unit == UNIT_RSSI)
				return "" + wifiRssi;
		}
		else {
			if(cellularAsuLevel <= 0) {
				return "";
			} else {
				if(unit == UNIT_PERCENT)
					return "" + getCellularSignalStrenghPercentage();
				else if(unit == UNIT_DBM)
					return "" + cellularDBm;
				else if(unit == UNIT_ASULEVEL)
					return "" + cellularAsuLevel;
			}
		}
		Log.e(LOGTAG, "getPrimarySignalStrength: Invalid unit: " + unit);
		return null;
	}

	public String toString() {
		String str = "";
		str += TIMESTAMP + "=" + (new Date(timestamp)).toString() + " ";
		str += STATE + "=" + getConnectionStateName() + " ";
		str += WIFI_SSID + "=" + ((wifiSsid == null || wifiSsid.length() == 0) ? "''" : wifiSsid) + "  ";
		str += WIFI_RSSI + "=" + wifiRssi + (wifiRssiFromScanResult ? "*" : "") + "  ";
		str += WIFI_SPEED + "=" + wifiSpeed + "  ";
		str += CELLULAR_NETWORK_OPERATOR + "=" + (cellularNetworkOperator.length() == 0 ? "''" : cellularNetworkOperator) + "  ";
		str += CELLULAR_NETWORK_TYPE + "=" + cellularNetworkType + "  ";
		str += CELLULAR_DBM + "=" + cellularDBm + "  ";
		str += CELLULAR_ASU_LEVEL + "=" + cellularAsuLevel;
		return str;
	}

	/**
	 * Will return the icon resource that fits to the connection situation
	 *
	 * @return icon resource or -1 on error
	 */
	public int getIndicatorIconRes() {
		if(state == STATE_WIFI) {
			int cellularStrength = getCellularSignalStrengh();
			switch (getWifiSignalStrength()) {
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
			int cellularSignalStrength = getCellularSignalStrengh();
			switch (cellularNetworkType) {
				case 0:	// GSM
					switch (cellularSignalStrength) {
						case 0:		return R.drawable.cellular_no_signal;
						case 1:		return R.drawable.cellular_1;
						case 2:		return R.drawable.cellular_2;
						case 3:		return R.drawable.cellular_3;
						case 4:		return R.drawable.cellular_4;
						case 5:		return R.drawable.cellular_5;
						default:	return -1;
					}
				case TelephonyManager.NETWORK_TYPE_GPRS:
					switch (cellularSignalStrength) {
						case 0:		return R.drawable.cellular_no_signal;
						case 1:		return R.drawable.cellular_1_g;
						case 2:		return R.drawable.cellular_2_g;
						case 3:		return R.drawable.cellular_3_g;
						case 4:		return R.drawable.cellular_4_g;
						case 5:		return R.drawable.cellular_5_g;
						default:	return -1;
					}
				case TelephonyManager.NETWORK_TYPE_EDGE:
					switch (cellularSignalStrength) {
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
					switch (cellularSignalStrength) {
						case 0:		return R.drawable.cellular_no_signal;
						case 1:		return R.drawable.cellular_1_h;
						case 2:		return R.drawable.cellular_2_h;
						case 3:		return R.drawable.cellular_3_h;
						case 4:		return R.drawable.cellular_4_h;
						case 5:		return R.drawable.cellular_5_h;
						default:	return -1;
					}
				case TelephonyManager.NETWORK_TYPE_LTE:
					switch (cellularSignalStrength) {
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

	/**
	 * Will return the signal strength of the cellular signal on a scale from 0 (no signal) to 5 (strong signal)
	 *
	 * @return signal strength (0 - 5)
	 */
	public int getCellularSignalStrengh() {
		// http://www.lte-anbieter.info/technik/asu.php
		switch (cellularNetworkType) {
			case 0:	// GSM
			case TelephonyManager.NETWORK_TYPE_GPRS:
			case TelephonyManager.NETWORK_TYPE_EDGE:
				if(cellularAsuLevel < 1)
					return 0;
				if(cellularAsuLevel < 6)
					return 1;
				if(cellularAsuLevel < 11)
					return 2;
				if(cellularAsuLevel < 16)
					return 3;
				if(cellularAsuLevel < 27)
					return 4;
				return 5;
			case TelephonyManager.NETWORK_TYPE_HSDPA:
			case TelephonyManager.NETWORK_TYPE_HSPA:
			case TelephonyManager.NETWORK_TYPE_HSPAP:
			case TelephonyManager.NETWORK_TYPE_HSUPA:
			case TelephonyManager.NETWORK_TYPE_UMTS:
				if(cellularAsuLevel < 1)
					return 0;
				if(cellularAsuLevel < 6)
					return 1;
				if(cellularAsuLevel < 11)
					return 2;
				if(cellularAsuLevel < 16)
					return 3;
				if(cellularAsuLevel < 24)
					return 4;
				return 5;
			case TelephonyManager.NETWORK_TYPE_LTE:
				if(cellularAsuLevel < 10)
					return 0;
				if(cellularAsuLevel < 15)
					return 1;
				if(cellularAsuLevel < 30)
					return 2;
				if(cellularAsuLevel < 47)
					return 3;
				if(cellularAsuLevel < 71)
					return 4;
				return 5;
		}
		Log.e(LOGTAG, "getCellularSignalStrengh: unknown network type: " + cellularNetworkType);
		return -1;
	}

	/**
	 * Will return the signal strength of the wifi signal on a scale from 0 (no signal) to 5 (strong signal)
	 *
	 * @return signal strength (0 - 5)
	 */
	public int getWifiSignalStrength() {
		if(wifiRssi < -100)
			return 0;
		if(wifiRssi < -80)
			return 1;
		if(wifiRssi < -68)
			return 2;
		if(wifiRssi < -56)
			return 3;
		if(wifiRssi < -50)
			return 4;
		return 5;
	}

	/**
	 * Will convert the RSSI value to percent.
	 *    -100 or lower will convert to 0%
	 *    -40 will convert to 100%
	 *    higher values will result in a percentage greater 100
	 *
	 * @return signal strength in percent
	 */
	public int getWifiSignalStrengthPercentage() {
		int percent = (int)((wifiRssi + 100) / 60f * 100);
		if(percent < 0)
			percent = 0;
		return percent;
	}

	/**
	 * Will convert the ASU value to percent.
	 *
	 * @return signal strength in percent
	 */
	public int getCellularSignalStrenghPercentage() {
		// http://www.lte-anbieter.info/technik/asu.php
		switch (cellularNetworkType) {
			case 0:	// GSM
			case TelephonyManager.NETWORK_TYPE_GPRS:
			case TelephonyManager.NETWORK_TYPE_EDGE:
			case TelephonyManager.NETWORK_TYPE_HSDPA:
			case TelephonyManager.NETWORK_TYPE_HSPA:
			case TelephonyManager.NETWORK_TYPE_HSPAP:
			case TelephonyManager.NETWORK_TYPE_HSUPA:
			case TelephonyManager.NETWORK_TYPE_UMTS:
				return (int)(cellularAsuLevel / 32f * 100);
			case TelephonyManager.NETWORK_TYPE_LTE:
				return (int)(cellularAsuLevel / 95f * 100);
		}
		Log.e(LOGTAG, "getCellularSignalStrenghPercentage: unknown network type: " + cellularNetworkType);
		return -1;
	}


	/**
	 * Loads the mcc-mnc list from the resources
	 *
	 * List format has to be:
	 * MCC,MNC,CC,Country,Network
	 * @param context 		context to access the resources
	 * @return number of list entries loaded or -1 on error
	 */
	private static int loadMccMncList(Context context) {
		int counter = -1;

		InputStream inputStream = context.getResources().openRawResource(R.raw.mcc_mnc);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		ArrayList<MccMncListItem> list = new ArrayList<>();
		try {
			String line = reader.readLine();
			Log.d(LOGTAG, "loadMccMncList(): load list with header: " + line);
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(",");
				if(values.length < 4)
					continue;
				int mcc;
				int mnc;
				try {
					mcc = Integer.valueOf(values[0]);
					mnc = Integer.valueOf(values[1]);
				} catch (NumberFormatException e) {
					Log.w(LOGTAG, "loadMccMncList(): Invalid number format: " + e.getMessage() + " - skip line!");
					continue;
				}
				String cc = values[2];
				String country = values[3];
				String network = "-";
				if(values.length >= 5)
					network = values[4];
				list.add(new MccMncListItem(mcc, mnc, cc, country, network));
			}
		} catch (IOException e) {
			Log.e(LOGTAG, "loadMccMncList(): " + e.getMessage());
			return -1;
		}

		mccMncList = list;

		return counter;
	}

	/**
	 * Returns the list item  that matches the mcc and mnc. The list item contains
	 * information about the country and the network provider
	 *
	 * @param context 		context to access resources
	 * @param mcc			mobile country code
	 * @param mnc 			mobile network code
	 *
	 * @return Matching list item or null if not found
	 */
	public static MccMncListItem lookupMccMnc(Context context, int mcc, int mnc) {
		if(mccMncList == null) {
			if (loadMccMncList(context) <= 0)
				return null;
		}

		for(MccMncListItem item: mccMncList) {
			if(item.getMcc() == mcc && item.getMnc() == mnc)
				return item;
		}

		return null;
	}

	/**
	 * Returns the list item  that matches the mcc and mnc of this connection data instance. The list item contains
	 * information about the country and the network provider
	 *
	 * @return Matching list item or null if not found
	 */
	public MccMncListItem lookupMccMnc() {
		return lookupMccMnc(context, cellularMCC, cellularMNC);
	}

	public static class MccMncListItem {
		private int mcc;
		private int mnc;
		private String cc;
		private String country;
		private String network;

		public MccMncListItem (int mcc, int mnc, String cc, String country, String network) {
			this.mcc = mcc;
			this.mnc = mnc;
			this.cc = cc;
			this.country = country;
			this.network = network;
		}

		public int getMcc() {
			return mcc;
		}

		public int getMnc() {
			return mnc;
		}

		public String getCc() {
			return cc;
		}

		public String getCountry() {
			return country;
		}

		public String getNetwork() {
			return network;
		}
	}
}
