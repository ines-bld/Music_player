package com.example.mymusicplayer;
import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class DownloadActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private EditText urlEditText;
    private Button downloadButton;
    private TextView statusTextView;
    private BroadcastReceiver downloadCompleteReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        urlEditText = findViewById(R.id.url);
        downloadButton = findViewById(R.id.btndownload);
        statusTextView = findViewById(R.id.txtStatus);

        statusTextView.setVisibility(View.GONE);

        downloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId != -1) {
                    String[] titleAndFilePath = getDownloadedFileTitleAndPath(downloadId);
                    if (titleAndFilePath != null) {
                        String title = titleAndFilePath[0];
                        String filePath = titleAndFilePath[1];

                    if (title != null) {
                        statusTextView.setVisibility(View.VISIBLE);
                        statusTextView.setText("Download complete. Title: " + title);
                        statusTextView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Pass the downloaded file title to MusicService
                                Intent intentD = new Intent();
                                intentD.setAction("PlayDownloadAudio");
                                intentD.putExtra("audio", title);
                                intentD.putExtra("path", filePath);
                                sendBroadcast(intentD);
                            }
                        });
                    }
                    } else {
                        statusTextView.setText("Download complete. Title retrieval failed.");
                    }
                } else {
                    statusTextView.setText("Download complete. Unknown title.");
                }
            }
        };

        registerReceiver(downloadCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlEditText.getText().toString();
                if (url.isEmpty()) {
                    Toast.makeText(DownloadActivity.this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (ContextCompat.checkSelfPermission(DownloadActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DownloadActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_CODE);
                } else {
                    startDownload(url);
                }
            }
        });
    }

    private void startDownload(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        String fileName = "audio_" + System.currentTimeMillis() + ".mp3";
        request.setTitle(fileName);
        request.setDescription("Downloading audio file");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);

        Toast.makeText(DownloadActivity.this, "Download started", Toast.LENGTH_SHORT).show();
    }

    private String[] getDownloadedFileTitleAndPath(long downloadId) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        Cursor cursor = downloadManager.query(query);
        if (cursor != null && cursor.moveToFirst()) {
            int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
            int filePathIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

            String title = cursor.getString(titleIndex);
            String filePath = cursor.getString(filePathIndex);

            cursor.close();

            // Remove "file://" prefix from the file path
            filePath = filePath.replace("file://", "");

            return new String[]{title, filePath};
        }

        return null;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String url = urlEditText.getText().toString();
                startDownload(url);
            } else {
                Toast.makeText(DownloadActivity.this, "Permission denied. Unable to download.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadCompleteReceiver);
    }

}
