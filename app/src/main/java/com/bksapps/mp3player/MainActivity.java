package com.bksapps.mp3player;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {

    private static final int REQUEST_CODE=3456;
    private ArrayList<Song> songList;
    ListView songView;

    MusicService mMusicService;
    private Intent playIntent;
    private Boolean musicBound=false;
    private MusicController controller;
    private boolean paused=false, playbackPaused=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView= (ListView)findViewById(R.id.song_list);
        songList=new ArrayList<Song>();
        checkUserPermission();

        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        setController();

    }

    private void setController(){
        //Set the controller up
        controller= new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    //play next
    private void playNext(){
        mMusicService.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    //play previous
    private void playPrev(){
        mMusicService.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private ServiceConnection musicConnection= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder=(MusicService.MusicBinder)service;
            //getService
            mMusicService=binder.getService();
            //pass list
            mMusicService.setList(songList);
            musicBound=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound=false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent=new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    public void songPicked(View view){
        mMusicService.setSong(Integer.parseInt(view.getTag().toString()));
        mMusicService.playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    public void checkUserPermission(){
        if(Build.VERSION.SDK_INT>=23){
            //Ask permission
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
            }else{
                getSongList();
            }
        }else{
            getSongList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode== REQUEST_CODE && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            getSongList();
        }else{
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            checkUserPermission();
        }
    }

    public void getSongList(){
        ContentResolver musicResolver= getContentResolver();
        Uri musicUri= MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor= musicResolver.query(musicUri, null, null, null, null);
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //getColumns
            int titleColumn=musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn= musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int idColumn= musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);

            //add Songs to List
            do{

                long thisId=musicCursor.getLong(idColumn);
                String thisTitle=musicCursor.getString(titleColumn);
                String thisArtist= musicCursor.getString(artistColumn);
                songList.add(new Song(thisId,thisTitle,thisArtist));
            }while(musicCursor.moveToNext());

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                //shuffle
                mMusicService.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                mMusicService=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        mMusicService=null;
        super.onDestroy();
    }

    @Override
    public void start() {
        mMusicService.go();
    }

    @Override
    public void pause() {
        playbackPaused=true;
        mMusicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(mMusicService!=null && musicBound && mMusicService.isPng()){
            return mMusicService.getDur();
        }else {
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        if(mMusicService!=null && musicBound && mMusicService.isPng()){
            return mMusicService.getPosn();
        }else{
            return 0;
        }

    }

    @Override
    public void seekTo(int pos) {
        mMusicService.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(mMusicService!=null && musicBound){
            return mMusicService.isPng();
        }else {
            return false;
        }
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
    }
    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }
}
