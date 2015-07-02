package com.example.shawn.myspotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


// Artivity Fragment for Top Ten Tracks of an artist
public class TopTenTracksActivityFragment extends Fragment {

    private final String TRACK_LIST = "trackList";
    private RecyclerView mTrackListView;
    private ArrayList<TrackHelper> mTracks;

    public TopTenTracksActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String ARTIST_BUNDLE = "ARTIST_BUNDLE";
        String ARTIST_NAME_EXTRA = "ARTIST_NAME_EXTRA";
        String ARTIST_ID_EXTRA = "ARTIST_ID_EXTRA";

        View rootView = inflater.inflate(R.layout.fragment_top_ten_tracks, container, false);

        mTrackListView = (RecyclerView)rootView.findViewById(R.id.recyclerview_artist_tracks);
        mTrackListView.setLayoutManager(new LinearLayoutManager(getActivity()));

        Intent sourceIntent = getActivity().getIntent();

        // Check to see if the tracks are already stored
        if(savedInstanceState != null) {
            mTracks = savedInstanceState.getParcelableArrayList(TRACK_LIST);
            PopulateResults();
        } else {
            // Pull in the supplied Artist and Artist ID. Fire off a pull for the Top Track list
            if (sourceIntent != null && sourceIntent.hasExtra(ARTIST_BUNDLE)) {
                Bundle artistBundle = sourceIntent.getBundleExtra(ARTIST_BUNDLE);
                ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();

                if (actionBar != null) {
                    actionBar.setSubtitle(artistBundle.getString(ARTIST_NAME_EXTRA));
                }

                GetTracksTask mGetTracksTask = new GetTracksTask();

                mGetTracksTask.execute(artistBundle.getString(ARTIST_ID_EXTRA));
            }
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(TRACK_LIST, mTracks);
    }

    // Grab the results and set the adapter
    private void PopulateResults() {
        RecyclerView.Adapter mTrackListAdapter = new TrackHelperAdapter(mTracks);
        mTrackListView.setAdapter(mTrackListAdapter);
    }

    // Adapter for Track data
    public class TrackHelperAdapter extends RecyclerView.Adapter<TrackHelperAdapter.TrackViewHolder> {

        public TrackHelperAdapter(List<TrackHelper> tracks) {
            mTracks = new ArrayList<>(tracks);
        }

        public class TrackViewHolder extends RecyclerView.ViewHolder {// implements View.OnClickListener {
            public final TextView mTextViewTrackName;
            public final TextView mTextViewAlbumName;
            public final ImageView mImageViewAlbum;

            public TrackViewHolder(View view) {
                super(view);
                mTextViewTrackName = (TextView)view.findViewById(R.id.textview_track_name);
                mTextViewAlbumName = (TextView)view.findViewById(R.id.textview_album_name);
                mImageViewAlbum = (ImageView)view.findViewById(R.id.imageview_album_image);

                // This will be user later in Part 2
                /*mTextViewTrackName.setOnClickListener(this);
                mTextViewAlbumName.setOnClickListener(this);
                mImageViewAlbum.setOnClickListener(this);*/
            }

            // This will be user later in Part 2
            /*@Override
            public void onClick(View v) {
                int index = getAdapterPosition();

                TrackHelper artist = mArtists.get(index);

                Bundle extras = new Bundle();

                extras.putString(ARTIST_NAME_EXTRA, artist.getName());
                extras.putString(ARTIST_ID_EXTRA, artist.getId());

                Intent topTenIntent = new Intent(getActivity().getApplicationContext(), TopTenTracksActivity.class)
                        .putExtra(ARTIST_BUNDLE, extras);

                startActivity(topTenIntent);
            }*/

        }

        @Override
        public TrackViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(getActivity().getApplicationContext())
                    .inflate(R.layout.list_item_track, (ViewGroup) viewGroup.getParent(), false);

            return new TrackViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TrackViewHolder trackViewHolder, int i) {
            String albumImageUrl = mTracks.get(i).getAlbumImageUrl();

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

            options.put("country", Locale.getDefault().getCountry());

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
                        albumImageUrl = t.album.images.get(0).url;
                    }

                    mTracks.add(new TrackHelper(t.id, t.name, t.album.name, albumImageUrl));
                }
            } else {
                Utils.makeToastShort(getActivity(), getString(R.string.message_no_tracks_found_for_artist));
            }

            PopulateResults();
        }
    }

    // Lightweight helper for holding Track data. Parcelable for eventual interactivity storage
    public class TrackHelper implements Parcelable {

        String mId;
        String mName;
        String mAlbumName;
        String mAlbumImage;

        public TrackHelper(String id, String name, String albumName, String imageUrl) {
            mId = id;
            mName = name;
            mAlbumName = albumName;
            mAlbumImage = imageUrl;
        }

        public String getId() {
            return mId;
        }

        public void setId(String id) {
            mId = id;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getAlbumName() {
            return mAlbumName;
        }

        public void setAlbumName(String albumName) {
            mAlbumName = albumName;
        }

        public String getAlbumImageUrl() {
            return mAlbumImage;
        }

        public void setAlbumImageUrl(String albumImageUrl) {
            mAlbumImage = albumImageUrl;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mName);
            dest.writeString(mAlbumName);
            dest.writeString(mAlbumImage);
        }

        public final Parcelable.Creator<TrackHelper> CREATOR
                = new Parcelable.Creator<TrackHelper>() {

            @Override
            public TrackHelper createFromParcel(Parcel source) {
                return null;
            }

            @Override
            public TrackHelper[] newArray(int size) {
                return new TrackHelper[0];
            }
        };

        private TrackHelper(Parcel parcel) {
            mName = parcel.readString();
            mAlbumName = parcel.readString();
            mAlbumImage = parcel.readString();
        }
    }

}
