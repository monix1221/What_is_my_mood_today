package com.example.android.whatismymoodtoday;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by monika on 2017-05-29.
 */

public class FetchRecognizeEmotionTask extends AsyncTask<String, String, List<RecognizeResult>> {

    public EmotionServiceClient mClient;
    TextView mEmotionRecognitionResponse;
    Bitmap mBitmap;
    List<RecognizeResult> result;
    private HandlingUi listener;
    private Exception e = null;

    public FetchRecognizeEmotionTask(HandlingUi listener) {
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        listener.onLoadStartShowProgress();
    }

    @Override
    protected List<RecognizeResult> doInBackground(String... params) {

        mClient = MainActivity.mClient;

        try {
            return processWithAutoFaceDetection();
        } catch (Exception e) {
            this.e = e;
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<RecognizeResult> recognizeResults) {
        super.onPostExecute(recognizeResults);

        if (e != null) {
            mEmotionRecognitionResponse.setText("Error: " + e.getMessage());
            this.e = null;
        } else {
            if (recognizeResults.size() == 0) {
                mEmotionRecognitionResponse.setText("No emotion detected :(");
            } else {
                Integer count = 0;
                mEmotionRecognitionResponse.setText("Your detailed emotions: \n");
                for (RecognizeResult r : recognizeResults) {

                    mEmotionRecognitionResponse.append(String.format("\nFace #%1$d \n", count));
                    mEmotionRecognitionResponse.append(String.format("\t anger: %1$.5f\n", r.scores.anger));
                    mEmotionRecognitionResponse.append(String.format("\t contempt: %1$.5f\n", r.scores.contempt));
                    mEmotionRecognitionResponse.append(String.format("\t disgust: %1$.5f\n", r.scores.disgust));
                    mEmotionRecognitionResponse.append(String.format("\t fear: %1$.5f\n", r.scores.fear));
                    mEmotionRecognitionResponse.append(String.format("\t happiness: %1$.5f\n", r.scores.happiness));
                    mEmotionRecognitionResponse.append(String.format("\t neutral: %1$.5f\n", r.scores.neutral));
                    mEmotionRecognitionResponse.append(String.format("\t sadness: %1$.5f\n", r.scores.sadness));
                    mEmotionRecognitionResponse.append(String.format("\t surprise: %1$.5f\n", r.scores.surprise));

                    count++; //used if there are many faces in the photo

                }
                result = listener.getAllRecognizeResults(recognizeResults);
            }
        }

        listener.onLoadFinishShowViews();
    }

    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();

        this.result = this.mClient.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
        return result;
    }
}