Network Notifications for Android Wear
======================================

This is the repository of the Wear Network Notifications Application.
It's a simply yet helpful app that will bring up notifications on connectivity
state and signal strength on an Android Wear device.

Just one glance to know if you are using Wifi, LTE, UMTS or if you are currently offline! 
See the signl strength in percent or dBm and the current network name! 
The information will automatically update every time the watch wakes 
up from ambient mode.

You even get a notification if your watch looses the Bluetooth connection 
to the phone. This makes any 'forgot my phone' app unnecessary!

The notifications can be customized through the companion app on the 
phone and it is also possible to only show the notification cards 
manually on the watch.


![Wear Network Notifications](https://raw.githubusercontent.com/demantz/WearNetworkNotifications/master/screenshots/feature.png)


Implemented Features
--------------------
* Notification on transition between WIFI, MOBILE and OFFLINE
* Customizable behaviour
* Signal strength indicatior icon and absolute value in %, RSSI/dBm and ASU level
* [NEW] Additional details pages showing MCC, MNC, LAC, CID, BSSID, IP, ...
* Automatic update of the notification when the wearable device wakes up
* Available languages: English and German

Android Permissions
-------------------
* VIBRATE: Obviously to make the watch vibrate on a notification (can be turned off)
* ACCESS_NETWORK_STATE: Get information about the network connection status
* ACCESS_WIFI_STATE: Get the wifi name and signal strength
* READ_PHONE_STATE: Get information about the cellular connection
* ACCESS_COARSE_LOCATION: To query the system for the currently connected
  cell (to get the signal strength) this permission is also required
* INTERNET and BILLING: For the donation dialog which uses PayPal, Google in-app purchases,
  Flattr and Bitcoin.

Testet Devices
--------------
* LG G Watch R
* LG G Watch


Known Issues
------------
* If the notification is swiped away immediately after the device wakes
  up, the notification might come back because the updated notification
  shows up.
* The Android system doesn't send a Broadcast after losing network connection
  (WIFI --> OFFLINE  and  MOBILE --> OFFLINE aren't working)
* On my Nexus 5 with Lollipop: After the Nexus 5 connects to a Wifi network
  while locked and screen is of, the Android system reports: wifi-speed==-1 
  and wifi-rssi==-127 until I turn on the screen of the Nexus 5
  (see: https://code.google.com/p/android/issues/detail?id=38483)
  I worked around this issue by using the RSSI from the last wifi scan
  results in this case. Not ideal, but what should I do?


Installation / Usage
--------------------
The app project in this repository was generated by Android Studio.
Use the 'import project' function in Android Studio to import the repository
as new project.

The WearNetworkNotifications.apk file is also in this repository so that it can be used without 
building it yourself. But it won't be synched to the latest code base all the time.
Install the apk file on the handheld device and it will be synced to the
wearable device automatically (by the Android system).


Thanks
------
I want to thank Dominik Schürmann for developing a [Android Donation Fragment](https://github.com/dschuermann/android-donations-lib)
which is used by this app!

License
-------
This application is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.
[http://www.gnu.org/licenses/gpl.html](http://www.gnu.org/licenses/gpl.html) GPL version 2 or higher

principal author: Dennis Mantz <dennis.mantz[at]googlemail.com>


![Wear Network Notifications 1](https://raw.githubusercontent.com/demantz/WearNetworkNotifications/master/screenshots/WearNetworkNotifications1.jpg)

![Wear Network Notifications 2](https://raw.githubusercontent.com/demantz/WearNetworkNotifications/master/screenshots/WearNetworkNotifications2.jpg)

![Wear Network Notifications 3](https://raw.githubusercontent.com/demantz/WearNetworkNotifications/master/screenshots/WearNetworkNotifications3.jpg)


(photos by Dennis Mantz)
