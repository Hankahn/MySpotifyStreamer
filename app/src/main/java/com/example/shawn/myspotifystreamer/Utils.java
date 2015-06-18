package com.example.shawn.myspotifystreamer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

// Utility class where I can put helper methods
class Utils {

    // I found this stackoverflow post about detecting if the device has access to the internet:
    // http://stackoverflow.com/questions/17711079/check-if-device-has-internet-connection
    // I posted a question about the best way to handle this:
    // http://discussions.udacity.com/t/best-way-to-handle-when-the-device-is-not-online/22676
    public static boolean isDeviceOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnected();
    }

    public static void makeToastShort(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

}
