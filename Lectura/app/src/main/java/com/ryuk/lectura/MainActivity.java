package com.ryuk.lectura;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private Uri selectedFileUri;
    private Button btnViewContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSelectFile = findViewById(R.id.btnSelectFile);
        btnViewContent = findViewById(R.id.btnViewContent);

        btnSelectFile.setOnClickListener(v -> startActivityForResult(
                new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("text/plain"), 1));

        btnViewContent.setOnClickListener(v -> startActivity(
                new Intent(this, ContentActivity.class)
                        .putExtra("fileUri", selectedFileUri.toString())));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        selectedFileUri = data.getData();
        ((TextView)findViewById(R.id.tvSelectedFile)).setText("Archivo: " + selectedFileUri.getLastPathSegment());
        btnViewContent.setEnabled(true);
    }
}