package com.example.cybersecurityproject;

import static android.content.ContentValues.TAG;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafeBrowsingThreat;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class LinkCheckerActivity extends AppCompatActivity  {
    private WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linkchecker);

    }

    @Override
    protected void onResume() {
        super.onResume();
        initSafeBrowsing();
    }

    public void initSafeBrowsing(){
        new Thread(() -> {
            try {
                Tasks.await(SafetyNet.getClient(getApplicationContext()).initSafeBrowsing());
                Log.d("TAG", "SafetyNet initialized successfully.");
            } catch (ExecutionException | InterruptedException e) {
                Log.e("TAG", "Error initializing SafetyNet: " + e.getMessage());
            }
        }).start();
    }

    public void destroySafeBrowsingSession(){
        SafetyNet.getClient(this).shutdownSafeBrowsing();
    }

    public void checkUrlThreat(String url){
        SafetyNet.getClient(this).lookupUri(url,
                        "AIzaSyCtYLMLNa_XoZKXU0nn9mYWMDhFyd0DBiA",
                        SafeBrowsingThreat.TYPE_POTENTIALLY_HARMFUL_APPLICATION,
                        SafeBrowsingThreat.TYPE_SOCIAL_ENGINEERING)
                .addOnSuccessListener(this,
                        new OnSuccessListener<SafetyNetApi.SafeBrowsingResponse>() {
                            @Override
                            public void onSuccess(SafetyNetApi.SafeBrowsingResponse sbResponse) {
                                // Indicates communication with the service was successful.
                                // Identify any detected threats.
                                if (sbResponse.getDetectedThreats().isEmpty()) {
                                    // No threats found
                                    String rating = "Safe (5/5)";
                                    Toast.makeText(getApplicationContext(), "No Threats Found! Rating: " + rating, Toast.LENGTH_LONG).show();
                                } else {
                                    // Threats found
                                    List<SafeBrowsingThreat> detectedThreats = sbResponse.getDetectedThreats();
                                    int rating = calculateRating(detectedThreats);

                                    StringBuilder threatDetails = new StringBuilder();
                                    for (SafeBrowsingThreat threat : detectedThreats) {
                                        threatDetails.append(threat.getThreatType() == SafeBrowsingThreat.TYPE_SOCIAL_ENGINEERING
                                                ? "Social Engineering"
                                                : "Potentially Harmful Application");
                                        threatDetails.append("\n");
                                    }

                                    Toast.makeText(getApplicationContext(),
                                            "Threats Found! Rating: " + rating + "/5\nDetails: " + threatDetails,
                                            Toast.LENGTH_LONG).show();
                                    Log.d("TAG", "Threats detected: " + threatDetails);
                                }
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // An error occurred while communicating with the service.
                        if (e instanceof ApiException) {
                            // An error with the Google Play Services API contains some
                            // additional details.
                            ApiException apiException = (ApiException) e;
                            Log.d(TAG, "Error: " + CommonStatusCodes
                                    .getStatusCodeString(apiException.getStatusCode()));

                            // Note: If the status code, apiException.getStatusCode(),
                            // is SafetyNetStatusCode.SAFE_BROWSING_API_NOT_INITIALIZED,
                            // you need to call initSafeBrowsing(). It means either you
                            // haven't called initSafeBrowsing() before or that it needs
                            // to be called again due to an internal error.
                        } else {
                            // A different, unknown type of error occurred.
                            Log.d(TAG, "Error: " + e.getMessage());
                        }
                    }
                });
    }

    // Custom method to calculate rating based on threats
    private int calculateRating(List<SafeBrowsingThreat> detectedThreats) {
        int rating = 5; // Start with max rating

        for (SafeBrowsingThreat threat : detectedThreats) {
            switch (threat.getThreatType()) {
                case SafeBrowsingThreat.TYPE_SOCIAL_ENGINEERING:
                    rating -= 2; // Reduce for social engineering threats
                    break;
                case SafeBrowsingThreat.TYPE_POTENTIALLY_HARMFUL_APPLICATION:
                    rating -= 3; // Reduce for potentially harmful applications
                    break;
                default:
                    break;
            }
        }
        return Math.max(rating, 1); // Ensure the minimum rating is 1
    }
}

