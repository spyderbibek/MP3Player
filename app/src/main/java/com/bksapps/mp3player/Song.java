package com.bksapps.mp3player;

/**
 * Created by spyde on 7/26/2017.
 */

public class Song {

    private long id;
    private String title;
    private String artist;

    public Song() {
    }

    public Song(long id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }
}
