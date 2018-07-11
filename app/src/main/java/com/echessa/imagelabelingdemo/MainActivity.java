package com.echessa.imagelabelingdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_PHOTO_REQUEST_CODE = 100;
    private static final int ASK_PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = MainActivity.class.getName();

    private TextView mTextView;
    private ImageView mImageView;
    private View mLayout;

    private FirebaseVisionLabelDetector mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextView = findViewById(R.id.textView);
        mImageView = findViewById(R.id.imageView);
        mLayout = findViewById(R.id.main_layout);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermissions();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                mImageView.setImageBitmap(bitmap);
                mTextView.setText("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case ASK_PERMISSION_REQUEST_CODE: {
                // If permission request is cancelled, grantResults array will be empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    openGallery();
                } else {
                    // Permission denied. Handle appropriately e.g. you can disable the
                    // functionality that depends on the permission.
                }
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDetector != null) {
            try {
                mDetector.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception thrown while trying to close Image Labeling Detector: " + e);
            }
        }
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
        } else if (id == R.id.action_process) {
            processImage();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user of why permission is needed
                Snackbar.make(mLayout, R.string.storage_access_required,
                        Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Request the permission
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                                ASK_PERMISSION_REQUEST_CODE);
                    }
                }).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        ASK_PERMISSION_REQUEST_CODE);
            }
        } else {
            // Permission has already been granted
            openGallery();
        }
    }

    private void openGallery() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO_REQUEST_CODE);
    }

    private void processImage() {
        if (mImageView.getDrawable() == null) {
            // ImageView has no image
            Snackbar.make(mLayout, R.string.select_image, Snackbar.LENGTH_SHORT).show();
        } else {
            // ImageView contains image
            Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            mDetector = FirebaseVision.getInstance().getVisionLabelDetector();
            mDetector.detectInImage(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<List<FirebaseVisionLabel>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionLabel> labels) {
                                    // Task completed successfully
                                    StringBuilder sb = new StringBuilder();
                                    for (FirebaseVisionLabel label : labels) {
                                        String text = label.getLabel();
                                        String entityId = label.getEntityId();
                                        float confidence = label.getConfidence();
                                        sb.append("Label: " + text + "; Confidence: " + confidence + "; Entity ID: " + entityId + "\n");
                                    }
                                    mTextView.setText(sb);
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    Log.e(TAG, "Image labelling failed " + e);
                                }
                            });
        }
    }
}
