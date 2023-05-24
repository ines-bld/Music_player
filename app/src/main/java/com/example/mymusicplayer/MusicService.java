package com.example.mymusicplayer;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener{
    private MyReceiver recv;
    private MediaPlayer player;
    private List<AudioItem> audioList;
    private int currentAudioIndex;
    public MusicService() {}

    @Override
    public IBinder onBind(Intent intent) {return null;}

    @Override
    public void onCreate(){
        recv = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("PlayPause");
        intentFilter.addAction("Next");
        intentFilter.addAction("Previous");
        registerReceiver(recv, intentFilter);
        player = new MediaPlayer();
        player.setOnCompletionListener(this);
        audioList = getAllAudioFromDevice();
        Log.d("MusicService", "Audio List: " + audioList.toString());
        // Register the playSelectedSongReceiver
        IntentFilter filter = new IntentFilter("PlaySelectedSong");
        registerReceiver(playSelectedSongReceiver, filter);
        // Register the playDownloadSongReceiver
        IntentFilter filterD = new IntentFilter("PlayDownloadAudio");
        registerReceiver(playDownloadSongReceiver, filterD);
        super.onCreate();
    }

    String channelId = "my_channel_id";
    CharSequence channelName = "My Channel";
    int importance = NotificationManager.IMPORTANCE_DEFAULT;
    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        FLAG_UPDATE_CURRENT);

        PendingIntent pendingIntent1 =
                PendingIntent.getBroadcast(this, 0, new Intent("PlayPause"),
                        FLAG_UPDATE_CURRENT);

        // Define the intent for the next button
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 1, new Intent("Next"),
                FLAG_UPDATE_CURRENT);

        // Define the intent for the previous button
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 2, new Intent("Previous"),
                FLAG_UPDATE_CURRENT);
        // Notification Channel
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new

                    NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(notificationChannel);}

        Notification notification =
                new NotificationCompat.Builder(this, channelId)
                        .setContentTitle("Lecture en cours")
                        .setContentText(audioList.get(currentAudioIndex).getTitle())
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .addAction(R.drawable.ic_launcher_background, "Previous", prevPendingIntent)
                        .addAction(R.drawable.ic_launcher_foreground, "PlayPause", pendingIntent1)
                        .addAction(R.drawable.ic_launcher_background, "Next", nextPendingIntent)
                        .setContentIntent(pendingIntent)
                        .setPriority(Notification.PRIORITY_MAX)
                        .build();
        startForeground(110, notification);
        if (audioList.size() > 0) {
            currentAudioIndex = 0;
            playAudio(audioList.get(currentAudioIndex).getFilePath());
        }
        return START_STICKY;
    }

    private void updateNotification(String songTitle) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        FLAG_UPDATE_CURRENT);

        PendingIntent pendingIntent1 =
                PendingIntent.getBroadcast(this, 0, new Intent("PlayPause"),
                        FLAG_UPDATE_CURRENT);

        // Define the intent for the next button
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 1, new Intent("Next"),
                FLAG_UPDATE_CURRENT);

        // Define the intent for the previous button
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 2, new Intent("Previous"),
                FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Lecture en cours")
                .setContentText(songTitle)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .addAction(R.drawable.ic_launcher_background, "Previous", prevPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "PlayPause", pendingIntent1)
                .addAction(R.drawable.ic_launcher_background, "Next", nextPendingIntent)
                .setSound(null)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        Notification notification = builder.build();
        startForeground(110, notification);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        currentAudioIndex++;
        if (currentAudioIndex < audioList.size()) {
            playAudio(audioList.get(currentAudioIndex).getFilePath());
        } else {
            stopSelf();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(player.isPlaying()) player.stop();
        unregisterReceiver(recv);
        unregisterReceiver(playSelectedSongReceiver);
        unregisterReceiver(playDownloadSongReceiver);
    }

    public class MyReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();

            switch (action) {
                case "PlayPause":
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        if (player.getCurrentPosition() > 0) {
                            player.start();
                        } else {
                            // If the player is not prepared, start playing the first audio
                            playAudio(audioList.get(currentAudioIndex).getFilePath());
                            sendTitleUpdateBroadcast(context, audioList.get(currentAudioIndex).getTitle());
                        }
                    }
                    break;
                case "Next":
                    // Play the next audio file
                    currentAudioIndex++;
                    if (currentAudioIndex >= audioList.size()) {
                        currentAudioIndex = 0;
                    }
                    playAudio(audioList.get(currentAudioIndex).getFilePath());
                    updateNotification(audioList.get(currentAudioIndex).getTitle());
                    sendTitleUpdateBroadcast(context, audioList.get(currentAudioIndex).getTitle());
                    break;
                case "Previous":
                    // Play the previous audio file
                    currentAudioIndex--;
                    if (currentAudioIndex < 0) {
                        currentAudioIndex = audioList.size() - 1;
                    }
                    playAudio(audioList.get(currentAudioIndex).getFilePath());
                    updateNotification(audioList.get(currentAudioIndex).getTitle());
                    sendTitleUpdateBroadcast(context, audioList.get(currentAudioIndex).getTitle());
                    break;
            }
        }
        private void sendTitleUpdateBroadcast(Context context, String title) {
            Intent updateIntent = new Intent("TitleUpdate");
            updateIntent.putExtra("title", title);
            LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent);
        }
    }

    private BroadcastReceiver playSelectedSongReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("PlaySelectedSong")) {
                String selectedTitle = intent.getStringExtra("song");
                //hadi f broadcast ta3 favorites
                Toast.makeText(context, selectedTitle, Toast.LENGTH_SHORT).show();
                playSelectedSong(selectedTitle);
            }
        }
    };

    private BroadcastReceiver playDownloadSongReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("PlayDownloadAudio")) {
                String selectedTitle = intent.getStringExtra("audio");
                String filePath = intent.getStringExtra("path");
                Toast.makeText(context, selectedTitle, Toast.LENGTH_SHORT).show();
                AudioItem audioItem = new AudioItem(selectedTitle, filePath);
                audioList.add(audioItem);
                playSelectedSong(selectedTitle);
            }
        }
    };

    public void playSelectedSong(String selectedTitle) {
        int selectedIndex = -1;
        for (int i = 0; i < audioList.size(); i++) {
            if (audioList.get(i).getTitle().equals(selectedTitle)) {
                selectedIndex = i;
                break;
            }
        }

        if (selectedIndex != -1) {
            currentAudioIndex = selectedIndex;
            playAudio(audioList.get(currentAudioIndex).getFilePath());
            updateNotification(audioList.get(currentAudioIndex).getTitle());
        }
    }

    private void playAudio(String audioFilePath) {
        try {
            if (player.isPlaying()) {
                player.stop();
                player.reset();
            }
            player.setDataSource(audioFilePath);
            // Update the title when playing a new audio
            String title = audioList.get(currentAudioIndex).getTitle();
            sendTitleUpdateBroadcast(title);
            player.prepareAsync();
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    player.start();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            // Log the error message
            Log.e("MediaPlayer", "Error setting data source: " + e.getMessage());
            // Handle the error, such as displaying an error message to the user
            Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Log.e("MediaPlayer", "Illegal argument: " + e.getMessage());
            Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.e("MediaPlayer", "Security exception: " + e.getMessage());
            Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e("MediaPlayer", "Illegal state: " + e.getMessage());
            Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
        }
    }
    private void sendTitleUpdateBroadcast(String title) {
        Intent updateIntent = new Intent("TitleUpdate");
        updateIntent.putExtra("title", title);
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);
    }

    private List<AudioItem> getAllAudioFromDevice() {
        List<AudioItem> audioList = new ArrayList<>();
        String[] projection = {MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE};
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);
        if (cursor != null) {
            int filePathIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            if (filePathIndex >= 0 && titleIndex >= 0) {
                while (cursor.moveToNext()) {
                    String audioFilePath = cursor.getString(filePathIndex);
                    String audioTitle = cursor.getString(titleIndex);
                    if (audioFilePath.endsWith(".mp3")) {
                        AudioItem audioItem = new AudioItem(audioTitle, audioFilePath);
                        audioList.add(audioItem);
                    }
                }
            }
            cursor.close();
        }
        return audioList;
    }

    public class AudioItem {
        private String title;
        private String filePath;

        public AudioItem(String title, String filePath) {
            this.title = title;
            this.filePath = filePath;
        }

        public String getTitle() {
            return title;
        }

        public String getFilePath() {
            return filePath;
        }
    }
}