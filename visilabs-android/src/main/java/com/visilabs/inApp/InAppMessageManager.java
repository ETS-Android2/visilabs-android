package com.visilabs.inApp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.visilabs.InAppNotificationState;
import com.visilabs.Visilabs;
import com.visilabs.android.R;
import com.visilabs.api.VisilabsUpdateDisplayState;
import com.visilabs.mailSub.MailSubscriptionForm;
import com.visilabs.mailSub.MailSubscriptionFormActivity;
import com.visilabs.util.ActivityImageUtils;
import com.visilabs.util.VisilabsConstant;

import java.util.concurrent.locks.ReentrantLock;

public class InAppMessageManager {

    private final String _cookieID;
    private final String _dataSource;

    private final String LOG_TAG = "InAppManager";

    public InAppMessageManager(String cookieID, String dataSource) {
        this._cookieID = cookieID;
        this._dataSource = dataSource;
    }

    public void showMailSubscriptionForm(final MailSubscriptionForm mailSubscriptionForm, final Activity parent) {

        if (Build.VERSION.SDK_INT < VisilabsConstant.UI_FEATURES_MIN_API) {
            showDebugMessage("Android version is below necessary version");
        }

        parent.runOnUiThread(new Runnable() {
            @Override
            @TargetApi(VisilabsConstant.UI_FEATURES_MIN_API)
            public void run() {

                ReentrantLock lock = VisilabsUpdateDisplayState.getLockObject();
                lock.lock();
                try {
                    if (VisilabsUpdateDisplayState.hasCurrentProposal()) {
                        showDebugMessage("DisplayState is locked, will not show notifications");
                    } else {
                        AppCompatActivity context = (AppCompatActivity) parent;
                        Intent intent = new Intent(context, MailSubscriptionFormActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        intent.putExtra(VisilabsInAppActivity.INTENT_ID_KEY, getStateId(parent, mailSubscriptionForm));
                        context.startActivity(intent);
                    }
                } catch (Exception ex) {
                    Log.e(LOG_TAG, ex.getMessage(), ex);
                } finally {
                    lock.unlock();
                }
            }
        });

    }

    public void showInAppMessage(final InAppMessage inAppMessage, final Activity parent) {

        if (Build.VERSION.SDK_INT < VisilabsConstant.UI_FEATURES_MIN_API) {
            showDebugMessage("Android version is below necessary version");
        }

        parent.runOnUiThread(new Runnable() {
            @Override
            @TargetApi(VisilabsConstant.UI_FEATURES_MIN_API)
            public void run() {

                ReentrantLock lock = VisilabsUpdateDisplayState.getLockObject();
                lock.lock();
                try {
                    Boolean willShowInApp = true;
                    if (VisilabsUpdateDisplayState.hasCurrentProposal()) {
                        showDebugMessage("DisplayState is locked, will not show notifications");
                        willShowInApp = false;
                    }
                    if (inAppMessage.getType() == null) {
                        showDebugMessage("No in app available, will not show.");
                        willShowInApp = false;
                    }
                    if (inAppMessage.getType() == InAppActionType.FULL && !VisilabsConstant.checkNotificationActivityAvailable(parent.getApplicationContext())) {
                        showDebugMessage("Application is not configured to show full screen in app, none will be shown.");
                        willShowInApp = false;
                    }

                    if(!willShowInApp){
                        return;
                    }

                    AppCompatActivity context = (AppCompatActivity) parent;

                    Intent intent = new Intent(context, TemplateActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                    int stateId = 0;
                    VisilabsUpdateDisplayState visilabsUpdateDisplayState = null;

                    switch (inAppMessage.getType()) {


                        case UNKNOWN:

                            break;

                        case MINI:

                            stateId = getStateId(parent, inAppMessage);

                            visilabsUpdateDisplayState = VisilabsUpdateDisplayState.claimDisplayState(stateId);

                            if (visilabsUpdateDisplayState == null) {
                                showDebugMessage("Notification's display proposal was already consumed, no notification will be shown.");
                            } else {
                                openInAppMiniFragment(stateId, parent, visilabsUpdateDisplayState);
                            }

                            break;


                        case FULL:

                            openInAppActivity(parent, getStateId(parent, inAppMessage));

                            break;

                        case FULL_IMAGE:

                            intent.putExtra(VisilabsInAppActivity.INTENT_ID_KEY, getStateId(parent, inAppMessage));

                            context.startActivity(intent);

                            break;

                        case SMILE_RATING:

                            intent.putExtra(VisilabsInAppActivity.INTENT_ID_KEY, getStateId(parent, inAppMessage));

                            context.startActivity(intent);

                            break;

                        case NPS:


                            intent.putExtra(VisilabsInAppActivity.INTENT_ID_KEY, getStateId(parent, inAppMessage));

                            context.startActivity(intent);

                            break;

                        case IMAGE_TEXT_BUTTON:

                            intent.putExtra(VisilabsInAppActivity.INTENT_ID_KEY, getStateId(parent, inAppMessage));

                            context.startActivity(intent);

                            break;


                        case IMAGE_BUTTON:

                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            intent.putExtra(VisilabsInAppActivity.INTENT_ID_KEY, getStateId(parent, inAppMessage));

                            context.startActivity(intent);

                            break;

                        case ALERT:

                            stateId = getStateId(parent, inAppMessage);

                            visilabsUpdateDisplayState = VisilabsUpdateDisplayState.claimDisplayState(stateId);

                            if (visilabsUpdateDisplayState == null) {
                                showDebugMessage("Notification's display proposal was already consumed, no notification will be shown.");
                            } else {
                                if(inAppMessage.getAlertType() != null && inAppMessage.getAlertType().equals("actionSheet")) {
                                    openInAppActionSheet(stateId, parent, visilabsUpdateDisplayState);
                                } else {
                                    openInAppAlert(stateId, parent, visilabsUpdateDisplayState);
                                }
                            }


                            break;

                        default:
                            Log.e(LOG_TAG, "Unrecognized notification type " + inAppMessage.getType() + " can't be shown");
                    }

                } catch (Exception ex) {
                    Log.e(LOG_TAG, ex.getMessage(), ex);
                } finally {
                    lock.unlock();
                }
            }
        });

    }

    private int getStateId(Activity parent, MailSubscriptionForm mailSubscriptionForm) {
        int highlightColor = ActivityImageUtils.getHighlightColorFromBackground(parent);

        InAppNotificationState inAppNotificationState =  new InAppNotificationState(mailSubscriptionForm, highlightColor);

        int stateID = VisilabsUpdateDisplayState.proposeDisplay(inAppNotificationState, _cookieID, _dataSource);

        if (stateID <= 0) {
            Log.e(LOG_TAG, "DisplayState Lock in inconsistent state!");
        }

        return stateID;
    }

    private int getStateId(Activity parent, InAppMessage inAppMessage) {
        int highlightColor = ActivityImageUtils.getHighlightColorFromBackground(parent);

        InAppNotificationState inAppNotificationState =  new InAppNotificationState(inAppMessage, highlightColor);

        int stateID = VisilabsUpdateDisplayState.proposeDisplay(inAppNotificationState, _cookieID, _dataSource);

        if (stateID <= 0) {
            Log.e(LOG_TAG, "DisplayState Lock in inconsistent state!");
        }

        return stateID;
    }

    private void openInAppMiniFragment(int stateID, Activity parent, VisilabsUpdateDisplayState visilabsUpdateDisplayState) {

        VisilabsInAppFragment visilabsInAppFragment = new VisilabsInAppFragment();
        if (visilabsUpdateDisplayState.getDisplayState() != null) {
            visilabsInAppFragment.setInAppState(stateID, (InAppNotificationState) visilabsUpdateDisplayState.getDisplayState());

            visilabsInAppFragment.setRetainInstance(true);

            FragmentTransaction transaction = parent.getFragmentManager().beginTransaction();
            transaction.add(android.R.id.content, visilabsInAppFragment);
            transaction.commit();
        }
    }

    private void openInAppActivity(Activity parent, int inAppData) {

        Intent intent = new Intent(parent.getApplicationContext(), VisilabsInAppActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(VisilabsInAppActivity.INTENT_ID_KEY, inAppData);
        parent.startActivity(intent);
    }


    //TODO: orientation-change onDismiss not called
    private void openInAppAlert(final int stateID, final Activity parent, VisilabsUpdateDisplayState visilabsUpdateDisplayState) {
        if(visilabsUpdateDisplayState.getDisplayState() == null){
            VisilabsUpdateDisplayState.releaseDisplayState(stateID);
            return;
        }
        InAppNotificationState state = (InAppNotificationState) visilabsUpdateDisplayState.getDisplayState();
        final InAppMessage inAppMessage = state.getInAppMessage();
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(parent, R.style.AlertDialogStyle);
        alertDialogBuilder.setTitle(inAppMessage.getTitle().replace("\\n","\n"))
                .setMessage(inAppMessage.getBody().replace("\\n","\n"))
                .setCancelable(false)
                .setPositiveButton(inAppMessage.getButtonText(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final String uriString = inAppMessage.getButtonURL();
                        Uri uri = null;
                        if (uriString != null && uriString.length() > 0) {
                            try {
                                uri = Uri.parse(uriString);
                                Intent viewIntent = new Intent(Intent.ACTION_VIEW, uri);
                                parent.startActivity(viewIntent);
                            } catch (IllegalArgumentException e) {
                                Log.i(LOG_TAG, "Can't parse notification URI, will not take any action", e);
                            } catch (ActivityNotFoundException e) {
                                Log.i(LOG_TAG, "User doesn't have an activity for notification URI " + uri);
                            }
                        }
                        Visilabs.CallAPI().trackInAppMessageClick(inAppMessage, null);
                        VisilabsUpdateDisplayState.releaseDisplayState(stateID);
                    }
                })
                .setNegativeButton(inAppMessage.getCloseButtonText(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        VisilabsUpdateDisplayState.releaseDisplayState(stateID);
                        dialog.cancel();
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        VisilabsUpdateDisplayState.releaseDisplayState(stateID);
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void openInAppActionSheet(final int stateID, final Activity parent, VisilabsUpdateDisplayState visilabsUpdateDisplayState) {
        if(visilabsUpdateDisplayState.getDisplayState() == null){
            VisilabsUpdateDisplayState.releaseDisplayState(stateID);
            return;
        }
        if(parent instanceof AppCompatActivity) {
            InAppNotificationState state = (InAppNotificationState) visilabsUpdateDisplayState.getDisplayState();
            final InAppMessage inAppMessage = state.getInAppMessage();
            AppCompatActivity appCompatActivity = (AppCompatActivity)parent;
            VisilabsBottomSheetDialogFragment visilabsBottomSheetDialogFragment = VisilabsBottomSheetDialogFragment.newInstance();
            visilabsBottomSheetDialogFragment.setCancelable(false);
            visilabsBottomSheetDialogFragment.setInAppState(stateID, state);
            visilabsBottomSheetDialogFragment.show(appCompatActivity.getSupportFragmentManager(), "visilabs_dialog_fragment");

        }
    }


    private void showDebugMessage(String message) {
        if (VisilabsConstant.DEBUG) {
            Log.v(LOG_TAG, message);
        }
    }
}
