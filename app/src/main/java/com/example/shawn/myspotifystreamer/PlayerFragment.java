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

    private String TRACK_BUNDLE = "TRACK_BUNDLE";
    private String CURRENT_TRACK_EXTRA = "CURRENT_TRACK";
    private String TRACK_LIST_EXTRA = "TRACK_LIST";
    private String IS_PLAYING_EXTRA = "IS_PLAYING";
    private String IS_PAUSED_EXTRA = "IS_PAUSED";

    private int mCurrentTrack = 0;
    private ArrayList<TrackHelper> mTracks;

    private PlayerService mService;
    private boolean mPlayerBound = false;
    private boolean mIsPlaying = false;
    private boolean mIsPaused = false;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter("PLAYER-EVENT"));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String event = intent.getStringExtra("EVENT");
            if(event == "NEXT-TRACK") {
                nextTrack();
                mService.playTrack();
            }
        }
    };

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
            mIsPlaying = savedInstanceState.getBoolean(IS_PLAYING_EXTRA);
            mIsPaused = savedInstanceState.getBoolean(IS_PAUSED_EXTRA);

            resetSeekBar();
            updateSeekBar();
        } else {
            if (arguments != null) {
                mCurrentTrack = arguments.getInt(CURRENT_TRACK_EXTRA);
                mTracks = arguments.getParcelableArrayList(TRACK_LIST_EXTRA);
            } else if (sourceIntent != null && sourceIntent.hasExtra(TRACK_BUNDLE)) {
                Bundle bundleArguments = sourceIntent.getBundleExtra(TRACK_BUNDLE);
                mCurrentTrack = bundleArguments.getInt(CURRENT_TRACK_EXTRA, 0);
                mTracks = bundleArguments.getParcelableArrayList(TRACK_LIST_EXTRA);
            }
        }

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTrack();
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
                    mService.seekPosition(progress);
                }

                mElapsedText.setText(Utils.formatMillisecondsAsTime(progress));
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
        mProgressSeekBar.removeCallbacks(onEverySecond);
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

            mService.setTracks(mTracks);

            updateTrack();

            mPlayerBound = true;

            if(!mIsPlaying && !mIsPaused) {
                playTrack();
            }

            updatePlayButton();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPlayerBound = false;
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(CURRENT_TRACK_EXTRA, mCurrentTrack);
        outState.putParcelableArrayList(TRACK_LIST_EXTRA, mTracks);
        outState.putBoolean(IS_PLAYING_EXTRA, mIsPlaying);
        outState.putBoolean(IS_PAUSED_EXTRA, mIsPaused);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    private void updateTrack() {
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

    private void playTrack() {
        if (mService != null && !mIsPlaying) {
            if(!mIsPaused) {
                mService.setCurrentTrack(mCurrentTrack);
                mService.playTrack();

                updateSeekBar();
            } else {
                mService.resumeTrack();
            }

            mIsPlaying = true;
            mIsPaused = false;

            updatePlayButton();
        } else {
            mService.pauseTrack();
            mIsPlaying = false;
            mIsPaused = true;

            updatePlayButton();
        }
    }

    private void nextTrack() {
        if(mCurrentTrack < mTracks.size() - 1) {
            mCurrentTrack++;
        } else {
            mCurrentTrack = 0;
        }

        resetSeekBar();
        updateTrack();
        mService.setCurrentTrack(mCurrentTrack);

        if(mIsPlaying) {
            updateSeekBar();
        }

        if(!mService.isPlaying()) {
            mIsPaused = false;
        }
    }

    private void previousTrack() {
        if (mCurrentTrack > 0) {
            mCurrentTrack--;
        } else {
            mCurrentTrack = mTracks.size() - 1;
        }

        resetSeekBar();
        updateTrack();
        mService.setCurrentTrack(mCurrentTrack);

        if (mIsPlaying) {
            updateSeekBar();
        }

        if (!mService.isPlaying()) {
            mIsPaused = false;
        }
    }

    private void updateSeekBar() {
        mProgressSeekBar.setProgress(0);

        mElapsedText.setText(getString(R.string.default_elapsed_text));
        mDurationText.setText(getString(R.string.default_elapsed_text));

        mProgressSeekBar.postDelayed(onEverySecond, 1000);
    }

    private void resetSeekBar() {
        mProgressSeekBar.setMax(0);
        mProgressSeekBar.setProgress(0);
        mElapsedText.setText(getString(R.string.default_elapsed_text));
        mDurationText.setText(getString(R.string.default_elapsed_text));
        mProgressSeekBar.removeCallbacks(onEverySecond);
    }

    private void updatePlayButton() {
        if(mIsPlaying) {
            mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            mPlayButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private Runnable onEverySecond = new Runnable() {
        @Override
        public void run(){
            if(mService != null) {
                if(mService.isPlaying()) {
                    mProgressSeekBar.setMax(mService.getDuration());
                    mProgressSeekBar.setProgress(mService.getPosition());

                    mDurationText.setText(Utils.formatMillisecondsAsTime(mService.getDuration()));
                }
            }

            mProgressSeekBar.postDelayed(onEverySecond, 1000);
        }
    };

}
