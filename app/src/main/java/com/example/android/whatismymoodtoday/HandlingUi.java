package com.example.android.whatismymoodtoday;

import com.microsoft.projectoxford.emotion.contract.RecognizeResult;

import java.io.Serializable;
import java.util.List;

/**
 * Created by monika on 2017-05-31.
 */

/**
 * This interface is needed to pass data from FetchRecognizeEmotionTask to objects in MainActivity
 * and we can simply handle UI changes on the main thread
 */
public interface HandlingUi  {
    void onLoadFinishShowViews();
    void onLoadStartShowProgress();
    List<RecognizeResult> getAllRecognizeResults(List<RecognizeResult> res);
}
