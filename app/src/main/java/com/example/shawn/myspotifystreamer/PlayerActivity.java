package com.example.shawn.myspotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by Shawn on 7/7/2015.
 */
public class PlayerActivity extends ActionBarActivity {

    private final String LOG_TAG = PlayerActivity.class.getSimpleName();

    private String mCurrentPlayingTrackUrl = "";
    private String mCurrentPlayingTrackName = "";
    private String mCurrentPlayingArtistName = "";
    private MenuItem mShareMenuItem;
    private ShareActionProvider mShareActionProvider;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String event = intent.getStringExtra(PlayerService.EVENT_TAG);

            if (event.equals(PlayerService.TRACK_PLAYING)) {
                mCurrentPlayingTrackUrl = intent.getStringExtra(PlayerService.TRACK_TAG);
                mCurrentPlayingTrackName = intent.getStringExtra(PlayerService.TRACK_NAME_TAG);
                mCurrentPlayingArtistName = intent.getStringExtra(PlayerService.TRACK_ARTIST_TAG);

                if(mShareMenuItem != null) {
                    mShareMenuItem.setVisible(true);
                }

                if(mShareActionProvider != null) {
                    mShareActionProvider.setShareIntent(createShareTrackIntent());
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        ActionBar actionBar = getSupportActionBar();

        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(PlayerService.INTENT_TAG));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);

        mShareMenuItem = menu.findItem(R.id.action_share_track);

        mShareActionProvider = new ShareActionProvider(this);

        MenuItemCompat.setActionProvider(mShareMenuItem, mShareActionProvider);

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareTrackIntent());
        } else {
            Log.d(LOG_TAG, getString(R.string.error_null_share_action_provider));
        }

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
        }

        return super.onOptionsItemSelected(item);
    }

    // Creates a share intent
    private Intent createShareTrackIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType(getString(R.string.notification_type));
        shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(
                getString(R.string.share_message_format),
                mCurrentPlayingTrackName, mCurrentPlayingArtistName, mCurrentPlayingTrackUrl));
        return shareIntent;
    }

}
