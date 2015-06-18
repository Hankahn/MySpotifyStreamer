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


// Artivity Fragment for Top Ten Tracks of an artist
public class TopTenTracksActivityFragment extends Fragment {

    private RecyclerView mSongListView;
    private ArrayList<SongHelper> mSongs;

    public TopTenTracksActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_ten_tracks, container, false);

        mSongListView = (RecyclerView)rootView.findViewById(R.id.recyclerview_artist_songs);
        mSongListView.setLayoutManager(new LinearLayoutManager(getActivity()));

        Intent sourceIntent = getActivity().getIntent();

        // Pull in the supplied Artist and Artist ID. Fire off a pull for the Top Track list
        String ARTIST_BUNDLE = "ARTIST_BUNDLE";
        if(sourceIntent != null && sourceIntent.hasExtra(ARTIST_BUNDLE)) {
            Bundle artistBundle = sourceIntent.getBundleExtra(ARTIST_BUNDLE);

            ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();

            if(actionBar != null) {
                String ARTIST_NAME_EXTRA = "ARTIST_NAME_EXTRA";
                actionBar.setSubtitle(artistBundle.getString(ARTIST_NAME_EXTRA));
            }

            SongGetTask mSongGetTask = new SongGetTask();

            String ARTIST_ID_EXTRA = "ARTIST_ID_EXTRA";
            mSongGetTask.execute(artistBundle.getString(ARTIST_ID_EXTRA));
        }

        return rootView;
    }

    // Grab the results and set the adapter
    private void PopulateResults() {
        RecyclerView.Adapter mSongListAdapter = new SongHelperAdapter(mSongs);
        mSongListView.setAdapter(mSongListAdapter);
    }

    // Adapter for Song data
    public class SongHelperAdapter extends RecyclerView.Adapter<SongHelperAdapter.SongViewHolder> {

        public SongHelperAdapter(List<SongHelper> songs) {
            mSongs = new ArrayList<>(songs);
        }

        public class SongViewHolder extends RecyclerView.ViewHolder {// implements View.OnClickListener {
            public final TextView mTextViewSongName;
            public final TextView mTextViewAlbumName;
            public final ImageView mImageViewAlbum;

            public SongViewHolder(View view) {
                super(view);
                mTextViewSongName = (TextView)view.findViewById(R.id.textview_song_name);
                mTextViewAlbumName = (TextView)view.findViewById(R.id.textview_album_name);
                mImageViewAlbum = (ImageView)view.findViewById(R.id.imageview_album_image);

                // This will be user later in Part 2
                /*mTextViewSongName.setOnClickListener(this);
                mTextViewAlbumName.setOnClickListener(this);
                mImageViewAlbum.setOnClickListener(this);*/
            }

            // This will be user later in Part 2
            /*@Override
            public void onClick(View v) {
                int index = getAdapterPosition();

                SongHelper artist = mArtists.get(index);

                Bundle extras = new Bundle();

                extras.putString(ARTIST_NAME_EXTRA, artist.getName());
                extras.putString(ARTIST_ID_EXTRA, artist.getId());

                Intent topTenIntent = new Intent(getActivity().getApplicationContext(), TopTenTracksActivity.class)
                        .putExtra(ARTIST_BUNDLE, extras);

                startActivity(topTenIntent);
            }*/

        }

        @Override
        public SongViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(getActivity().getApplicationContext())
                    .inflate(R.layout.list_item_song, (ViewGroup) viewGroup.getParent(), false);

            return new SongViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SongViewHolder songViewHolder, int i) {
            String albumImageUrl = mSongs.get(i).getAlbumImageUrl();

            songViewHolder.mTextViewSongName.setText(mSongs.get(i).getName());
            songViewHolder.mTextViewAlbumName.setText(mSongs.get(i).getAlbumName());

            if(!albumImageUrl.equals("")) {
                Picasso.with(songViewHolder.mImageViewAlbum.getContext()).load(albumImageUrl)
                        .resizeDimen(R.dimen.album_image_width, R.dimen.album_image_height)
                        .into(songViewHolder.mImageViewAlbum);
            } else {
                Picasso.with(songViewHolder.mImageViewAlbum.getContext()).load(R.drawable.noimage)
                        .resize(getResources().getDimensionPixelSize(R.dimen.artist_image_width), getResources().getDimensionPixelSize(R.dimen.album_image_height))
                        .into(songViewHolder.mImageViewAlbum);
            }
        }

        @Override
        public int getItemCount() {
            return mSongs.size();
        }
    }

    // AsyncTask for pulling Top Tracks for an artist
    private class SongGetTask extends AsyncTask<String, Void, ArrayList<SongHelper>> {

        private final String LOG_TAG = SongGetTask.class.getSimpleName();
        private boolean wasCancelled = false;

        @Override
        protected ArrayList<SongHelper> doInBackground(String... params) {
            ArrayList<SongHelper> songs = new ArrayList<>();

            if(Utils.isDeviceOnline(getActivity())) {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                // Originally tried using getArtistTopTrack(String) but according to the forums and the
                // Spotify documentation you need to supply the country.
                // http://discussions.udacity.com/t/problem-getting-top-tracks-bad-request-error/20376
                // https://developer.spotify.com/web-api/get-artists-top-tracks/
                Map<String, Object> options = new HashMap<>();

                options.put("country", Locale.getDefault().getCountry());

                Tracks tracks = spotify.getArtistTopTrack(params[0], options);

                for (Track t : tracks.tracks) {
                    String albumImageUrl = "";

                    if (t.album.images.size() > 0) {
                        albumImageUrl = t.album.images.get(0).url;
                    }

                    songs.add(new SongHelper(t.id, t.name, t.album.name, albumImageUrl));
                }
            } else {
                Log.d(LOG_TAG, getString(R.string.message_device_not_online));
                wasCancelled = true;
            }

            return songs;
        }

        @Override
        protected void onPostExecute(ArrayList<SongHelper> result) {
            // The call was cancelled so get out
            if(wasCancelled) {
                Utils.makeToastShort(getActivity(), getString(R.string.toast_song_get_failed));
                getActivity().finish();
            }

            mSongs = result;

            PopulateResults();
        }
    }

    // Lightweight helper for holding Song data. Parcelable for eventual interactivity storage
    public class SongHelper implements Parcelable {

        String mId;
        String mName;
        String mAlbumName;
        String mAlbumImage;

        public SongHelper(String id, String name, String albumName, String imageUrl) {
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

        public final Parcelable.Creator<SongHelper> CREATOR
                = new Parcelable.Creator<SongHelper>() {

            @Override
            public SongHelper createFromParcel(Parcel source) {
                return null;
            }

            @Override
            public SongHelper[] newArray(int size) {
                return new SongHelper[0];
            }
        };

        private SongHelper(Parcel parcel) {
            mName = parcel.readString();
            mAlbumName = parcel.readString();
            mAlbumImage = parcel.readString();
        }
    }

}
