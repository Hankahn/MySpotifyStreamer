package com.example.shawn.myspotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


// Artivity Fragment for Top Ten Tracks of an artist
public class TopTenTracksFragment extends Fragment {

    private final String LOG_TAG = TopTenTracksFragment.class.getSimpleName();

    final static String ARTIST_BUNDLE = "ARTIST_BUNDLE";
    final static String ARTIST_NAME_EXTRA = "ARTIST_NAME_EXTRA";
    final static String ARTIST_ID_EXTRA = "ARTIST_ID_EXTRA";

    final static String COUNTRY_OPTION = "country";

    private final String TRACK_LIST = "trackList";
    private RecyclerView mTrackListView;
    private ArrayList<TrackHelper> mTracks;
    private String mArtistId;
    private String mArtistName;
    private String mCurrentPlayingTrackUrl = "";
    private String mCurrentPlayingTrackName = "";
    private String mCurrentPlayingArtistName = "";
    private MenuItem mNowPlayingMenuItem;
    private MenuItem mShareMenuItem;
    private ShareActionProvider mShareActionProvider;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String event = intent.getStringExtra(PlayerService.EVENT_TAG);

            if (event.equals(PlayerService.TRACK_PLAYING)) {
                if(isAdded()) {
                    mNowPlayingMenuItem.setVisible(true);

                    mCurrentPlayingTrackUrl = intent.getStringExtra(PlayerService.TRACK_TAG);
                    mCurrentPlayingTrackName = intent.getStringExtra(PlayerService.TRACK_NAME_TAG);
                    mCurrentPlayingArtistName = intent.getStringExtra(PlayerService.TRACK_ARTIST_TAG);
                    mShareMenuItem.setVisible(true);

                    if (mShareActionProvider != null) {
                        mShareActionProvider.setShareIntent(createShareTrackIntent());
                    }
                }
            } else if (event.equals(PlayerService.TRACK_PAUSED)) {
                mNowPlayingMenuItem.setVisible(false);
            }
        }
    };

    public TopTenTracksFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_ten_tracks, container, false);

        mTrackListView = (RecyclerView) rootView.findViewById(R.id.recyclerview_artist_tracks);
        mTrackListView.setLayoutManager(new LinearLayoutManager(getActivity()));

        Bundle arguments = getArguments();
        Intent sourceIntent = getActivity().getIntent();

        // Check to see if the tracks are already stored
        if (savedInstanceState != null) {
            mTracks = savedInstanceState.getParcelableArrayList(TRACK_LIST);
            mArtistId = savedInstanceState.getString(ARTIST_ID_EXTRA);
            mArtistName = savedInstanceState.getString(ARTIST_NAME_EXTRA);
            populateResults();
        } else {
            // Pull in the supplied Artist and Artist ID. Fire off a pull for the Top Track list
            if (arguments != null) {
                mArtistId = arguments.getString(ARTIST_ID_EXTRA);
                mArtistName = arguments.getString(ARTIST_NAME_EXTRA);
            } else if (sourceIntent != null && sourceIntent.hasExtra(ARTIST_BUNDLE)) {
                Bundle artistBundle = sourceIntent.getBundleExtra(ARTIST_BUNDLE);

                mArtistId = artistBundle.getString(ARTIST_ID_EXTRA);
                mArtistName = artistBundle.getString(ARTIST_NAME_EXTRA);
            }
        }

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();

        if (actionBar != null && mArtistName != null) {
            actionBar.setSubtitle(mArtistName);
        }

        if (mTracks == null && mArtistId != null) {
            GetTracksTask mGetTracksTask = new GetTracksTask();

            mGetTracksTask.execute(mArtistId);
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter(PlayerService.INTENT_TAG));

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(TRACK_LIST, mTracks);
        outState.putString(ARTIST_ID_EXTRA, mArtistId);
        outState.putString(ARTIST_NAME_EXTRA, mArtistName);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_top_ten_tracks, menu);

        mNowPlayingMenuItem = menu.findItem(R.id.action_now_playing);
        mShareMenuItem = menu.findItem(R.id.action_share_track);

        if(getActivity() != null) {
            mShareActionProvider = new ShareActionProvider(getActivity());

            MenuItemCompat.setActionProvider(mShareMenuItem, mShareActionProvider);

            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareTrackIntent());
            } else {
                Log.d(LOG_TAG, getString(R.string.error_null_share_action_provider));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home){
            getActivity().finish();

            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        } else if (id == R.id.action_now_playing) {
            if(isAdded()) {
                Intent playerIntent = new Intent(getActivity(), PlayerActivity.class);

                startActivity(playerIntent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    // Grab the results and set the adapter
    private void populateResults() {
        RecyclerView.Adapter mTrackListAdapter = new TrackHelperAdapter(mTracks);
        mTrackListView.setAdapter(mTrackListAdapter);
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

    // Adapter for Track data
    public class TrackHelperAdapter
            extends RecyclerView.Adapter<TrackHelperAdapter.TrackViewHolder> {

        public TrackHelperAdapter(List<TrackHelper> tracks) {
            mTracks = new ArrayList<>(tracks);
        }

        public class TrackViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {
            public final TextView mTextViewTrackName;
            public final TextView mTextViewAlbumName;
            public final ImageView mImageViewAlbum;

            public TrackViewHolder(View view) {
                super(view);
                mTextViewTrackName = (TextView)view.findViewById(R.id.textview_track_name);
                mTextViewAlbumName = (TextView)view.findViewById(R.id.textview_album_name);
                mImageViewAlbum = (ImageView)view.findViewById(R.id.imageview_album_image);

                // Any click on the ViewHolder will trigger onClick
                view.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int index = getAdapterPosition();

                ((Callback) getActivity())
                        .onTrackSelected(index, mTracks);
            }

        }

        @Override
        public TrackViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(getActivity().getApplicationContext())
                    .inflate(R.layout.list_item_track, (ViewGroup) viewGroup.getParent(), false);

            return new TrackViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TrackViewHolder trackViewHolder, int i) {
            String albumImageUrl = mTracks.get(i).getAlbumImage();

            trackViewHolder.mTextViewTrackName.setText(mTracks.get(i).getName());
            trackViewHolder.mTextViewAlbumName.setText(mTracks.get(i).getAlbumName());

            if(!albumImageUrl.equals("")) {
                Picasso.with(trackViewHolder.mImageViewAlbum.getContext()).load(albumImageUrl)
                        .fit()
                        .centerCrop()
                        .into(trackViewHolder.mImageViewAlbum);
            } else {
                Picasso.with(trackViewHolder.mImageViewAlbum.getContext()).load(R.drawable.noimage)
                        .fit()
                        .centerCrop().into(trackViewHolder.mImageViewAlbum);
            }
        }

        @Override
        public int getItemCount() {
            return mTracks.size();
        }
    }

    // AsyncTask for pulling Top Tracks for an artist
    private class GetTracksTask extends AsyncTask<String, Void, Tracks> {

        private final String LOG_TAG = GetTracksTask.class.getSimpleName();

        @Override
        protected Tracks doInBackground(String... params) {
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            // Originally tried using getArtistTopTrack(String) but according to the forums and the
            // Spotify documentation you need to supply the country.
            // http://discussions.udacity.com/t/problem-getting-top-tracks-bad-request-error/20376
            // https://developer.spotify.com/web-api/get-artists-top-tracks/
            Map<String, Object> options = new HashMap<>();

            Context context = getActivity();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String countryCodeKey = context.getString(R.string.pref_country_code_key);
            String country = prefs.getString(countryCodeKey,
                    context.getString(R.string.pref_country_code_default));

            options.put(COUNTRY_OPTION, country);

            try {
                return spotify.getArtistTopTrack(params[0], options);
            } catch (RetrofitError rError) {
                Log.d(LOG_TAG, getString(R.string.error_spotify_track_get_failed));
                return null;
            }
        }

        @Override
        protected void onPostExecute(Tracks result) {
            if(result == null) {
                Utils.makeToastShort(getActivity(), getString(R.string.toast_track_get_failed));
                getActivity().finish();
                return;
            }

            mTracks = new ArrayList<>();

            if(result.tracks.size() > 0) {
                for (Track t : result.tracks) {
                    String albumImageUrl = "";

                    if (t.album.images.size() > 0) {
                        Image selectedImage = t.album.images.get(0);
                        for(Image i : t.album.images) {
                            if(i.height < 300 || i.width < 300) {
                                break;
                            } else {
                                selectedImage = i;
                            }
                        }
                        albumImageUrl = selectedImage.url;
                    }

                    mTracks.add(new TrackHelper(t.id, t.name, t.preview_url, mArtistName, t.album.name,
                            albumImageUrl));
                }
            } else {
                Utils.makeToastShort(getActivity(),
                        getString(R.string.message_no_tracks_found_for_artist));
            }

            populateResults();
        }
    }

    public interface Callback {

        void onTrackSelected(int currentTrack, ArrayList<TrackHelper> tracks);

    }

}
