package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int FILE_PICKER_REQUEST_CODE = 102;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button1 = findViewById(R.id.button1);
        Button button2 = findViewById(R.id.button2);
        Button button3 = findViewById(R.id.openPdfButton);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPromptDialog("Button 1 Clicked");
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPromptDialog("Button 2 Clicked");
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });



    }


    private void showPromptDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void openFilePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openFilePickerIntent();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        } else {
            openFilePickerIntent();
        }
    }

    private void openFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Set the MIME type to select any file type
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePickerIntent();
            } else {
                // Handle the case where the user denies the permission
                showPromptDialog("Behn Ke lode, agree karna tha");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri selectedFileUri = data.getData();
            openSelectedFile(selectedFileUri);
        }
    }

    private void openSelectedFile(Uri fileUri) {
        String displayName = getDisplayName(fileUri);
        String filePath = getCacheDir() + "/" + displayName;

        try {
            InputStream in = getContentResolver().openInputStream(fileUri);
            FileOutputStream out = new FileOutputStream(filePath);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();

            File file = new File(filePath);

            if (file.exists()) {
                Uri cachedFileUri = FileProvider.getUriForFile(this, "com.example.myapplication.fileprovider", file);

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(cachedFileUri, getMimeType(filePath));

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // Handle the case where there is no suitable viewer app installed
                    // Log a message for debugging
                    Log.e("MyApp", "No suitable viewer app installed for the file: " + filePath);
                }
            } else {
                // Handle the case where the file does not exist
                // Log a message for debugging
                Log.e("MyApp", "File does not exist: " + filePath);
            }
        } catch (Exception e) {
            // Handle exceptions
            e.printStackTrace();
            // Log a message for debugging
            Log.e("MyApp", "Error opening file: " + e.getMessage());
        }
    }


    private String getDisplayName(Uri uri) {
        String displayName = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                displayName = cursor.getString(nameIndex);
            }
        }
        return displayName;
    }

    private String getMimeType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1);
        String mimeType;

        if (extension.equalsIgnoreCase("pdf")) {
            mimeType = "application/pdf";
        } else if (extension.equalsIgnoreCase("png")) {
            mimeType = "image/png";
        } else {
            mimeType = "*/*";
        }

        return mimeType;
    }
}
