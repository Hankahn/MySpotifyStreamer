package com.example.shawn.myspotifystreamer;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

// Main activity for app responsible for searching for Artists
public class SearchActivity extends ActionBarActivity implements SearchFragment.Callback, TopTenTracksFragment.Callback {

    private static final String TOPTENTRACKSFRAGMENT_TAG = "TTTTAG";
    String ARTIST_BUNDLE = "ARTIST_BUNDLE";
    private String CURRENT_TRACK_EXTRA = "CURRENT_TRACK";
    private String TRACK_LIST_EXTRA = "TRACK_LIST";
    private String ARTIST_ID_EXTRA = "ARTIST_ID_EXTRA";
    private String ARTIST_NAME_EXTRA = "ARTIST_NAME_EXTRA";

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        if (findViewById(R.id.fragment_top_ten) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_top_ten, new TopTenTracksFragment(), TOPTENTRACKSFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        TopTenTracksFragment df = (TopTenTracksFragment)getSupportFragmentManager().findFragmentByTag(TOPTENTRACKSFRAGMENT_TAG);
        if ( null != df ) {
            //df.onLocationChanged(location);
        }
    }

    @Override
    public void onArtistSelected(String artistId, String artistName) {
        Bundle arguments = new Bundle();

        arguments.putString(ARTIST_NAME_EXTRA, artistName);
        arguments.putString(ARTIST_ID_EXTRA, artistId);

        if(mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            TopTenTracksFragment fragment = new TopTenTracksFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_top_ten, fragment, TOPTENTRACKSFRAGMENT_TAG)
                    .commit();
        } else {
            Intent topTenIntent = new Intent(this, TopTenTracksActivity.class)
                    .putExtra(ARTIST_BUNDLE, arguments);

            startActivity(topTenIntent);
        }
    }

    @Override
    public void onTrackSelected(int currentTrack,
                                ArrayList<TrackHelper> tracks) {
        Bundle arguments = new Bundle();

        arguments.putInt(CURRENT_TRACK_EXTRA, currentTrack);
        arguments.putParcelableArrayList(TRACK_LIST_EXTRA, tracks);

        FragmentManager fragmentManager = getFragmentManager();
        PlayerFragment newFragment = new PlayerFragment();

        newFragment.setArguments(arguments);

        newFragment.show(fragmentManager, "dialog");
    }

}
