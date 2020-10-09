package com.visilabs.story;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.visilabs.Visilabs;
import com.visilabs.VisilabsResponse;
import com.visilabs.story.model.skinbased.VisilabsSkinBasedResponse;
import com.visilabs.story.model.storylookingbanner.Actiondata;
import com.visilabs.util.VisilabsConstant;

import com.visilabs.api.VisilabsCallback;
import com.visilabs.inApp.VisilabsActionRequest;
import com.visilabs.story.model.StoryItemClickListener;
import com.visilabs.story.model.storylookingbanner.VisilabsStoryLookingBannerResponse;

public class VisilabsRecyclerView extends RecyclerView {

    Context context;
    StoryItemClickListener storyItemClickListener;

    String skin_bassed = "{\n" +
            "  \"capping\": \"{\\\"data\\\":{}}\",\n" +
            "  \"VERSION\": 2,\n" +
            "  \"FavoriteAttributeAction\": [\n" +
            "  ],\n" +
            "  \"Story\": [\n" +
            "    {\n" +
            "      \"actid\": \"int action id\",\n" +
            "      \"title\": \"string action name\",\n" +
            "      \"actiontype\": \"Story\",\n" +
            "      \"actiondata\": {\n" +
            "        \"stories\": [\n" +
            "          {\n" +
            "            \"title\": \"Ramon\",\n" +
            "            \"thumbnail\": \"https://cdn.jpegmini.com/user/images/slider_puffin_jpegmini_mobile.jpg\",\n" +
            "            \"items\": [\n" +
            "              {\n" +
            "                \"fileSrc\": \"https://nenedir.com.tr/wp-content/uploads/2018/11/reklam.jpg\",\n" +
            "                \"filePreview\": \"\",\n" +
            "                \"buttonText\": \"Visit my Portfolio\",\n" +
            "                \"buttonTextColor\": \"#7d1212\",\n" +
            "                \"buttonColor\": \"#998686\",\n" +
            "                \"fileType\": \"photo\",\n" +
            "                \"displayTime\": 3,\n" +
            "                \"targetUrl\": \"http://visilabs.com/?title=Ramon&OM.zn=acttype-32&OM.zpc=act-160\",\n" +
            "                \"targetUrlOriginal\": \"http://visilabs.com/?title=Ramon\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"fileSrc\": \"https://digitalage.com.tr/wp-content/uploads/2017/06/Sosyal-medya-reklam-modelleri.jpg\",\n" +
            "                \"filePreview\": \"\",\n" +
            "                \"buttonText\": \"test test Portfolio\",\n" +
            "                \"buttonTextColor\": \"#7d1212\",\n" +
            "                \"buttonColor\": \"#998686\",\n" +
            "                \"fileType\": \"video\",\n" +
            "                \"displayTime\": 0,\n" +
            "                \"targetUrl\": \"\",\n" +
            "                \"targetUrlOriginal\": \"\"\n" +
            "              }\n" +
            "            ]\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"Rivers Cuomo\",\n" +
            "            \"thumbnail\": \"https://interactive-examples.mdn.mozilla.net/media/cc0-images/grapefruit-slice-332-332.jpg\",\n" +
            "            \"items\": []\n" +
            "          }\n" +
            "        ],\n" +
            "        \"taTemplate\": \"skin_based\",\n" +
            "        \"ExtendedProps\": \"%7B%22storylb_img_borderWidth%22%3A%223%22%2C%22storylb_img_borderColor%22%3A%22%23ebc70b%22%2C%22storylb_img_borderRadius%22%3A%2250%25%22%2C%22storylb_img_boxShadow%22%3A%22rgba(0%2C0%2C0%2C0.4)%205px%205px%2010px%22%2C%22storylb_label_color%22%3A%22%23ff34ae%22%7D\"\n" +
            "      }\n" +
            "    }]\n" +
            "}\n" +
            " ";

    public VisilabsRecyclerView(Context context) {
        super(context);
        this.context = context;
    }

    public VisilabsRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VisilabsRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setStoryAction(Context context, StoryItemClickListener storyItemClickListener) {
        this.storyItemClickListener = storyItemClickListener;
        VisilabsActionRequest visilabsActionRequest;
        try {
            visilabsActionRequest = Visilabs.CallAPI().requestAction("Story");
            visilabsActionRequest.executeAsyncAction(getVisilabsStoryCallback(context));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setStoryActionId(Context context, String actionId, StoryItemClickListener storyItemClickListener) {
        this.storyItemClickListener = storyItemClickListener;
        VisilabsActionRequest visilabsActionRequest;
        try {
            visilabsActionRequest = Visilabs.CallAPI().requestActionId(actionId);
            visilabsActionRequest.executeAsyncAction(getVisilabsStoryCallback(context));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public VisilabsCallback getVisilabsStoryCallback(final Context context) {

        return new VisilabsCallback() {
            @Override
            public void success(VisilabsResponse response) {
                try {
                    VisilabsStoryLookingBannerResponse visilabsStoryLookingBannerResponse = new Gson().fromJson(response.getRawResponse(), VisilabsStoryLookingBannerResponse.class);

                    if (visilabsStoryLookingBannerResponse.getStory().get(0).getActiondata().getTaTemplate().equals("story_looking_bannersz")) {

                        VisilabsStoryLookingBannerAdapter visilabsStoryLookingBannerAdapter = new VisilabsStoryLookingBannerAdapter(context, storyItemClickListener);

                        visilabsStoryLookingBannerAdapter.setStoryList(visilabsStoryLookingBannerResponse, visilabsStoryLookingBannerResponse.getStory().get(0).getActiondata().getExtendedProps());

                        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                        setHasFixedSize(true);

                        setAdapter(visilabsStoryLookingBannerAdapter);

                    } else {
                        VisilabsSkinBasedResponse skinBased = new Gson().fromJson(skin_bassed, VisilabsSkinBasedResponse.class);

                        VisilabsSkinBasedAdapter visilabsSkinBasedAdapter = new VisilabsSkinBasedAdapter(context, storyItemClickListener);

                        visilabsSkinBasedAdapter.setStoryList(skinBased, skinBased.getStory().get(0).getActiondata().getExtendedProps());

                        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                        setHasFixedSize(true);

                        setAdapter(visilabsSkinBasedAdapter);
                    }

                } catch (Exception ex) {
                    Log.e("Error", ex.getMessage(), ex);
                }
            }

            @Override
            public void fail(VisilabsResponse response) {
                Log.d("Error", response.getRawResponse());
            }
        };
    }
}
