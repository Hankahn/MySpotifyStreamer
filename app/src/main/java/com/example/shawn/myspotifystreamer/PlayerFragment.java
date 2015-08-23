package com.example.shawn.myspotifystreamer;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by Shawn on 7/7/2015.
 */
public class PlayerFragment extends DialogFragment {

    private final String LOG_TAG = PlayerFragment.class.getSimpleName();

    private final static String TRACK_BUNDLE = "TRACK_BUNDLE";
    private final static String CURRENT_TRACK_EXTRA = "CURRENT_TRACK";
    private final static String TRACK_LIST_EXTRA = "TRACK_LIST";

    private int mCurrentTrack = -1;
    private ArrayList<TrackHelper> mTracks;

    private PlayerService mService;
    private Intent mIntent;

    private TextView mArtistNameTextView;
    private TextView mAlbumNameTextView;
    private ImageView mAlbumImageView;
    private TextView mTrackNameTextView;
    private ImageButton mPlayButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private SeekBar mProgressSeekBar;
    private TextView mElapsedText;
    private TextView mDurationText;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String event = intent.getStringExtra(PlayerService.EVENT_TAG);

            switch (event) {
                case PlayerService.TRACK_PREPARED:
                    int duration = intent.getIntExtra(PlayerService.DURATION_TAG, 0);

                    mProgressSeekBar.setMax(duration);
                    mDurationText.setText(Utils.formatMillisecondsAsTime(duration, getString(R.string.millisecond_time_format)));
                    break;
                case PlayerService.TRACK_CHANGE:
                    mCurrentTrack = intent.getIntExtra(PlayerService.TRACK_TAG, 0);
                    updateTrack();
                    break;
                case PlayerService.TRACK_STATUS:
                    if (mService != null) {
                        if (mService.isPlaying()) {
                            int position = intent.getIntExtra(PlayerService.POSTION_TAG, 0);
                            mProgressSeekBar.setProgress(position);
                            mElapsedText.setText(Utils.formatMillisecondsAsTime(position, getString(R.string.millisecond_time_format)));
                        }
                    }
                    break;
                case PlayerService.TRACK_PLAYING:
                    mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
                    break;
                case PlayerService.TRACK_PAUSED:
                    mPlayButton.setImageResource(android.R.drawable.ic_media_play);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter(PlayerService.INTENT_TAG));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        mArtistNameTextView = (TextView) rootView.findViewById(R.id.textview_artist_name);
        mAlbumNameTextView = (TextView) rootView.findViewById(R.id.textview_album_name);
        mAlbumImageView = (ImageView) rootView.findViewById(R.id.imageview_album_image);
        mTrackNameTextView = (TextView) rootView.findViewById(R.id.textview_track_name);
        mPlayButton = (ImageButton) rootView.findViewById(R.id.button_play);
        mNextButton = (ImageButton) rootView.findViewById(R.id.button_next);
        mPrevButton = (ImageButton) rootView.findViewById(R.id.button_prev);
        mProgressSeekBar = (SeekBar) rootView.findViewById(R.id.seekbar_progress);
        mElapsedText = (TextView) rootView.findViewById(R.id.text_elapsed);
        mDurationText = (TextView) rootView.findViewById(R.id.text_duration);

        Bundle arguments = getArguments();
        Intent sourceIntent = getActivity().getIntent();

        if(savedInstanceState != null) {
            mCurrentTrack = savedInstanceState.getInt(CURRENT_TRACK_EXTRA, 0);
            mTracks = savedInstanceState.getParcelableArrayList(TRACK_LIST_EXTRA);
        } else {
            if (sourceIntent != null && sourceIntent.hasExtra(TRACK_BUNDLE)) {
                Bundle bundleArguments = sourceIntent.getBundleExtra(TRACK_BUNDLE);

                mCurrentTrack = bundleArguments.getInt(CURRENT_TRACK_EXTRA, 0);
                mTracks = bundleArguments.getParcelableArrayList(TRACK_LIST_EXTRA);
            } else if (arguments != null) {
                mCurrentTrack = arguments.getInt(CURRENT_TRACK_EXTRA);
                mTracks = arguments.getParcelableArrayList(TRACK_LIST_EXTRA);
            }
        }

        mPlayButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (mService != null) {
                    if (mService.isPlaying()) {
                        mService.pauseTrack();
                    } else if (mService.isPaused()) {
                        mService.resumeTrack();
                    } else {
                        mService.playTrack();
                    }
                }
            }

        });

        mNextButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                nextTrack();
            }

        });

        mPrevButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                previousTrack();
            }

        });

        mProgressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (mService != null) {
                        mService.seekPosition(progress);
                    }
                }

                mElapsedText.setText(Utils.formatMillisecondsAsTime(progress, getString(R.string.millisecond_time_format)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(mIntent == null) {
            mIntent = new Intent(getActivity(), PlayerService.class);
            getActivity().bindService(mIntent, playerConnection, Context.BIND_AUTO_CREATE);
            getActivity().startService(mIntent);
        }
    }

    @Override
    public void onDestroy() {
        getActivity().unbindService(playerConnection);
        mService = null;
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private ServiceConnection playerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;

            mService = binder.getService();

            // This code is run when coming back from the now playing button
            if (mTracks == null && mService.isPlaying()) {
                mTracks = mService.getTracks();
                mCurrentTrack = mService.getCurrentTrack();

                int duration = mService.getDuration();

                updateTrack();

                mProgressSeekBar.setMax(duration);
                mDurationText.setText(Utils.formatMillisecondsAsTime(duration, getString(R.string.millisecond_time_format)));
            }
            // This code is run when the screen rotates
            else if (mService.getTracks() != null && mTracks != null && mService.getCurrentTrackUrl().equals(mTracks.get(mCurrentTrack).getUrl())) {
                int duration = mService.getDuration();
                int position = mService.getPosition();

                updateTrack();

                mProgressSeekBar.setMax(duration);
                mDurationText.setText(Utils.formatMillisecondsAsTime(duration, getString(R.string.millisecond_time_format)));

                if (mService.isPaused()) {
                    mPlayButton.setImageResource(android.R.drawable.ic_media_play);
                    mElapsedText.setText(Utils.formatMillisecondsAsTime(position, getString(R.string.millisecond_time_format)));
                    mProgressSeekBar.setProgress(position);
                }
            }
            // This code is run when a user has selected a track from the track list
            else {
                mService.setTracks(mTracks);

                updateTrack();

                mService.setCurrentTrack(mCurrentTrack);
                mService.playTrack();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(CURRENT_TRACK_EXTRA, mCurrentTrack);
        outState.putParcelableArrayList(TRACK_LIST_EXTRA, mTracks);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    // Update the player UI based on the current tracks
    private void updateTrack() {
        if(mTracks == null) {
            mTracks = mService.getTracks();
        }

        TrackHelper track = mTracks.get(mCurrentTrack);

        mArtistNameTextView.setText(track.getArtistName());
        mAlbumNameTextView.setText(track.getAlbumName());
        mTrackNameTextView.setText(track.getName());

        if(!track.getAlbumImage().equals("")) {
            Picasso.with(getActivity()).load(track.getAlbumImage())
                    .fit()
                    .centerCrop()
                    .into(mAlbumImageView);
        } else {
            Picasso.with(getActivity()).load(R.drawable.noimage)
                    .fit()
                    .centerCrop()
                    .into(mAlbumImageView);
        }
    }

    // Move the player to the next track
    private void nextTrack() {
        if(mCurrentTrack < mTracks.size() - 1) {
            mCurrentTrack++;
        } else {
            mCurrentTrack = 0;
        }

        mProgressSeekBar.setProgress(0);

        updateTrack();
        mService.setCurrentTrack(mCurrentTrack);
    }

    // Move the player to the previous track
    private void previousTrack() {
        if (mCurrentTrack > 0) {
            mCurrentTrack--;
        } else {
            mCurrentTrack = mTracks.size() - 1;
        }

        updateTrack();
        mService.setCurrentTrack(mCurrentTrack);
    }

}
