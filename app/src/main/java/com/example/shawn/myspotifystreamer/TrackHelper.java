package com.example.shawn.myspotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Shawn on 7/13/2015.
 */
// Lightweight helper for holding Track data. Parcelable for eventual interactivity storage
public class TrackHelper implements Parcelable {

    String mId;
    String mName;
    String mUrl;
    String mArtistName;
    String mAlbumName;
    String mAlbumImage;

    public TrackHelper(String id, String name, String url, String artistName, String albumName,
                       String albumImage) {
        mId = id;
        mName = name;
        mUrl = url;
        mArtistName = artistName;
        mAlbumName = albumName;
        mAlbumImage = albumImage;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public void setArtistName(String artistName) {
        mArtistName = artistName;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    public void setAlbumName(String albumName) {
        mAlbumName = albumName;
    }

    public String getAlbumImage() {
        return mAlbumImage;
    }

    public void setAlbumImage(String albumImage) {
        mAlbumImage = albumImage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mName);
        dest.writeString(mUrl);
        dest.writeString(mArtistName);
        dest.writeString(mAlbumName);
        dest.writeString(mAlbumImage);
    }

    public static final Parcelable.Creator<TrackHelper> CREATOR
            = new Parcelable.Creator<TrackHelper>() {

        @Override
        public TrackHelper createFromParcel(Parcel source) {
            return new TrackHelper(source);
        }

        @Override
        public TrackHelper[] newArray(int size) {
            return new TrackHelper[size];
        }
    };

    private TrackHelper(Parcel parcel) {
        mId = parcel.readString();
        mName = parcel.readString();
        mUrl = parcel.readString();
        mArtistName = parcel.readString();
        mAlbumName = parcel.readString();
        mAlbumImage = parcel.readString();
    }

}
