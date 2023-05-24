package com.example.mymusicplayer;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.Manifest;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    MusicService musicService = new MusicService();
    private String receivedTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView play = findViewById(R.id.play);
        ImageView pause = findViewById(R.id.pause);
        ImageView next = findViewById(R.id.next);
        ImageView previous = findViewById(R.id.previous);
        ImageView fav = findViewById(R.id.fav);
        ImageView addFav = findViewById(R.id.addfav);
        TextView song = findViewById(R.id.song);

        pause.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
        previous.setVisibility(View.GONE);
        fav.setVisibility(View.GONE);
        addFav.setVisibility(View.GONE);
        song.setVisibility(View.GONE);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pause.setVisibility(View.VISIBLE);
                next.setVisibility(View.VISIBLE);
                previous.setVisibility(View.VISIBLE);
                play.setVisibility(View.GONE);
                song.setVisibility(View.VISIBLE);
                addFav.setVisibility(View.VISIBLE);
                if(checkPermission() == false){
                    requestPermission();
                    return;
                } else {
                    startService(new Intent(getApplicationContext(),
                            MusicService.class));
                }
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pause.setVisibility(View.GONE);
                next.setVisibility(View.GONE);
                previous.setVisibility(View.GONE);
                play.setVisibility(View.VISIBLE);
                song.setVisibility(View.GONE);
                addFav.setVisibility(View.GONE);
                fav.setVisibility(View.GONE);

                stopService(new
                        Intent(getApplicationContext(),
                        MusicService.class));
            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("Previous");
                sendBroadcast(intent);
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("Next");
                sendBroadcast(intent);
            }
        });
        addFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addFav.setVisibility(View.GONE);
                fav.setVisibility(View.VISIBLE);
                String currentSongTitle = receivedTitle; // Retrieve the title of the currently playing song
                DatabaseHelper databaseHelper = new DatabaseHelper(MainActivity.this);
                databaseHelper.addFavoriteSong(currentSongTitle);
                Toast.makeText(MainActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
            }
        });

        fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addFav.setVisibility(View.VISIBLE);
                fav.setVisibility(View.GONE);
                String currentSongTitle = receivedTitle;// Retrieve the title of the currently playing song
                // Remove the song title from the database
                DatabaseHelper databaseHelper = new DatabaseHelper(MainActivity.this);
                databaseHelper.removeFavoriteSong(currentSongTitle);
                Toast.makeText(MainActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
            }
        });

        // Register the receiver for the title updates
        IntentFilter intentFilter = new IntentFilter("TitleUpdate");
        LocalBroadcastManager.getInstance(this).registerReceiver(titleUpdateReceiver, intentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.fav:
                Intent intentfav = new Intent(MainActivity.this, FavoriteSongsActivity.class);
                startActivity(intentfav);
                return true;
            case R.id.download:
                Intent intent = new Intent(this, DownloadActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BroadcastReceiver titleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("TitleUpdate")) {
                receivedTitle = intent.getStringExtra("title");
                // Update your TextView with the received title
                TextView song = findViewById(R.id.song);
                song.setText(receivedTitle);

                ImageView fav = findViewById(R.id.fav);
                ImageView addFav = findViewById(R.id.addfav);
                DatabaseHelper databaseHelper = new DatabaseHelper(MainActivity.this);
                List<String> favoriteSongTitles = databaseHelper.getFavoriteSongsTitles();
                // Check if the current song title is in the favorite song titles list
                if (favoriteSongTitles.contains(receivedTitle)) {
                    // Show the "fav" ImageView and hide the "addfav" ImageView
                    fav.setVisibility(View.VISIBLE);
                    addFav.setVisibility(View.GONE);
                } else {
                    // Show the "addfav" ImageView and hide the "fav" ImageView
                    addFav.setVisibility(View.VISIBLE);
                    fav.setVisibility(View.GONE);
                }
            }
        }
    };

    boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(result == PackageManager.PERMISSION_GRANTED){
            return true;
        }else{
            return false;
        }
    }

    void requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)){
            Toast.makeText(MainActivity.this,"READ PERMISSION IS REQUIRED,PLEASE ALLOW FROM SETTINGS",Toast.LENGTH_SHORT).show();
        }else
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},123);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(titleUpdateReceiver);
        stopService(new
                Intent(getApplicationContext(),
                MusicService.class));
    }
}