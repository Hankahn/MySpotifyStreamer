package com.example.shawn.myspotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;

// Main activity for app responsible for searching for Artists
public class SearchActivity extends ActionBarActivity {

    private final String ARTIST_LIST = "artistList";
    private EditText mArtistSearchField;
    private RecyclerView mArtistResultView;
    private ArrayList<ArtistHelper> mArtists;
    private ArtistSearchTask mArtistSearchTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mArtistResultView = (RecyclerView)findViewById(R.id.recyclerview_artist_results);
        mArtistResultView.setLayoutManager(new LinearLayoutManager(this));
        mArtistSearchField = (EditText)findViewById(R.id.edittext_artist_search);

        // Set focus to the search field.
        mArtistSearchField.requestFocus();

        // Restore any preserved data for the activity
        if(savedInstanceState != null) {
            mArtists = savedInstanceState.getParcelableArrayList(ARTIST_LIST);

            if(mArtists != null) {
                PopulateResults();
            }
        }

        // Bring up the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mArtistSearchField, InputMethodManager.SHOW_IMPLICIT);

        // Setup a handler for when the user presses the search button on the keyboard or types enter
        mArtistSearchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    mArtistSearchTask = new ArtistSearchTask();

                    if (!v.getText().toString().trim().isEmpty()) {
                        mArtistSearchTask.execute(v.getText().toString());
                    } else {
                        Utils.makeToastShort(getApplicationContext(), getResources().getString(R.string.search_must_enter_terms));
                    }

                    return true;
                }

                return false;
            }
        });
    }

    public void clearArtistSearchButtonClick(View v) {
        if(mArtistSearchTask != null && !mArtistSearchTask.isCancelled()) {
            mArtistSearchTask.cancel(true);
        }

        mArtistSearchField.setText("");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(ARTIST_LIST, mArtists);
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

    // Grab the results and set the adapter
    private void PopulateResults() {
        RecyclerView.Adapter artistResultAdapter = new ArtistHelperAdapter(mArtists);
        mArtistResultView.setAdapter(artistResultAdapter);
    }

    // Adapter for Artist data
    public class ArtistHelperAdapter extends RecyclerView.Adapter<ArtistHelperAdapter.ArtistViewHolder> {

        private final String ARTIST_NAME_EXTRA = "ARTIST_NAME_EXTRA";
        private final String ARTIST_ID_EXTRA = "ARTIST_ID_EXTRA";
        private final String ARTIST_BUNDLE = "ARTIST_BUNDLE";

        public ArtistHelperAdapter(List<ArtistHelper> artists) {
            mArtists = new ArrayList<>(artists);
        }

        public class ArtistViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final TextView mTextViewArtistName;
            private final ImageView mImageViewArtist;

            public ArtistViewHolder(View view) {
                super(view);

                mTextViewArtistName = (TextView)view.findViewById(R.id.textview_artist_name);
                mImageViewArtist = (ImageView)view.findViewById(R.id.imageview_artist_image);

                // Any click on the ViewHolder will trigger onClick
                view.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int index = getAdapterPosition();

                ArtistHelper artist = mArtists.get(index);

                Bundle extras = new Bundle();

                extras.putString(ARTIST_NAME_EXTRA, artist.getName());
                extras.putString(ARTIST_ID_EXTRA, artist.getId());

                Intent topTenIntent = new Intent(getApplicationContext(), TopTenTracksActivity.class)
                        .putExtra(ARTIST_BUNDLE, extras);

                startActivity(topTenIntent);
            }

        }

        @Override
        public ArtistHelperAdapter.ArtistViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(getApplicationContext())
                    .inflate(R.layout.list_item_artist, (ViewGroup) viewGroup.getParent(), false);

            return new ArtistViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ArtistViewHolder artistViewHolder, int i) {
            String artistImageUrl = mArtists.get(i).getArtistImageUrl();

            artistViewHolder.mTextViewArtistName.setText(mArtists.get(i).getName());

            if(!artistImageUrl.equals("")) {
                Picasso.with(artistViewHolder.mImageViewArtist.getContext()).load(artistImageUrl).resizeDimen(R.dimen.artist_image_width, R.dimen.artist_image_height).into(artistViewHolder.mImageViewArtist);
            } else {
                Picasso.with(artistViewHolder.mImageViewArtist.getContext()).load(R.drawable.noimage)
                        .resize(getResources().getDimensionPixelSize(R.dimen.artist_image_width), getResources().getDimensionPixelSize(R.dimen.artist_image_height))
                        .into(artistViewHolder.mImageViewArtist);
            }
        }

        @Override
        public int getItemCount() {
            return mArtists.size();
        }
    }

    // AsyncTask for pulling Artists by supplied search terms
    public class ArtistSearchTask extends AsyncTask<String, Void, ArrayList<ArtistHelper>> {

        private final String LOG_TAG = ArtistSearchTask.class.getSimpleName();

        @Override
        protected ArrayList<ArtistHelper> doInBackground(String... params) {
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            ArrayList<ArtistHelper> artists = new ArrayList<>();

            ArtistsPager aPager = spotify.searchArtists(params[0]);

            for(Artist a : aPager.artists.items) {
                String artistImageUrl = "";

                if(a.images.size() > 0) {
                    artistImageUrl = a.images.get(0).url;
                }

                artists.add(new ArtistHelper(a.id, a.name, artistImageUrl));
            }

            return artists;
        }

        @Override
        protected void onPostExecute(ArrayList<ArtistHelper> result) {
            if(result != null) {
                if(result.size() > 0) {
                    Utils.makeToastShort(getApplicationContext(), getResources()
                            .getString(R.string.results_found_pre) + result.size() + getResources()
                            .getString(R.string.results_found_post));
                } else {
                    Utils.makeToastShort(getApplicationContext(), getResources().getString(R.string.results_found_none));
                }

                mArtists = result;

                PopulateResults();

                Log.v(LOG_TAG, getResources().getString(R.string.results_found_pre) + result.size());
            } else {
                Log.v(LOG_TAG, getResources().getString(R.string.search_failed));
            }
        }
    }

    // Lightweight helper for holding Artist data. Parcelable for interactivity storage
    public class ArtistHelper implements Parcelable {

        String mId;
        String mName;
        String mArtistImageUrl;

        public ArtistHelper(String id, String name, String artistImageUrl) {
            mId = id;
            mName = name;
            mArtistImageUrl = artistImageUrl;
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

        public String getArtistImageUrl() {
            return mArtistImageUrl;
        }

        public void setImageUrl(String artistImageUrl) {
            mArtistImageUrl = artistImageUrl;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mName);
            dest.writeString(mArtistImageUrl);
        }

        public final Parcelable.Creator<ArtistHelper> CREATOR
                = new Parcelable.Creator<ArtistHelper>() {

            @Override
            public ArtistHelper createFromParcel(Parcel source) {
                return null;
            }

            @Override
            public ArtistHelper[] newArray(int size) {
                return new ArtistHelper[0];
            }
        };

        private ArtistHelper(Parcel parcel) {
            mName = parcel.readString();
            mArtistImageUrl = parcel.readString();
        }
    }

}
