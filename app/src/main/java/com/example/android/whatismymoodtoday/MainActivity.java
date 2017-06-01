package com.example.android.whatismymoodtoday;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.android.whatismymoodtoday.databinding.ActivityMainBinding;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.Order;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements HandlingUi {

    static final int GET_FROM_GALLERY_REQUEST_CODE = 1; //it can be any number; needed for gallery request
    static final int GET_FROM_CAMERA_REQUEST_CODE = 2;
    //keys for hndling activity state
    static final String BITMAP_KEY = "bitmap key";
    static final String RESPONSE_KEY = "response key";
    static final String UPLOAD_TEXT_KEY = "upload key";
    static final String DETAILED_RESPONSE_KEY = "det key";

    public static EmotionServiceClient mClient;
    public static String API_KEY;
    public List<RecognizeResult> mEmotionResults;
    public HandlingUi mListener;
    Bitmap mBitmap;
    ActivityMainBinding binding; //needed for binding (instead of using heavy findViewById method

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        API_KEY = getString(R.string.emotion_API_key);
        binding.buttonGallery.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        binding.buttonCamera.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        if (savedInstanceState != null) {
            mBitmap = savedInstanceState.getParcelable(BITMAP_KEY);
            binding.ivUploadedFacePhoto.setImageBitmap(mBitmap);
            binding.tvEmotionSummary.setText(DETAILED_RESPONSE_KEY);
        }

        if (mClient == null) {
            mClient = new EmotionServiceRestClient(getString(R.string.emotion_API_key));
        }

        mListener = new HandlingUi() {

            @Override
            public void onLoadFinishShowViews() {
                binding.tvLoadingIndicator.setVisibility(View.GONE);
                binding.pbLoadingIndicator.setVisibility(View.GONE);
                binding.tvChooseHowToUploadPhoto.setText(R.string.upload_another_photo);
            }

            @Override
            public void onLoadStartShowProgress() {
                binding.tvLoadingIndicator.setVisibility(View.VISIBLE);
                binding.pbLoadingIndicator.setVisibility(View.VISIBLE);
            }

            @Override
            public List<RecognizeResult> getAllRecognizeResults(List<RecognizeResult> emotionResults) {
                mEmotionResults = emotionResults;
                setEmotionShortResults();
                return emotionResults;
            }
        };
    }

    /**
     * method will hide loading indicator
     */
    @Override
    public void onLoadFinishShowViews() {
    }

    /**
     * method will show loading progress - it will display progress bar and progress tv
     */
    @Override
    public void onLoadStartShowProgress() {
    }

    @Override
    public List<RecognizeResult> getAllRecognizeResults(List<RecognizeResult> res) {
        return res;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        mBitmap = savedInstanceState.getParcelable(BITMAP_KEY);
        binding.tvFacePhotoResponse.setText(savedInstanceState.getString(RESPONSE_KEY));
        binding.tvChooseHowToUploadPhoto.setText(savedInstanceState.getString(UPLOAD_TEXT_KEY));
        binding.tvEmotionSummary.setText(savedInstanceState.getString(DETAILED_RESPONSE_KEY));
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_KEY, mBitmap);
        outState.putString(RESPONSE_KEY, binding.tvFacePhotoResponse.getText().toString());
        outState.putString(UPLOAD_TEXT_KEY, binding.tvChooseHowToUploadPhoto.getText().toString());
        outState.putString(DETAILED_RESPONSE_KEY, binding.tvEmotionSummary.getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Detects request code
        /** Activity.RESULT_OK -> Standard activity result: operation succeeded. */

        //gallery is opened
        if (requestCode == GET_FROM_GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                binding.ivUploadedFacePhoto.setImageBitmap(bitmap);
                mBitmap = bitmap;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //camera is opened
        if (requestCode == GET_FROM_CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            binding.ivUploadedFacePhoto.setImageBitmap(imageBitmap);
            mBitmap = imageBitmap;
        }

        doRecognizeEmotion();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, GET_FROM_CAMERA_REQUEST_CODE);
        }
    }

    private void openGallery() {
        Intent getPictureFromGallery = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(getPictureFromGallery, GET_FROM_GALLERY_REQUEST_CODE);
    }

    public void doRecognizeEmotion() {
        try {
            FetchRecognizeEmotionTask fetchTask = new FetchRecognizeEmotionTask(mListener);
            fetchTask.mBitmap = this.mBitmap;
            fetchTask.mClient = this.mClient;
            fetchTask.mEmotionRecognitionResponse = this.binding.tvFacePhotoResponse;
            fetchTask.execute();

        } catch (Exception e) {
            binding.tvFacePhotoResponse.setText("Error encountered. Exception is: " + e.toString());
        }
    }

    /**
     * this method gets executed after async task is finished
     * it is another way to set emotion values -> another approach is in FetchRecognizeEmotionTask in onPostExecute method
     */
    public void setEmotionShortResults() {

        binding.tvEmotionSummary.setVisibility(View.VISIBLE);

        if (mEmotionResults != null) {
            binding.tvEmotionSummary.setText("");

            //Here we order results and take 3 emotions with highest number;
            //TODO need to add code in the loop and store the values in a better way
            //TODO add detail response for more than 1 person
            Double emotionValue1 = mEmotionResults.get(0).scores.ToRankedList(Order.DESCENDING).get(0).getValue();
            Double emotionValue2 = mEmotionResults.get(0).scores.ToRankedList(Order.DESCENDING).get(1).getValue();
            Double emotionValue3 = mEmotionResults.get(0).scores.ToRankedList(Order.DESCENDING).get(2).getValue();

            String emotionKey1 = mEmotionResults.get(0).scores.ToRankedList(Order.DESCENDING).get(0).getKey();
            String emotionKey2 = mEmotionResults.get(0).scores.ToRankedList(Order.DESCENDING).get(1).getKey();
            String emotionKey3 = mEmotionResults.get(0).scores.ToRankedList(Order.DESCENDING).get(2).getKey();

            binding.tvEmotionSummary.setText(getString(R.string.you_got) + "\n");

            binding.tvEmotionSummary.append(String.format("%.2f", emotionValue1 * 100) + "% " + emotionKey1.toLowerCase() + " today!\n");
            binding.tvEmotionSummary.append(String.format("%.2f", emotionValue2 * 100) + "% " + emotionKey2.toLowerCase() + " today!\n");
            binding.tvEmotionSummary.append(String.format("%.2f", emotionValue3 * 100) + "% " + emotionKey3.toLowerCase() + " today!\n");

        } else {
            binding.tvEmotionSummary.setText(getString(R.string.empty_emotion_result));
        }
    }
}