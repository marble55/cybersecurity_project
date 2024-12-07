package com.example.cybersecurityproject;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.SafeBrowsingResponse;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewCompat;

public class LinkCheckerActivity extends AppCompatActivity  {
    private WebView webView;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linkchecker);

        WebViewCompat.stpartSafeBrowsing(this, success -> {
            if (!success) {
                Log.e("SafeBrowsing", "Unable to initialize Safe Browsing!");
            }
        });

        webView = findViewById(R.id.webView);
        webView.getSettings().setSafeBrowsingEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @RequiresApi(api = Build.VERSION_CODES.O_MR1)
            @Override
            public void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType, SafeBrowsingResponse callback) {
                // Automatically go back to safety when encountering a threat
                callback.backToSafety(true);
            }
        });

        webView.loadUrl("https://example.com");
    }
}

