package com.mantz_it.wearnetworknotifications;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.sufficientlysecure.donations.DonationsFragment;

/**
 * <h1>Wear Network Notifications - Donation Activity</h1>
 *
 * Module:      DonationActivity.java
 * Description: This is a activity that shows Dominik Schürmann's DonationsFragment.
 *              Note: Code is almost a exact copy of the example app of the Donation library.
 *              URL: https://github.com/dschuermann/android-donations-lib
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
public class DonationActivity extends FragmentActivity {
	/**
	 * Google
	 */
	private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApBKmoDk7kMzCmUhGgFDWcj0A+o3IkFA4rYAjxoLhMddkczqqyjopw/tW4pqpsZ/4hQrd/R5EHawBGWmXknWVRzI2A5IdS/tjD+vH4U7czADbOztWreT7q1Firs1Es30fsB8TGXVcK0gGklW5rjd4RnZzrWJgvNZFYjQkzVD22LH4IkekbmiOX40GFftjBrJvqWq65ps8SWfFc13vrBxcl0t5vM/tcPKHIuy5Q79+5i544GQE883RXSwYmcHTt0wA/+g4SDiwoDhD2BdoeiZgJX89i57XLESl8T+StZYrq5Q/zCPRw+C/EiTDiKsr8ZLuV5g8udpuhWNl2eCHDuHOkwIDAQAB";
	private static final String[] GOOGLE_CATALOG = new String[]{"wearnetworknotifications.donation.1",
			"wearnetworknotifications.donation.2", "wearnetworknotifications.donation.3", "wearnetworknotifications.donation.5", "wearnetworknotifications.donation.8",
			"wearnetworknotifications.donation.10"};

	/**
	 * PayPal
	 */
	private static final String PAYPAL_USER = "dennis.mantz@googlemail.com";
	private static final String PAYPAL_CURRENCY_CODE = "EUR";

	/**
	 * Flattr
	 */
	private static final String FLATTR_PROJECT_URL = "https://github.com/demantz/WearNetworkNotifications/";
	// FLATTR_URL without http:// !
	private static final String FLATTR_URL = "https://flattr.com/thing/3949530/demantzWearNetworkNotifications-on-GitHub";
	//create this url by browsing to "https://flattr.com/submit/auto?user_id=demantz&url=https%3A%2F%2Fgithub.com%2Fdemantz%2FWearNetworkNotifications&title=WearNetworkNotifications" once!

	/**
	 * Bitcoin
	 */
	private static final String BITCOIN_ADDRESS = "1NApYRwdPoiBjVfCKJV2EnmorJKBEr7Vtx";

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.donations_activity);

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		DonationsFragment donationsFragment;

		donationsFragment = DonationsFragment.newInstance(BuildConfig.DEBUG, true, GOOGLE_PUBKEY, GOOGLE_CATALOG,
				getResources().getStringArray(R.array.donation_google_catalog_values), true, PAYPAL_USER,
				PAYPAL_CURRENCY_CODE, getString(R.string.donation_paypal_item), true, FLATTR_PROJECT_URL, FLATTR_URL, true, BITCOIN_ADDRESS);

		ft.replace(R.id.donations_activity_container, donationsFragment, "donationsFragment");
		ft.commit();
	}

	/**
	 * Needed for Google Play In-app Billing. It uses startIntentSenderForResult(). The result is not propagated to
	 * the Fragment like in startActivityForResult(). Thus we need to propagate manually to our Fragment.
	 *
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag("donationsFragment");
		if (fragment != null) {
			fragment.onActivityResult(requestCode, resultCode, data);
		}
	}
}
