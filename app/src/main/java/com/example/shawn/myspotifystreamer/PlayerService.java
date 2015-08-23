package com.example.shawn.myspotifystreamer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Shawn on 7/8/2015.
 */
public class PlayerService extends Service implements MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final String LOG_TAG = PlayerService.class.getSimpleName();
    private final IBinder mPlayerBind = new PlayerBinder();

    private final static int TIMER_INTERVAL = 1000;

    public final static String INTENT_TAG = "PLAYER-EVENT";
    public final static String EVENT_TAG = "EVENT";
    public final static String DURATION_TAG = "DURATION";
    public final static String POSTION_TAG = "POSITION";
    public final static String PLAYING_TAG = "PLAYING";
    public final static String TRACK_TAG = "TRACK";
    public final static String TRACK_NAME_TAG = "TRACK-NAME-TAG";
    public final static String TRACK_ARTIST_TAG = "TRACK-ARTIST-TAG";

    // Player Events
    public final static String TRACK_CHANGE = "TRACK-CHANGE";
    public final static String TRACK_PREPARED = "TRACK-PREPARED";
    public final static String TRACK_STATUS = "TRACK-PROGRESS";
    public final static String TRACK_PLAYING = "TRACK-PLAYING";
    public final static String TRACK_PAUSED = "TRACK-PAUSED";

    public final static String REMOTE_INTENT_TAG = "com.example.shawn.myspotifystreamer.REMOTE-EVENT";

    // Remote Control Events
    public final static String REMOTE_PLAY = "REMOTE-PLAY";
    public final static String REMOTE_NEXT = "REMOTE-NEXT";
    public final static String REMOTE_PREVIOUS = "REMOTE-PREVIOUS";

    private MediaPlayer mPlayer;
    private ArrayList<TrackHelper> mTracks;
    private int mCurrentTrack;

    private Thread statusUpdateThread;

    public PlayerService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCurrentTrack = 0;
        mPlayer = new MediaPlayer();

        setupPlayer();

        registerReceiver(mMessageReceiver, new IntentFilter(REMOTE_INTENT_TAG));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra(EVENT_TAG);

            switch (event) {
                case REMOTE_PLAY:
                    if (isPlaying()) {
                        pauseTrack();
                    } else {
                        resumeTrack();
                    }
                    break;
                case REMOTE_NEXT:
                    nextTrack();
                    break;
                case REMOTE_PREVIOUS:
                    previousTrack();
                    break;
            }
        }
    };

    public class PlayerBinder extends Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mPlayerBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancelAll();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        long position = mPlayer.getCurrentPosition();

        if(position > 0) {
            nextTrack();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPlayer.start();

        Intent intent = new Intent(INTENT_TAG)
                .putExtra(EVENT_TAG, TRACK_PREPARED)
                .putExtra(DURATION_TAG, mPlayer.getDuration());

        sendPlayerIntent(intent);

        statusUpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread currentThread = Thread.currentThread();

                while (statusUpdateThread == currentThread) {
                    if (mPlayer.isPlaying()) {
                        Intent intent = new Intent(INTENT_TAG)
                                .putExtra(EVENT_TAG, TRACK_STATUS)
                                .putExtra(POSTION_TAG, mPlayer.getCurrentPosition())
                                .putExtra(PLAYING_TAG, mPlayer.isPlaying());

                        sendPlayerIntent(intent);

                        TrackHelper track = mTracks.get(mCurrentTrack);

                        Intent startedIntent = new Intent(INTENT_TAG)
                                .putExtra(EVENT_TAG, TRACK_PLAYING)
                                .putExtra(TRACK_TAG, track.getUrl())
                                .putExtra(TRACK_NAME_TAG, track.getName())
                                .putExtra(TRACK_ARTIST_TAG, track.getArtistName());

                        sendPlayerIntent(startedIntent);
                    }

                    startNotification(mTracks.get(mCurrentTrack));

                    try {
                        Thread.sleep(TIMER_INTERVAL);
                    } catch (InterruptedException ex) {
                        Log.e(LOG_TAG, getString(R.string.error_putting_thread_to_sleep) + ex.getMessage());
                        break;
                    }
                }
            }
        });

        statusUpdateThread.start();
    }

    // Setups the the player
    public void setupPlayer() {
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnCompletionListener(this);
    }

    // Provides a list of the tracks assigned to the service
    public ArrayList<TrackHelper> getTracks() {
        return mTracks;
    }

    // Loads the artists track into the service
    public void setTracks(ArrayList<TrackHelper> tracks) {
        mTracks = tracks;
    }

    // Loads the current track and prepares it for playing
    public void playTrack() {
        mPlayer.reset();

        TrackHelper track = mTracks.get(mCurrentTrack);
        String currentTrackUrl = track.getUrl();

        try {
            mPlayer.setDataSource(currentTrackUrl);
            mPlayer.prepareAsync();
        } catch (IOException ex) {
            Log.e(LOG_TAG, getString(R.string.error_setting_datasource), ex);
        }
    }

    // Moves the player to the next track
    public void nextTrack() {
        if (mCurrentTrack < mTracks.size() - 1) {
            mCurrentTrack++;
        } else {
            mCurrentTrack = 0;
        }

        Intent intent = new Intent(INTENT_TAG)
                .putExtra(EVENT_TAG, TRACK_CHANGE)
                .putExtra(TRACK_TAG, mCurrentTrack);

        setCurrentTrack(mCurrentTrack);

        sendPlayerIntent(intent);
    }

    // Moves the player to the previous track
    public void previousTrack() {
        if (mCurrentTrack > 0) {
            mCurrentTrack--;
        } else {
            mCurrentTrack = mTracks.size() - 1;
        }

        Intent intent = new Intent(INTENT_TAG)
                .putExtra(EVENT_TAG, TRACK_CHANGE)
                .putExtra(TRACK_TAG, mCurrentTrack);

        setCurrentTrack(mCurrentTrack);

        sendPlayerIntent(intent);
    }

    // Pauses the player
    public void pauseTrack() {
        mPlayer.pause();

        Intent intent = new Intent(INTENT_TAG)
                .putExtra(EVENT_TAG, TRACK_PAUSED);

        sendPlayerIntent(intent);
    }

    // Provides the index of the current track
    public int getCurrentTrack () {
        return mCurrentTrack;
    }

    // Provides the url of the current track
    public String getCurrentTrackUrl () {
        return mTracks.get(mCurrentTrack).getUrl();
    }

    // Changes the current track and loads its for playing
    public void setCurrentTrack(int trackIndex) {
        mCurrentTrack = trackIndex;

        playTrack();
    }

    // Resumes the playing of the current track
    public void resumeTrack() {
        mPlayer.start();
    }

    // Returns whether player is playing
    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    // Returns whether player is paused
    public boolean isPaused() {
        return !mPlayer.isPlaying() && mPlayer.getCurrentPosition() > 0;
    }

    // Advances the track to the specified position
    public void seekPosition(int position){
        mPlayer.seekTo(position);
    }

    // Provides the current player duration
    public int getDuration() {
        return mPlayer.getDuration();
    }

    // Provides the current player position
    public int getPosition() {
        return mPlayer.getCurrentPosition();
    }

    // Helper function to broadcast a player intent
    private void sendPlayerIntent(Intent intent) {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    // Creates a Notification.Action object based on the supplied action and request code
    private NotificationCompat.Action generateAction(int icon, String title, String intentAction,
                                                     int requestCode) {
        Intent intent = new Intent(REMOTE_INTENT_TAG)
                .putExtra(EVENT_TAG, intentAction);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    // Build the media player notification
    private void startNotification(TrackHelper track) {
        Context context = getApplicationContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if (!displayNotifications) {
            return;
        }

        Bitmap albumArt = null;

        try {
            albumArt = Picasso.with(getApplicationContext()).load(track.getAlbumImage())
                    .resizeDimen(R.dimen.notification_album_image_width,
                            R.dimen.notification_album_image_height)
                    .get();

        } catch (IOException ex) {
            Log.e(LOG_TAG, getString(R.string.error_unable_to_load_album_art) + ex.toString());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(track.getArtistName())
                .setContentText(track.getName())
                .setOngoing(true);

        if (albumArt != null) {
            builder.setLargeIcon(albumArt);
        }

        builder.addAction(generateAction(android.R.drawable.ic_media_previous,
                getString(R.string.notification_action_title_previous), REMOTE_PREVIOUS, 0));
        builder.addAction(generateAction(
                isPlaying() ? android.R.drawable.ic_media_pause :
                        android.R.drawable.ic_media_play,
                isPlaying() ? getString(R.string.notification_action_title_pause) :
                        getString(R.string.notification_action_title_play),
                REMOTE_PLAY, 1));
        builder.addAction(generateAction(android.R.drawable.ic_media_next,
                getString(R.string.notification_action_title_next), REMOTE_NEXT, 2));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(1, builder.build());
    }

}
