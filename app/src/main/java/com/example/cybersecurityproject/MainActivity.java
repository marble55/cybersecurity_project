package com.example.cybersecurityproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.cybersecurityproject.databinding.ActivityMainBinding;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import static android.content.ContentValues.TAG;
import androidx.annotation.NonNull;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafeBrowsingThreat;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private TextInputEditText inputText;
    private Button buttonNextActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
//        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        Button checkButton = findViewById(R.id.buttonCheckThreat);
        inputText = findViewById(R.id.urlTextInput);
        buttonNextActivity = findViewById(R.id.buttonSwitchActivity);

        // Handle the intent that started the activity
        handleIntent(getIntent());

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = String.valueOf(inputText.getText());

                if(url.isEmpty()){
                    Toast.makeText(MainActivity.this, "Please input an URL", Toast.LENGTH_LONG).show();
                } else {
                    checkUrlThreat(url);
                }
            }
        });

        buttonNextActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ImageAnalyzerActivity.class);
                startActivity(intent);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        initSafeBrowsing();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        destroySafeBrowsingSession();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the current intent
        handleIntent(intent); // Re-handle the intent to update sentText
    }

    //Intent Handling
    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            String type = intent.getType();
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

                if (sharedText != null) {
                    // Log the shared text
                    Log.d("IntentSharedText", "Received shared text: " + sharedText);
                    handleSharedText(sharedText);
                } else {
                    Log.d("IntentSharedText", "No text found in the intent");
                }
            } else {
                Log.d("IntentSharedText", "Unsupported MIME type: " + type);
            }
        } else {
            Log.d("IntentSharedText", "Intent action is not ACTION_SEND");
        }
    }

    public void handleSharedText(String sharedText){
        if (sharedText.isEmpty()) {
            Toast.makeText(MainActivity.this, "No shared link detected!", Toast.LENGTH_LONG).show();
            Log.d("SharedText", "No shared link detected");
        } else {
            checkUrlThreat(sharedText);
            Log.d("SharedText", "Shared Link: " + sharedText);
        }
    }


//  Safe Browsing Functions
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
                                    Toast.makeText(getApplicationContext(), "No Threats Found for " + url +"!\nRating: " + rating, Toast.LENGTH_LONG).show();
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
                                            "Threats Found for " + url + "!\nRating: " + rating + "/5\nDetails: " + threatDetails,
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