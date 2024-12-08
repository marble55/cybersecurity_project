package com.example.cybersecurityproject;

import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AIStudioHelper {
    private final GenerativeModel model;
    private String[] resultText;

    public AIStudioHelper(){
        model = new GenerativeModel(
                "gemini-1.5-flash-001",
                "AIzaSyANHjt9fcQ35VNw6WI-r-FEOemKj31vACk"
        );
    }


    public String[] getResultText(){
        return this.resultText;
    }
    public String promptGenerate(String imageContent) {
        GenerativeModelFutures gm = GenerativeModelFutures.from(model);
        String result = null;  // Initialize the result

        Content content = new Content.Builder()
                .addText("Analyze the message and give a rating from 1 to 10 on how likely it is a phishing scam:")
                .addText(imageContent)
                .build();

        ListenableFuture<GenerateContentResponse> response = gm.generateContent(content);

        Executor executor = Executors.newFixedThreadPool(4);

        // Add the callback to handle the response asynchronously
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                // Set the result inside the callback
                Log.d("AIModel", "Response Success!");
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("AIModel", "Response Failure" + t.toString());
                t.printStackTrace();
            }
        }, executor);

        try {
            // Block until the result is available
            result = response.get(30, TimeUnit.SECONDS).getText();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return result;  // Return the result once it is available
    }

}
