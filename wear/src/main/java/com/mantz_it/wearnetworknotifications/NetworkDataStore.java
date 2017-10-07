package com.mantz_it.wearnetworknotifications;

import android.util.Log;

import com.mantz_it.common.ConnectionData;

import java.util.ArrayList;

/**
 * Created by dennis on 4/12/17.
 */

public class NetworkDataStore {
    private static NetworkDataStore instance = null;
    private static final String LOGTAG = "NetworkDataStore";

    private Callback callback;
    private ArrayList<ConnectionData> store;

    private NetworkDataStore() {
        this.store = new ArrayList<>(50);
    }

    public static NetworkDataStore getInstance() {
        if(instance == null) {
            instance = new NetworkDataStore();
        }
        Log.d(LOGTAG, "getInstance: instance="+instance.toString());
        return instance;
    }

    public void setCallback(Callback callback) {
        Log.d(LOGTAG, "setCallback: instance="+instance.toString());
        this.callback = callback;
    }

    public void insertData(ConnectionData data) {
        // remove old data:
        Log.d(LOGTAG, "insertData: instance="+instance.toString());
        long now = System.currentTimeMillis();
        long thresholdTime = now - (1000*60*5);
        for(int i = store.size()-1; i >= 0; i--) {
            if(store.get(i).getTimestamp() < thresholdTime)
                store.remove(i);
            else
                break; // we expect the items are sorted by the timestamp
        }
        store.add(0, data);
        if(callback != null)
            callback.onDataUpdated(this);
    }

    public int getDataCount() {
        return store.size();
    }

    public ConnectionData getData(int index) {
        if(index < store.size())
            return store.get(index);
        else
            return null;
    }



    public interface Callback {
        void onDataUpdated(NetworkDataStore networkDataStore);
    }
}
