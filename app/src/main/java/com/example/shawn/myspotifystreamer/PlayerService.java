package com.example.shawn.myspotifystreamer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Shawn on 7/8/2015.
 */
public class PlayerService extends Service implements MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final String LOG_TAG = PlayerService.class.getSimpleName();
    private final IBinder mPlayerBind = new PlayerBinder();

    private MediaPlayer mPlayer;
    private ArrayList<TrackHelper> mTracks;
    private int mCurrentTrack;

    public PlayerService() {
        super();
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Created");
        super.onCreate();
        mCurrentTrack = 0;
        mPlayer = new MediaPlayer();

        setupPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "Started");

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mPlayerBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        long position = getPosition();

        if(position > 0) {
            Intent stopIntent = new Intent("PLAYER-EVENT");
            stopIntent.putExtra("EVENT", "NEXT-TRACK");

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(stopIntent);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPlayer.start();
    }

    public void setupPlayer() {
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnCompletionListener(this);
    }

    public void setTracks(ArrayList<TrackHelper> tracks) {
        mTracks = tracks;
    }

    public void playTrack() {
        mPlayer.reset();

        TrackHelper track = mTracks.get(mCurrentTrack);
        String currentTrackUrl = track.getUrl();

        try {
            mPlayer.setDataSource(currentTrackUrl);
            mPlayer.prepareAsync();
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Error setting DataSource", ex);
        }
    }

    public void setCurrentTrack(int trackIndex) {
        mCurrentTrack = trackIndex;

        if(isPlaying()) {
            playTrack();
        }
    }

    public void pauseTrack() {
        mPlayer.pause();
    }

    public void resumeTrack() {
        mPlayer.start();
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public int getPosition() {
        return mPlayer.getCurrentPosition();
    }

    public int getDuration() { return mPlayer.getDuration(); }

    public void seekPosition(int position){
        mPlayer.seekTo(position);
    }

    public class PlayerBinder extends Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }

}
