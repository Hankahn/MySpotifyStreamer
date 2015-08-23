package com.example.shawn.myspotifystreamer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import retrofit.RetrofitError;

/**
 * Created by Shawn on 7/6/2015.
 */
public class SearchFragment extends Fragment {

    final static String COUNTRY_OPTION = "country";

    private final String ARTIST_LIST = "artistList";
    private SearchView mArtistSearchField;
    private RecyclerView mArtistResultView;
    private ArrayList<SearchFragment.ArtistHelper> mArtists;
    private ArtistSearchTask mArtistSearchTask = null;
    private ProgressDialog mProgressDialog;

    public SearchFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);

        mArtistResultView = (RecyclerView) rootView.findViewById(R.id.recyclerview_artist_results);
        mArtistResultView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mArtistSearchField = (SearchView) rootView.findViewById(R.id.searchview_artist_search);

        // Set focus to the search field.
        mArtistSearchField.setIconifiedByDefault(false);
        mArtistSearchField.setQueryHint(getResources().getString(R.string.search_hint));

        // Restore any preserved data for the activity
        if (savedInstanceState != null) {
            mArtists = savedInstanceState.getParcelableArrayList(ARTIST_LIST);

            if (mArtists != null) {
                PopulateResults();
            }
        }

        // Setup a handler for when the user presses the search button on the keyboard or
        // types enter
        mArtistSearchField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Clean up any remaining UI elements from the last search, this mainly comes up
                // when using a physical keyboard and the emulator
                if(mArtistSearchTask != null) {
                    mArtistSearchTask.cancel(true);
                }

                if(mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }

                mArtistSearchTask = new ArtistSearchTask();

                if (!query.isEmpty()) {
                    mArtistSearchTask.execute(query);
                } else {
                    Utils.makeToastShort(getActivity(), getResources()
                            .getString(R.string.search_must_enter_terms));
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(ARTIST_LIST, mArtists);
    }

    // Grab the results and set the adapter
    private void PopulateResults() {
        RecyclerView.Adapter artistResultAdapter = new ArtistHelperAdapter(mArtists);
        mArtistResultView.setAdapter(artistResultAdapter);
    }

    // Adapter for Artist data
    public class ArtistHelperAdapter
            extends RecyclerView.Adapter<ArtistHelperAdapter.ArtistViewHolder> {

        public ArtistHelperAdapter(List<ArtistHelper> artists) {
            mArtists = new ArrayList<>(artists);
        }

        public class ArtistViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {
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

                ((Callback) getActivity())
                        .onArtistSelected(artist.getId(), artist.getName());
            }

        }

        @Override
        public ArtistHelperAdapter.ArtistViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(getActivity())
                    .inflate(R.layout.list_item_artist, (ViewGroup) viewGroup.getParent(), false);

            return new ArtistViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ArtistViewHolder artistViewHolder, int i) {
            String artistImageUrl = mArtists.get(i).getArtistImageUrl();

            artistViewHolder.mTextViewArtistName.setText(mArtists.get(i).getName());

            if(!artistImageUrl.equals("")) {
                Picasso.with(artistViewHolder.mImageViewArtist.getContext()).load(artistImageUrl)
                        .fit()
                        .centerCrop()
                        .into(artistViewHolder.mImageViewArtist);
            } else {
                Picasso.with(artistViewHolder.mImageViewArtist.getContext()).load(R.drawable.noimage)
                        .fit()
                        .centerCrop()
                        .into(artistViewHolder.mImageViewArtist);
            }
        }

        @Override
        public int getItemCount() {
            return mArtists.size();
        }
    }

    // AsyncTask for pulling Artists by supplied search terms
    public class ArtistSearchTask extends AsyncTask<String, Void, ArtistsPager> {

        private final String LOG_TAG = ArtistSearchTask.class.getSimpleName();

        @Override
        protected ArtistsPager doInBackground(String... params) {
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            try {
                Map<String, Object> options = new HashMap<>();

                Context context = getActivity();

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String countryCodeKey = context.getString(R.string.pref_country_code_key);
                String country = prefs.getString(countryCodeKey,
                        context.getString(R.string.pref_country_code_default));

                options.put(COUNTRY_OPTION, country);

                return spotify.searchArtists(params[0], options);
            } catch (RetrofitError rError) {
                Log.d(LOG_TAG, getString(R.string.error_spotify_artist_search_failed) + rError.toString());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = ProgressDialog.show(getActivity(),
                    getString(R.string.progress_dialog_title), getString(R.string.progress_dialog_message));
        }

        @Override
        protected void onPostExecute(ArtistsPager result) {
            mProgressDialog.dismiss();

            if(result == null) {
                Utils.makeToastShort(getActivity(),
                        getString(R.string.toast_artist_search_failed_retry));
                return;
            }

            mArtists = new ArrayList<>();

            for (Artist a : result.artists.items) {
                String artistImageUrl = "";

                if (a.images.size() > 0) {
                    Image selectedImage = a.images.get(0);
                    for(Image i : a.images) {
                        if(i.height < 300 || i.width < 300) {
                            break;
                        } else {
                            selectedImage = i;
                        }
                    }
                    artistImageUrl = selectedImage.url;
                }

                mArtists.add(new ArtistHelper(a.id, a.name, artistImageUrl));
            }

            if(mArtists.size() > 0) {
                Utils.makeToastShort(getActivity(),
                        String.format(getString(R.string.message_results_found), mArtists.size()));
            } else {
                Utils.makeToastShort(getActivity(), getResources().getString(R.string.message_no_results_found));
            }

            PopulateResults();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
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

    public interface Callback {

        void onArtistSelected(String artistId, String artistName);

    }

}
