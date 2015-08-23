package com.example.shawn.myspotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import java.util.ArrayList;

// Activity for Top Tracks by artist
public class TopTenTracksActivity extends ActionBarActivity
        implements TopTenTracksFragment.Callback {

    private final static String TRACK_BUNDLE = "TRACK_BUNDLE";
    private final static String CURRENT_TRACK_EXTRA = "CURRENT_TRACK";
    private final static String TRACK_LIST_EXTRA = "TRACK_LIST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_ten_tracks);

        ActionBar actionBar = getSupportActionBar();

        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    public void onTrackSelected(int currentTrack,
                                ArrayList<TrackHelper> tracks) {
        Bundle arguments = new Bundle();

        arguments.putInt(CURRENT_TRACK_EXTRA, currentTrack);
        arguments.putParcelableArrayList(TRACK_LIST_EXTRA, tracks);

        Intent playerIntent = new Intent(this, PlayerActivity.class)
                .putExtra(TRACK_BUNDLE, arguments);

        startActivity(playerIntent);
    }

}
