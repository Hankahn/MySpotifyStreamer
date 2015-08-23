package com.example.shawn.myspotifystreamer;

import android.content.Context;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

// Utility class where I can put helper methods
class Utils {

    // Helper function to create a short toast
    public static void makeToastShort(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

    // Helper function to format milliseconds as a time string (Ex. 01:30)
    public static String formatMillisecondsAsTime(int milliseconds, String format) {
        return String.format(format,
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds));
    }

}
