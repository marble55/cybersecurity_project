package com.example.cybersecurityproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class LinkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the URL from the intent
        Uri data = intent.getData();
        if (data != null) {
            String url = data.toString();  // This is the URL that was opened
            Log.d("Captured URL", url);

            // For testing, log the URL to ensure it's being captured
            Toast.makeText(context, "Captured URL: " + url, Toast.LENGTH_LONG).show();
        }
    }
}
