package com.example.cybersecurityproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cybersecurityproject.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the ActivityResultLauncher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData(); // Get the image URI
                        binding.selectedImageView.setImageURI(imageUri); // Display the image
                    }
                }
        );

        binding.toastButton.setOnClickListener(v -> openImageSelector());

        binding.buttonFirst.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
        );

//        binding.toastButton.setOnClickListener(v -> {
//            String message = binding.textInput1.getText().toString();
//            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();}
//        );

    }

    @SuppressLint("IntentReset")
    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*"); // Show only images
        imagePickerLauncher.launch(intent); // Launch the image picker
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}