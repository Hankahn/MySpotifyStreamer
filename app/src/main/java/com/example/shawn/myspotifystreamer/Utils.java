package com.example.shawn.myspotifystreamer;

import android.content.Context;
import android.widget.Toast;

// Utility class where I can put helper methods
class Utils {

    public static void makeToastShort(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

}
