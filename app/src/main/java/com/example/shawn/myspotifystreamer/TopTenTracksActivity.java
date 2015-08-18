package com.example.shawn.myspotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

// Activity for Top Tracks by artist
public class TopTenTracksActivity extends ActionBarActivity implements TopTenTracksFragment.Callback {

    private String TRACK_BUNDLE = "TRACK_BUNDLE";
    private String CURRENT_TRACK_EXTRA = "CURRENT_TRACK";
    private String TRACK_LIST_EXTRA = "TRACK_LIST";

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_top_ten_tracks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home){
            this.finish();

            return true;
        } else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
