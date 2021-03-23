package com.visilabs.inApp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.squareup.picasso.Picasso;
import com.visilabs.InAppNotificationState;
import com.visilabs.Visilabs;
import com.visilabs.android.R;
import com.visilabs.android.databinding.ActivityTemplateBinding;
import com.visilabs.android.databinding.ActivityTemplateCarouselBinding;
import com.visilabs.api.VisilabsUpdateDisplayState;
import com.visilabs.inApp.carousel.CarouselAdapter;
import com.visilabs.inApp.carousel.CarouselFinishInterface;
import com.visilabs.util.StringUtils;
import com.visilabs.view.BaseRating;
import com.visilabs.view.SmileRating;


public class TemplateActivity extends Activity implements SmileRating.OnSmileySelectionListener, SmileRating.OnRatingSelectedListener {

    public static final String INTENT_ID_KEY = "INTENT_ID_KEY";
    InAppMessage mInAppMessage;
    private VisilabsUpdateDisplayState mUpdateDisplayState;
    private int mIntentId = -1;
    private ActivityTemplateBinding binding;
    private ActivityTemplateCarouselBinding bindingCarousel;
    private boolean mIsCarousel = false;
    private CarouselFinishInterface carouselFinishInterface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntentId = getIntent().getIntExtra(INTENT_ID_KEY, Integer.MAX_VALUE);
        mInAppMessage = getInAppMessage();

        View view;

        //TODO: Remove this line after testing
        mInAppMessage.getActionData().setMsgType(InAppActionType.CAROUSEL.toString());

        if (mInAppMessage.getActionData().getMsgType() == InAppActionType.CAROUSEL) {
            mIsCarousel = true;
            bindingCarousel = ActivityTemplateCarouselBinding.inflate(getLayoutInflater());
            view = bindingCarousel.getRoot();
        } else {
            mIsCarousel = false;
            binding = ActivityTemplateBinding.inflate(getLayoutInflater());
            view = binding.getRoot();
        }

        setContentView(view);
        this.setFinishOnTouchOutside(false);

        if (isShowingInApp() && mInAppMessage != null) {
            if (mIsCarousel) {
                setupViewCarousel();
            } else {
                setUpView();
            }
        } else {
            VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
            finish();
        }

        this.setFinishOnTouchOutside(true);
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

        binding.llBack.setBackgroundColor(Color.parseColor(mInAppMessage.getActionData().getBackground()));
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
                        if (mInAppMessage.getActionData().getAndroidLnk() != null && mInAppMessage.getActionData().getAndroidLnk().length() > 0) {

                            try {

                                Visilabs.CallAPI().trackInAppMessageClick(mInAppMessage, getRateReport());
                                Intent viewIntent = new Intent(Intent.ACTION_VIEW, StringUtils.getURIfromUrlString(mInAppMessage.getActionData().getAndroidLnk()));
                                startActivity(viewIntent);

                            } catch (final ActivityNotFoundException e) {
                                Log.i("Visilabs", "User doesn't have an activity for notification URI");
                            }
                        } else {
                            Visilabs.CallAPI().trackInAppMessageClick(mInAppMessage, getRateReport());
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

        }
    }

    private void setTitle() {

        binding.tvTitle.setVisibility(View.VISIBLE);
        binding.tvTitle.setTypeface(mInAppMessage.getActionData().getFontFamily(), Typeface.BOLD);
        binding.tvTitle.setText(mInAppMessage.getActionData().getMsgTitle().replace("\\n", "\n"));
        binding.tvTitle.setTextColor(Color.parseColor(mInAppMessage.getActionData().getMsgTitleColor()));
        binding.tvTitle.setTextSize(Float.parseFloat(mInAppMessage.getActionData().getMsgBodyTextSize()) + 12);
    }

    private void setBody() {
        binding.tvBody.setText(mInAppMessage.getActionData().getMsgBody().replace("\\n", "\n"));
        binding.tvBody.setTypeface(mInAppMessage.getActionData().getFontFamily());
        binding.tvBody.setVisibility(View.VISIBLE);
        binding.tvBody.setTextColor(Color.parseColor(mInAppMessage.getActionData().getMsgBodyColor()));
        binding.tvBody.setTextSize(Float.parseFloat(mInAppMessage.getActionData().getMsgBodyTextSize()) + 8);
    }

    private void setButton() {

        binding.btnTemplate.setTypeface(mInAppMessage.getActionData().getFontFamily());
        binding.btnTemplate.setVisibility(View.VISIBLE);
        binding.btnTemplate.setText(mInAppMessage.getActionData().getBtnText());
        binding.btnTemplate.setTextColor(Color.parseColor(mInAppMessage.getActionData().getButtonTextColor()));
        binding.btnTemplate.setBackgroundColor(Color.parseColor(mInAppMessage.getActionData().getButtonColor()));

        binding.btnTemplate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInAppMessage.getActionData().getAndroidLnk() != null && mInAppMessage.getActionData().getAndroidLnk().length() > 0) {

                    try {

                        Visilabs.CallAPI().trackInAppMessageClick(mInAppMessage, getRateReport());
                        Intent viewIntent = new Intent(Intent.ACTION_VIEW, StringUtils.getURIfromUrlString(mInAppMessage.getActionData().getAndroidLnk()));
                        startActivity(viewIntent);

                    } catch (final ActivityNotFoundException e) {
                        Log.i("Visilabs", "User doesn't have an activity for notification URI");
                    }
                } else {
                    Visilabs.CallAPI().trackInAppMessageClick(mInAppMessage, getRateReport());
                }

                VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
                finish();
            }
        });
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
        VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
    }

    private void setupViewCarousel() {
        carouselFinishInterface = new CarouselFinishInterface() {
            @Override
            public void onFinish() {
                VisilabsUpdateDisplayState.releaseDisplayState(mIntentId);
                finish();
            }
        };
        CarouselAdapter adapter = new CarouselAdapter(getApplicationContext(), mInAppMessage, carouselFinishInterface);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        bindingCarousel.carouselListView.setLayoutManager(layoutManager);
        bindingCarousel.carouselListView.setAdapter(adapter);
        bindingCarousel.carouselListView.setHasFixedSize(true);
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(bindingCarousel.carouselListView);
    }
}
