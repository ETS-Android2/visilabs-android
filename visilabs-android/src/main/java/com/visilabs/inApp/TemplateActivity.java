package com.visilabs.inApp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.squareup.picasso.Picasso;
import com.visilabs.InAppNotificationState;
import com.visilabs.Visilabs;
import com.visilabs.android.R;
import com.visilabs.android.databinding.ActivityTemplateBinding;
import com.visilabs.android.databinding.ActivityTemplateCarouselBinding;
import com.visilabs.android.databinding.NpsSecondPopUpBinding;
import com.visilabs.api.VisilabsUpdateDisplayState;
import com.visilabs.inApp.carousel.OnSwipeTouchListener;
import com.visilabs.util.StringUtils;
import com.visilabs.view.BaseRating;
import com.visilabs.view.SmileRating;


public class TemplateActivity extends Activity implements SmileRating.OnSmileySelectionListener, SmileRating.OnRatingSelectedListener {
    enum NpsSecondPopUpType {
        IMAGE_TEXT_BUTTON,
        IMAGE_TEXT_BUTTON_IMAGE,
        FEEDBACK_FORM
    }

    private static final String LOG_TAG = "Template Activity";
    public static final String INTENT_ID_KEY = "INTENT_ID_KEY";
    private static final String CAROUSEL_LAST_INDEX_KEY = "carousel_last_index";
    InAppMessage mInAppMessage;
    private VisilabsUpdateDisplayState mUpdateDisplayState;
    private int mIntentId = -1;
    private ActivityTemplateBinding binding;
    private NpsSecondPopUpBinding bindingSecondPopUp;
    private ActivityTemplateCarouselBinding bindingCarousel;
    private boolean mIsCarousel = false;
    private int mCarouselPosition = -1;
    private boolean mIsRotation = false;
    private NpsSecondPopUpType secondPopUpType = NpsSecondPopUpType.IMAGE_TEXT_BUTTON;
    private InAppButtonInterface buttonCallback = null;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mIntentId = savedInstanceState.getInt(INTENT_ID_KEY, Integer.MAX_VALUE);
        } else {
            mIntentId = getIntent().getIntExtra(INTENT_ID_KEY, Integer.MAX_VALUE);
        }

        mInAppMessage = getInAppMessage();
        buttonCallback = Visilabs.CallAPI().getInAppButtonInterface();

        View view;

        //TODO: Open this line to test carousel.
        //mInAppMessage.getActionData().setMsgType(InAppActionType.CAROUSEL.toString());

        if (mInAppMessage.getActionData().getMsgType() == InAppActionType.CAROUSEL) {
            mIsCarousel = true;
            bindingCarousel = ActivityTemplateCarouselBinding.inflate(getLayoutInflater());
            view = bindingCarousel.getRoot();
            if (savedInstanceState != null) {
                mCarouselPosition = savedInstanceState.getInt(CAROUSEL_LAST_INDEX_KEY, -1);
            }
        } else {
            mIsCarousel = false;
            binding = ActivityTemplateBinding.inflate(getLayoutInflater());
            view = binding.getRoot();
        }

        cacheImages();

        setContentView(view);
        this.setFinishOnTouchOutside(false);

        if (isShowingInApp() && mInAppMessage != null) {
            if (mIsCarousel) {
                if (mCarouselPosition == -1) {
                    mCarouselPosition = 0;
                }
                bindingCarousel.carouselContainer.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
                    public void onSwipeRight() {
                        if (!isFirstCarousel()) {
                            mCarouselPosition--;
                            setupViewCarousel();
                        }
                    }

                    public void onSwipeLeft() {
                        if (!isLastCarousel()) {
                            mCarouselPosition++;
                            setupViewCarousel();
                        }
                    }
                });
                setupInitialViewCarousel();
            } else {
                setUpView();
            }
        } else {
            VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
            finish();
        }

        this.setFinishOnTouchOutside(true);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(INTENT_ID_KEY, mIntentId);
        if (mIsCarousel) {
            outState.putInt(CAROUSEL_LAST_INDEX_KEY, mCarouselPosition);
        }
        mIsRotation = true;
    }

    private InAppMessage getInAppMessage() {

        InAppNotificationState inAppNotificationState;

        mUpdateDisplayState = VisilabsUpdateDisplayState.claimDisplayState(mIntentId);

        if (mUpdateDisplayState == null || mUpdateDisplayState.getDisplayState() == null) {
            Log.e("Visilabs", "VisilabsNotificationActivity intent received, but nothing was found to show.");

            return null;
        } else {
            inAppNotificationState =
                    (InAppNotificationState) mUpdateDisplayState.getDisplayState();
            return inAppNotificationState.getInAppMessage();

        }
    }

    private void setUpView() {
        if (!mInAppMessage.getActionData().getImg().equals("")) {
            binding.ivTemplate.setVisibility(View.VISIBLE);
            Picasso.get().load(mInAppMessage.getActionData().getImg()).into(binding.ivTemplate);
        } else {
            binding.ivTemplate.setVisibility(View.GONE);
        }

        binding.smileRating.setOnSmileySelectionListener(this);
        binding.smileRating.setOnRatingSelectedListener(this);

        setCloseButton();

        setTemplate();
    }

    private void setTemplate() {

        if(mInAppMessage.getActionData().getBackground() != null && !mInAppMessage.getActionData().getBackground().equals("")) {
            try {
                binding.llBack.setBackgroundColor(Color.parseColor(mInAppMessage.getActionData().getBackground()));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Could not parse the data given for background color\nSetting the default value.");
                e.printStackTrace();
            }
        }
        binding.ibClose.setBackgroundResource(getCloseIcon());

        switch (mInAppMessage.getActionData().getMsgType()) {

            case IMAGE_TEXT_BUTTON:

                setTitle();
                setBody();
                setButton();
                setPromotionCode();
                binding.ratingBar.setVisibility(View.GONE);
                binding.smileRating.setVisibility(View.GONE);

                break;

            case FULL_IMAGE:

                binding.tvBody.setVisibility(View.GONE);
                binding.tvTitle.setVisibility(View.GONE);
                binding.smileRating.setVisibility(View.GONE);
                binding.btnTemplate.setVisibility(View.GONE);

                binding.ivTemplate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Visilabs.CallAPI().trackInAppMessageClick(mInAppMessage, getRateReport());
                        if (buttonCallback != null) {
                            Visilabs.CallAPI().setInAppButtonInterface(null);
                            buttonCallback.onPress(mInAppMessage.getActionData().getAndroidLnk());
                        } else {
                            if (mInAppMessage.getActionData().getAndroidLnk() != null && mInAppMessage.getActionData().getAndroidLnk().length() > 0) {
                                try {
                                    Intent viewIntent = new Intent(Intent.ACTION_VIEW, StringUtils.getURIfromUrlString(mInAppMessage.getActionData().getAndroidLnk()));
                                    startActivity(viewIntent);

                                } catch (final ActivityNotFoundException e) {
                                    Log.i("Visilabs", "User doesn't have an activity for notification URI");
                                }
                            }
                        }
                        VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
                        finish();
                    }
                });

                break;

            case IMAGE_BUTTON:

                binding.smileRating.setVisibility(View.GONE);
                binding.llTextContainer.setVisibility(View.GONE);
                setButton();

                break;

            case NPS:
                setTitle();
                setBody();
                setButton();
                showNps();

                break;

            case SMILE_RATING:

                setBody();
                setTitle();
                setButton();
                showSmileRating();

                break;

            case NPS_WITH_NUMBERS:

                binding.smileRating.setVisibility(View.GONE);
                setBody();
                setTitle();
                setButton();
                showNpsWithNumbers();

                break;

            case NPS_AND_SECOND_POP_UP:
                setNpsSecondPopUpCloseButton();
                setTitle();
                setBody();
                setNpsSecondPopUpButton();
                showNps();

                break;

        }
    }

    private void setNpsSecondPopUpCloseButton() {
        binding.ibClose.setVisibility(View.GONE);
        switch (mInAppMessage.getActionData().getSecondPopupType()) {
            case "image_text_button": {
                secondPopUpType = NpsSecondPopUpType.IMAGE_TEXT_BUTTON;
                break;
            }

            case "image_text_button_image": {
                secondPopUpType = NpsSecondPopUpType.IMAGE_TEXT_BUTTON_IMAGE;
                break;
            }

            case "feedback_form": {
                secondPopUpType = NpsSecondPopUpType.FEEDBACK_FORM;
                break;
            }
        }
    }

    private void setTitle() {

        binding.tvTitle.setVisibility(View.VISIBLE);
        binding.tvTitle.setTypeface(mInAppMessage.getActionData().getFontFamily(), Typeface.BOLD);
        binding.tvTitle.setText(mInAppMessage.getActionData().getMsgTitle().replace("\\n","\n"));
        if(mInAppMessage.getActionData().getMsgTitleColor() != null && !mInAppMessage.getActionData().getMsgTitleColor().equals("")) {
            try {
                binding.tvTitle.setTextColor(Color.parseColor(mInAppMessage.getActionData().getMsgTitleColor()));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Could not parse the data given for message title color\nSetting the default value.");
                e.printStackTrace();
                binding.tvTitle.setTextColor(getResources().getColor(R.color.blue));
            }
        } else {
            binding.tvTitle.setTextColor(getResources().getColor(R.color.blue));
        }
        try {
            binding.tvTitle.setTextSize(Float.parseFloat(mInAppMessage.getActionData().getMsgBodyTextSize()) + 12);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Could not parse the data given for message body text size\nSetting the default value.");
            e.printStackTrace();
            binding.tvTitle.setTextSize(16);
        }
    }

    private void setBody() {
        binding.tvBody.setText(mInAppMessage.getActionData().getMsgBody().replace("\\n", "\n"));
        binding.tvBody.setTypeface(mInAppMessage.getActionData().getFontFamily());
        binding.tvBody.setVisibility(View.VISIBLE);
        if(mInAppMessage.getActionData().getMsgBodyColor() != null && !mInAppMessage.getActionData().getMsgBodyColor().equals("")) {
            try {
                binding.tvBody.setTextColor(Color.parseColor(mInAppMessage.getActionData().getMsgBodyColor()));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Could not parse the data given for message body color\nSetting the default value.");
                e.printStackTrace();
            }
        }
        try {
            binding.tvBody.setTextSize(Float.parseFloat(mInAppMessage.getActionData().getMsgBodyTextSize()) + 8);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Could not parse the data given for message body text size\nSetting the default value.");
            e.printStackTrace();
            binding.tvBody.setTextSize(12);
        }
    }

    private void setButton() {

        binding.btnTemplate.setTypeface(mInAppMessage.getActionData().getFontFamily());
        binding.btnTemplate.setVisibility(View.VISIBLE);
        binding.btnTemplate.setText(mInAppMessage.getActionData().getBtnText());
        if(mInAppMessage.getActionData().getButtonTextColor() != null && !mInAppMessage.getActionData().getButtonTextColor().equals("")) {
            try {
                binding.btnTemplate.setTextColor(Color.parseColor(mInAppMessage.getActionData().getButtonTextColor()));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Could not parse the data given for button text color\nSetting the default value.");
                e.printStackTrace();
                binding.btnTemplate.setTextColor(getResources().getColor(R.color.black));
            }
        } else {
            binding.btnTemplate.setTextColor(getResources().getColor(R.color.black));
        }
        if(mInAppMessage.getActionData().getButtonColor() != null && !mInAppMessage.getActionData().getButtonColor().equals("")) {
            try {
                binding.btnTemplate.setBackgroundColor(Color.parseColor(mInAppMessage.getActionData().getButtonColor()));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Could not parse the data given for button color\nSetting the default value.");
                e.printStackTrace();
            }
        }

        binding.btnTemplate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Visilabs.CallAPI().trackInAppMessageClick(mInAppMessage, getRateReport());
                if(buttonCallback != null) {
                    Visilabs.CallAPI().setInAppButtonInterface(null);
                    buttonCallback.onPress(mInAppMessage.getActionData().getAndroidLnk());
                } else {
                    if (mInAppMessage.getActionData().getAndroidLnk() != null && mInAppMessage.getActionData().getAndroidLnk().length() > 0) {
                        try {
                            Intent viewIntent = new Intent(Intent.ACTION_VIEW, StringUtils.getURIfromUrlString(mInAppMessage.getActionData().getAndroidLnk()));
                            startActivity(viewIntent);

                        } catch (final ActivityNotFoundException e) {
                            Log.i("Visilabs", "User doesn't have an activity for notification URI");
                        }
                    }
                }
                VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
                finish();
            }
        });
    }

    private void setNpsSecondPopUpButton() {
        binding.btnTemplate.setTypeface(mInAppMessage.getActionData().getFontFamily());
        binding.btnTemplate.setVisibility(View.VISIBLE);
        binding.btnTemplate.setText(mInAppMessage.getActionData().getBtnText());
        if(mInAppMessage.getActionData().getButtonTextColor() != null && !mInAppMessage.getActionData().getButtonTextColor().equals("")) {
            try {
                binding.btnTemplate.setTextColor(Color.parseColor(mInAppMessage.getActionData().getButtonTextColor()));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Could not parse the data given for button text color\nSetting the default value.");
                e.printStackTrace();
                binding.btnTemplate.setTextColor(getResources().getColor(R.color.black));
            }
        } else {
            binding.btnTemplate.setTextColor(getResources().getColor(R.color.black));
        }
        if(mInAppMessage.getActionData().getButtonColor() != null && !mInAppMessage.getActionData().getButtonColor().equals("")) {
            try {
                binding.btnTemplate.setBackgroundColor(Color.parseColor(mInAppMessage.getActionData().getButtonColor()));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Could not parse the data given for button color\nSetting the default value.");
                e.printStackTrace();
            }
        }

        binding.btnTemplate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Visilabs.CallAPI().trackInAppMessageClick(mInAppMessage, getRateReport());
                if(secondPopUpType == NpsSecondPopUpType.FEEDBACK_FORM) {
                    if(isRatingAboveThreshold()) {
                        VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
                        finish();
                    } else {
                        setupSecondPopUp();
                    }
                } else {
                    setupSecondPopUp();
                }
            }
        });
    }

    private boolean isRatingAboveThreshold() {
        int rating = (int)(binding.ratingBar.getRating() * 2);
        return rating > (Integer.parseInt(mInAppMessage.getActionData().getSecondPopupFeecbackFormMinPoint()) * 2);
    }

    private void setupSecondPopUp () {
        bindingSecondPopUp = NpsSecondPopUpBinding.inflate(getLayoutInflater());
        setContentView(bindingSecondPopUp.getRoot());
        if(mInAppMessage.getActionData().getBackground() != null && !mInAppMessage.getActionData().getBackground().equals("")) {
            try {
                bindingSecondPopUp.container.setBackgroundColor(Color.parseColor(mInAppMessage.getActionData().getBackground()));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Could not parse the data given for background color\nSetting the default value.");
                e.printStackTrace();
            }
        }
        switch (secondPopUpType) {
            case IMAGE_TEXT_BUTTON: {
                bindingSecondPopUp.commentBox.setVisibility(View.GONE);
                bindingSecondPopUp.imageView2.setVisibility(View.GONE);
                if(mInAppMessage.getActionData().getPromotionCode()!=null &&
                        !mInAppMessage.getActionData().getPromotionCode().isEmpty()) {
                    bindingSecondPopUp.couponContainer.setBackgroundColor(Color.parseColor(
                            mInAppMessage.getActionData().getPromoCodeBackgroundColor()
                    ));
                    bindingSecondPopUp.couponCode.setText(mInAppMessage.getActionData().getPromotionCode());
                    bindingSecondPopUp.couponCode.setTextColor(Color.parseColor(
                            mInAppMessage.getActionData().getPromoCodeTextColor()
                    ));
                    bindingSecondPopUp.couponContainer.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText(getString(R.string.coupon_code), mInAppMessage.getActionData().getPromotionCode());
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(getApplicationContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    bindingSecondPopUp.couponContainer.setVisibility(View.GONE);
                }
                break;
            }
            case IMAGE_TEXT_BUTTON_IMAGE: {
                bindingSecondPopUp.commentBox.setVisibility(View.GONE);
                bindingSecondPopUp.couponContainer.setVisibility(View.GONE);
                if(mInAppMessage.getActionData().getSecondPopupImg2()!=null &&
                        !mInAppMessage.getActionData().getSecondPopupImg2().isEmpty()){
                    Picasso.get().load(mInAppMessage.getActionData().getSecondPopupImg2())
                            .into(bindingSecondPopUp.imageView2);
                }
                break;
            }
            case FEEDBACK_FORM: {
                bindingSecondPopUp.imageView2.setVisibility(View.GONE);
                bindingSecondPopUp.couponContainer.setVisibility(View.GONE);
                break;
            }
            default: {
                VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
                finish();
                break;
            }
        }
        bindingSecondPopUp.closeButton.setBackgroundResource(getCloseIcon());
        bindingSecondPopUp.closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
                finish();
            }
        });
        if(mInAppMessage.getActionData().getSecondPopupImg1()!=null &&
        !mInAppMessage.getActionData().getSecondPopupImg1().isEmpty()){
            Picasso.get().load(mInAppMessage.getActionData().getSecondPopupImg1())
                    .into(bindingSecondPopUp.imageView);
        }
        bindingSecondPopUp.titleView.setTypeface(mInAppMessage.getActionData().getFontFamily(), Typeface.BOLD);
        bindingSecondPopUp.titleView.setText(mInAppMessage.getActionData().getSecondPopupMsgTitle().replace("\\n","\n"));
        bindingSecondPopUp.titleView.setTextColor(Color.parseColor(mInAppMessage.getActionData().getMsgTitleColor()));
        bindingSecondPopUp.bodyTextView.setTypeface(mInAppMessage.getActionData().getFontFamily());
        bindingSecondPopUp.bodyTextView.setText(mInAppMessage.getActionData().getSecondPopupMsgBody().replace("\\n","\n"));
        bindingSecondPopUp.bodyTextView.setTextColor(Color.parseColor(mInAppMessage.getActionData().getMsgBodyColor()));
        bindingSecondPopUp.bodyTextView.setTextSize(Float.parseFloat(mInAppMessage.getActionData().getSecondPopupMsgBodyTextSize()) + 8);
        bindingSecondPopUp.button.setTypeface(mInAppMessage.getActionData().getFontFamily());
        bindingSecondPopUp.button.setText(mInAppMessage.getActionData().getSecondPopupBtnText());
        bindingSecondPopUp.button.setTextColor(Color.parseColor(mInAppMessage.getActionData().getButtonTextColor()));
        bindingSecondPopUp.button.setBackgroundColor(Color.parseColor(mInAppMessage.getActionData().getButtonColor()));
        bindingSecondPopUp.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSecondPopUpReport();
                if(secondPopUpType == NpsSecondPopUpType.FEEDBACK_FORM) {
                    sendCommentToLogger(bindingSecondPopUp.commentBox.getText().toString());
                } else {
                    if (buttonCallback != null) {
                        Visilabs.CallAPI().setInAppButtonInterface(null);
                        buttonCallback.onPress(mInAppMessage.getActionData().getAndroidLnk());
                    } else {
                        if(getInAppMessage().getActionData().getAndroidLnk()!=null &&
                                !getInAppMessage().getActionData().getAndroidLnk().isEmpty()) {
                            try {
                                Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                                        getInAppMessage().getActionData().getAndroidLnk()));
                                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(viewIntent);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "The link is not formatted properly!");
                            }
                        } else {
                            Log.e(LOG_TAG, "The link is empty or not in a proper format!");
                        }
                    }
                }
                VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
                finish();
            }
        });
    }

    private void sendSecondPopUpReport () {
        Visilabs.CallAPI().trackInAppMessageClick(mInAppMessage, null);
    }

    private void sendCommentToLogger(String comment) {
        //TODO Second Popup : call customEvent with the proper parameter here
    }

    private void setPromotionCode() {
        if (!StringUtils.isNullOrWhiteSpace(mInAppMessage.getActionData().getPromotionCode())
                && !StringUtils.isNullOrWhiteSpace(mInAppMessage.getActionData().getPromoCodeBackgroundColor())
                && !StringUtils.isNullOrWhiteSpace(mInAppMessage.getActionData().getPromoCodeTextColor())) {
            binding.llCouponContainer.setVisibility(View.VISIBLE);
            binding.llCouponContainer.setBackgroundColor(Color.parseColor(mInAppMessage.getActionData().getPromoCodeBackgroundColor()));
            binding.tvCouponCode.setText(mInAppMessage.getActionData().getPromotionCode());
            binding.tvCouponCode.setTextColor(Color.parseColor(mInAppMessage.getActionData().getPromoCodeTextColor()));
            binding.llCouponContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(getString(R.string.coupon_code), mInAppMessage.getActionData().getPromotionCode());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getApplicationContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            binding.llCouponContainer.setVisibility(View.GONE);
        }
    }

    private String getRateReport() {
        switch (mInAppMessage.getActionData().getMsgType()) {
            case SMILE_RATING:
                return "OM.s_point=" + binding.smileRating.getRating() + "&OM.s_cat=" + mInAppMessage.getActionData().getMsgType() + "&OM.s_page=act-" + mInAppMessage.getActId();

            case NPS:
                return "OM.s_point=" + binding.ratingBar.getRating() + "&OM.s_cat=" + mInAppMessage.getActionData().getMsgType() + "&OM.s_page=act-" + mInAppMessage.getActId();

            case NPS_WITH_NUMBERS:
                return "OM.s_point=" + binding.npsWithNumbersView.getSelectedRate() + "&OM.s_cat=" + mInAppMessage.getActionData().getMsgType() + "&OM.s_page=act-" + mInAppMessage.getActId();
        }

        return "";
    }


    public void setCloseButton() {

        binding.ibClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);

                finish();
            }
        });
    }

    void showNps() {

        binding.ratingBar.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.ratingBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.yellow)));
        }
    }

    void showSmileRating() {
        binding.smileRating.setVisibility(View.VISIBLE);
    }

    void showNpsWithNumbers() {
        binding.npsWithNumbersView.setVisibility(View.VISIBLE);
        int[] colors = new int[mInAppMessage.getActionData().getNumberColors().length];

        for (int i = 0; i < mInAppMessage.getActionData().getNumberColors().length; i++) {
            colors[i] = Color.parseColor(mInAppMessage.getActionData().getNumberColors()[i]);
        }
        binding.npsWithNumbersView.setColors(colors);
    }

    private int getCloseIcon() {

        switch (mInAppMessage.getActionData().getCloseButtonColor()) {

            case "white":
                return R.drawable.ic_close_white_24dp;

            case "black":
                return R.drawable.ic_close_black_24dp;
        }
        return R.drawable.ic_close_black_24dp;
    }

    private boolean isShowingInApp() {
        if (mUpdateDisplayState == null) {
            return false;
        }
        return InAppNotificationState.TYPE.equals(
                mUpdateDisplayState.getDisplayState().getType()
        );
    }

    @Override
    public void onSmileySelected(@BaseRating.Smiley int smiley, boolean reselected) {
        switch (smiley) {
            case SmileRating.BAD:
                Log.i("VL", "Bad");
                break;
            case SmileRating.GOOD:
                Log.i("VL", "Good");
                break;
            case SmileRating.GREAT:
                Log.i("VL", "Great");
                break;
            case SmileRating.OKAY:
                Log.i("VL", "Okay");
                break;
            case SmileRating.TERRIBLE:
                Log.i("VL", "Terrible");
                break;
            case SmileRating.NONE:
                Log.i("VL", "None");
                break;
        }
    }

    @Override
    public void onRatingSelected(int level, boolean reselected) {
        Log.i("VL", "Rated as: " + level + " - " + reselected);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsRotation) {
            mIsRotation = false;
        } else {
            VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
        }
    }

    private void setupInitialViewCarousel() {
        bindingCarousel.carouselCloseButton.setBackgroundResource(getCloseIcon());
        bindingCarousel.carouselCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
                finish();
            }
        });
        for (int i = 0; i < getCarouselItemCount(); i++) {
            View view = new View(getApplicationContext());
            view.setBackgroundResource(R.drawable.dot_indicator_default);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    40, 40);
            layoutParams.setMargins(10, 0, 10, 0);
            view.setLayoutParams(layoutParams);
            bindingCarousel.dotIndicator.addView(view);
        }
        setupViewCarousel();
    }

    private void setupViewCarousel() {
        bindingCarousel.carouselImage.setVisibility(View.VISIBLE);
        bindingCarousel.carouselTitle.setVisibility(View.VISIBLE);
        bindingCarousel.carouselBodyText.setVisibility(View.VISIBLE);
        bindingCarousel.carouselButton.setVisibility(View.VISIBLE);

        for (int i = 0; i < getCarouselItemCount(); i++) {
            if (i == mCarouselPosition) {
                bindingCarousel.dotIndicator.getChildAt(i).setBackgroundResource(R.drawable.dot_indicator_selected);
            } else {
                bindingCarousel.dotIndicator.getChildAt(i).setBackgroundResource(R.drawable.dot_indicator_default);
            }
        }
        //TODO: check mInAppMessage and mCarouselPosition and set visibilities
        switch (mCarouselPosition) {
            case 0: {
                bindingCarousel.carouselContainer.setBackgroundColor(Color.parseColor("#CD2F2F"));
                Picasso.get().
                        load("https://img.visilabs.net/in-app-message/uploaded_images/163_1100_154_20200603160304969.jpg")
                        .into(bindingCarousel.carouselImage);

                bindingCarousel.carouselTitle.setText("Title1");
                bindingCarousel.carouselTitle.setTextColor(Color.parseColor("#E8F279"));
                bindingCarousel.carouselTitle.setTextSize(32);
                bindingCarousel.carouselBodyText.setText("Text1");
                bindingCarousel.carouselBodyText.setTextColor(Color.parseColor("#E3A9E7"));
                bindingCarousel.carouselBodyText.setTextSize(24);
                bindingCarousel.carouselButton.setText("Button1");
                bindingCarousel.carouselButton.setTextColor(Color.parseColor("#000000"));
                bindingCarousel.carouselButton.setBackgroundColor(Color.parseColor("#A9E7E4"));
                bindingCarousel.carouselButton.setTextSize(24);
                bindingCarousel.carouselButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO : send report here
                        if (buttonCallback != null) {
                            Visilabs.CallAPI().setInAppButtonInterface(null);
                            buttonCallback.onPress("https://www.relateddigital.com/");
                        } else {
                            try {
                                Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.relateddigital.com/"));
                                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(viewIntent);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "The link is not formatted properly!");
                            }
                        }
                    }
                });
                break;
            }
            case 1: {
                bindingCarousel.carouselContainer.setBackgroundColor(Color.parseColor("#77CD2F"));
                bindingCarousel.carouselImage.setVisibility(View.GONE);
                bindingCarousel.carouselTitle.setText("Title2");
                bindingCarousel.carouselTitle.setTextColor(Color.parseColor("#F5F43E"));
                bindingCarousel.carouselTitle.setTextSize(32);
                bindingCarousel.carouselBodyText.setText("Text2");
                bindingCarousel.carouselBodyText.setTextColor(Color.parseColor("#FFFFFF"));
                bindingCarousel.carouselBodyText.setTextSize(24);
                bindingCarousel.carouselButton.setText("Button2");
                bindingCarousel.carouselButton.setBackgroundColor(Color.parseColor("#27FB76"));
                bindingCarousel.carouselButton.setTextColor(Color.parseColor("#000000"));
                bindingCarousel.carouselButton.setTextSize(24);
                bindingCarousel.carouselButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO : send report here
                        if (buttonCallback != null) {
                            Visilabs.CallAPI().setInAppButtonInterface(null);
                            buttonCallback.onPress("https://www.relateddigital.com/");
                        } else {
                            try {
                                Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.relateddigital.com/"));
                                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(viewIntent);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "The link is not formatted properly!");
                            }
                        }
                    }
                });
                break;
            }
            case 2: {
                bindingCarousel.carouselContainer.setBackgroundColor(Color.parseColor("#2FBBCD"));
                Picasso.get().
                        load("https://img.visilabs.net/in-app-message/uploaded_images/163_1100_411_20210121113801841.jpg")
                        .into(bindingCarousel.carouselImage);
                bindingCarousel.carouselTitle.setText("Title3");
                bindingCarousel.carouselTitle.setTextColor(Color.parseColor("#FFFFFF"));
                bindingCarousel.carouselTitle.setTextSize(32);
                bindingCarousel.carouselBodyText.setText("Text3");
                bindingCarousel.carouselBodyText.setTextColor(Color.parseColor("#1D19E0"));
                bindingCarousel.carouselBodyText.setTextSize(24);
                bindingCarousel.carouselButton.setVisibility(View.GONE);
                break;
            }
            case 3: {
                bindingCarousel.carouselContainer.setBackgroundColor(Color.parseColor("#C9CD2F"));
                bindingCarousel.carouselImage.setVisibility(View.GONE);
                bindingCarousel.carouselTitle.setText("Title4");
                bindingCarousel.carouselTitle.setTextColor(Color.parseColor("#E019D6"));
                bindingCarousel.carouselTitle.setTextSize(32);
                bindingCarousel.carouselBodyText.setVisibility(View.GONE);
                bindingCarousel.carouselButton.setText("Button4");
                bindingCarousel.carouselButton.setBackgroundColor(Color.parseColor("#E02B19"));
                bindingCarousel.carouselButton.setTextColor(Color.parseColor("#080201"));
                bindingCarousel.carouselButton.setTextSize(24);
                bindingCarousel.carouselButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO : send report here
                        if (buttonCallback != null) {
                            Visilabs.CallAPI().setInAppButtonInterface(null);
                            buttonCallback.onPress("https://www.relateddigital.com/");
                        } else {
                            try {
                                Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.relateddigital.com/"));
                                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(viewIntent);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "The link is not formatted properly!");
                            }
                        }
                    }
                });
                break;
            }
            case 4: {
                bindingCarousel.carouselContainer.setBackgroundColor(Color.parseColor("#CD2FC5"));
                Picasso.get().
                        load("https://e7.pngegg.com/pngimages/994/882/png-clipart-new-super-mario-bros-2-new-super-mario-bros-2-mario-luigi-superstar-saga-mario-heroes-super-mario-bros.png")
                        .into(bindingCarousel.carouselImage);
                bindingCarousel.carouselTitle.setVisibility(View.GONE);
                bindingCarousel.carouselBodyText.setText("Text5");
                bindingCarousel.carouselBodyText.setTextColor(getResources().getColor(R.color.yellow));
                bindingCarousel.carouselBodyText.setTextSize(24);
                bindingCarousel.carouselButton.setVisibility(View.GONE);
                break;
            }
            default: {
                break;
            }
        }
    }

    private int getCarouselItemCount() {
        //TODO: return real carousel item count here
        return 5;
    }

    private boolean isLastCarousel() {
        return mCarouselPosition == getCarouselItemCount() - 1;
    }

    private boolean isFirstCarousel() {
        return mCarouselPosition == 0;
    }

    private void cacheImages() {
        if (mIsCarousel) {
            //TODO: cache all images for carousel
            Picasso.get().
                    load("https://img.visilabs.net/in-app-message/uploaded_images/163_1100_154_20200603160304969.jpg").fetch();
            Picasso.get().
                    load("https://img.visilabs.net/in-app-message/uploaded_images/163_1100_411_20210121113801841.jpg").fetch();
            Picasso.get().
                    load("https://e7.pngegg.com/pngimages/994/882/png-clipart-new-super-mario-bros-2-new-super-mario-bros-2-mario-luigi-superstar-saga-mario-heroes-super-mario-bros.png").fetch();
        } else {
            Picasso.get().load(mInAppMessage.getActionData().getImg()).fetch();
            if(mInAppMessage.getActionData().getMsgType() == InAppActionType.NPS_AND_SECOND_POP_UP) {
                if(mInAppMessage.getActionData().getSecondPopupImg1()!=null &&
                        !mInAppMessage.getActionData().getSecondPopupImg1().isEmpty()){
                    Picasso.get().load(mInAppMessage.getActionData().getSecondPopupImg1()).fetch();
                }
                if(mInAppMessage.getActionData().getSecondPopupImg2()!=null &&
                        !mInAppMessage.getActionData().getSecondPopupImg2().isEmpty()){
                    Picasso.get().load(mInAppMessage.getActionData().getSecondPopupImg2()).fetch();
                }
            }
        }
    }
}
