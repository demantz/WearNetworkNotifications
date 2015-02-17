package com.mantz_it.wearnetworknotifications;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by dennis on 12/02/15.
 */
public class StartActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, NetworkNotificationService.class);
		intent.setAction(NetworkNotificationService.ACTION_SHOW_NOTIFICATION);
		startService(intent);
		finish();
	}
}
