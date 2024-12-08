package com.example.cybersecurityproject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cybersecurityproject.databinding.ActivityImageanalyzerBinding;
import com.example.cybersecurityproject.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageAnalyzerActivity extends AppCompatActivity {


    private static final String TAG = "OCRIntegration";
    private static final String API_KEY = "K84632435688957"; // Replace with your OCR.Space API key

    private ActivityImageanalyzerBinding binding;
    private Button buttonReturn;
    private Button buttonAnalyzeImage;
    private TextView responseDescription;

    // Launcher to handle image selection result
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        String imagePath = null;
                        try {
                            imagePath = saveUriToFile(this, selectedImageUri);
                        } catch (IOException e) {
                            Log.e("ImagePath", Objects.requireNonNull(e.getMessage()));
                            throw new RuntimeException(e);
                        }
                        Log.d("ImagePath", imagePath);
                        if (imagePath != null) {
                            updateResponseDescription("Loading...");
                            performOCR(imagePath);
                        } else {
                            Toast.makeText(this, "Unable to get image path", Toast.LENGTH_SHORT).show();
                            updateResponseDescription("Unable to get image path");
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityImageanalyzerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        buttonAnalyzeImage = findViewById(R.id.buttonImageAnalyze);
        buttonReturn = findViewById(R.id.buttonSwitchActivity);
        responseDescription = findViewById(R.id.responseTextView);

        buttonAnalyzeImage.setOnClickListener(new View.OnClickListener() {
            // Launch the gallery when the activity starts
            @Override
            public void onClick(View v) {
                 openGallery();
            }
        });

        buttonReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void updateResponseDescription(String response) {
        runOnUiThread(() -> responseDescription.setText(response));
    }


    private void performOCR(String imagePath) {
        OkHttpClient client = new OkHttpClient();

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            Toast.makeText(this, "Image file not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/png")))
                .addFormDataPart("apikey", API_KEY)
                .addFormDataPart("language", "eng") // Optional: Set language code
                .build();

        Request request = new Request.Builder()
                .url("https://api.ocr.space/parse/image")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "OCR request failed", e);
                runOnUiThread(() ->
                        Toast.makeText(ImageAnalyzerActivity.this, "OCR request failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                updateResponseDescription("Failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        String formattedResponse = extractAndFormatText(responseData);

                        runOnUiThread(() -> updateResponseDescription(formattedResponse));
                    } else {
                        runOnUiThread(() -> Toast.makeText(ImageAnalyzerActivity.this, "OCR request failed: " + response.code(), Toast.LENGTH_SHORT).show());
                    }
                } finally {
                    // Delete the temporary file
                    if (imageFile.exists()) {
                        imageFile.delete();
                    }
                }
            }
        });
    }

    private String saveUriToFile(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        File tempFile = new File(context.getCacheDir(), "temp_image.png");
        OutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.close();
        inputStream.close();

        return tempFile.getAbsolutePath();
    }
    private String extractAndFormatText(String jsonResponse) {
        StringBuilder formattedText = new StringBuilder();
        try {
            // Parse JSON
            JSONObject rootObject = new JSONObject(jsonResponse);
            JSONArray parsedResults = rootObject.getJSONArray("ParsedResults");

            // Extract ParsedText
            for (int i = 0; i < parsedResults.length(); i++) {
                JSONObject result = parsedResults.getJSONObject(i);
                String parsedText = result.getString("ParsedText");

                // Format the ParsedText
                String[] lines = parsedText.split("\r\n");
                for (String line : lines) {
                    // Clean up or add custom formatting logic if needed
                    formattedText.append(line.trim()).append("\n\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return formattedText.toString().trim();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (responseDescription != null) {
            outState.putString("descriptionText", responseDescription.getText().toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("descriptionText")) {
            updateResponseDescription(savedInstanceState.getString("descriptionText"));
        }
    }


}
