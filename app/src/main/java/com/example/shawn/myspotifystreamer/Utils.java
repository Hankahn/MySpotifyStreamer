package com.example.shawn.myspotifystreamer;

import android.content.Context;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

// Utility class where I can put helper methods
class Utils {

    public static void makeToastShort(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

    public static String formatMillisecondsAsTime(int milliseconds) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds));
    }

}
