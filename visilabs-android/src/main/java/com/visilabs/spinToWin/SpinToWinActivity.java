package com.visilabs.spinToWin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.google.gson.Gson;
import com.visilabs.android.R;
import com.visilabs.spinToWin.model.SpinToWinModel;
import com.visilabs.util.AppUtils;

import java.util.ArrayList;

public class SpinToWinActivity extends FragmentActivity implements SpinToWinCompleteInterface, SpinToWinCopyToClipboardInterface {
    private static final String LOG_TAG = "SpinToWin";

    private String jsonStr = "";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            jsonStr = savedInstanceState.getString("spin-to-win-json-str", "");
        } else {
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("spin-to-win-data")) {
                final SpinToWinModel response = (SpinToWinModel) intent.getSerializableExtra("spin-to-win-data");
                if (response != null) {
                    jsonStr = new Gson().toJson(response);
                } else {
                    Log.e(LOG_TAG, "Could not get the spin-to-win data properly!");
                    finish();
                }
            } else {
                Log.e(LOG_TAG, "Could not get the spin-to-win data properly!");
                finish();
            }
        }

        if (jsonStr != null && !jsonStr.equals("")) {
            ArrayList<String> res = AppUtils.createSpinToWinCustomFontFiles(this, jsonStr);
            if(res == null) {
                Log.e(LOG_TAG, "Could not get the spin-to-win data properly!");
                finish();
            } else {
                WebViewDialogFragment webViewDialogFragment = WebViewDialogFragment.newInstance(res.get(0), res.get(1), res.get(2));
                webViewDialogFragment.setSpinToWinListeners(this, this);
                webViewDialogFragment.display(getSupportFragmentManager());
            }
        } else {
            Log.e(LOG_TAG, "Could not get the spin-to-win data properly!");
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("spin-to-win-json-str", jsonStr);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCompleted() {
        finish();
    }

    @Override
    public void copyToClipboard(String couponCode) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Coupon Code", couponCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
        finish();
    }
}
