package com.example.mymusicplayer;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;

public class FavoriteSongsActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_songs);

        listView = findViewById(R.id.listView);
        databaseHelper = new DatabaseHelper(this);

        // Retrieve favorite songs titles from the database
        ArrayList<String> favoriteSongsTitles = databaseHelper.getFavoriteSongsTitles();

        // Create and set the adapter for the ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, favoriteSongsTitles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedTitle = (String) parent.getItemAtPosition(position);
                // Send the selected title back to MainActivity
                Intent intent = new Intent();
                intent.setAction("PlaySelectedSong");
                intent.putExtra("song", selectedTitle);
                sendBroadcast(intent);
            }
        });
    }
}
