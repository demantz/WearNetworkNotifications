package com.mantz_it.wearnetworknotifications;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * <h1>Wear Network Notifications - Start Activity</h1>
 *
 * Module:      StartActivity.java
 * Description: This is the main activity of the wearable app and simply invokes the
 *              NetworkNotificationService in order to manually trigger the notification.
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
public class StartActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// invoke the NetworkNotification service:
		Intent intent = new Intent(this, NetworkNotificationService.class);
		intent.setAction(NetworkNotificationService.ACTION_SHOW_NOTIFICATION);
		startService(intent);
		finish();
	}
}
