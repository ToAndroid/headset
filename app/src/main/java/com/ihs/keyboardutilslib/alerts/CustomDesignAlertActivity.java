package com.ihs.keyboardutilslib.alerts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.artw.lockscreen.LockerEnableDialog;
import com.artw.lockscreen.lockerappguide.LockerAppGuideManager;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.activity.HSAppCompatActivity;
import com.ihs.chargingscreen.activity.ChargingFullScreenAlertDialogActivity;
import com.ihs.keyboardutils.adbuffer.AdLoadingView;
import com.ihs.keyboardutils.alerts.CustomUIRateAlert;
import com.ihs.keyboardutils.alerts.KCAlert;
import com.ihs.keyboardutilslib.R;
import com.kc.commons.utils.KCCommonUtils;

/**
 * Created by jixiang on 16/11/3.
 */

public class CustomDesignAlertActivity extends HSAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_design_alert);
    }

    public void showOneButtonAlert(View view) {
//        new KCAlert.Builder(this)
//                .setTitle("This is title")
//                .setMessage("This is message")
//                .setTopImageResource(R.drawable.keyboard_bg)
//                //.setImageUri("http://emojidevelop.s3.amazonaws.com/gif_test/fml.gif")
//                .setPositiveButton("Positive button", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        Toast.makeText(CustomDesignAlertActivity.this, "Positive button clicked", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .show();
        LockerAppGuideManager.getInstance().showDownloadLockerAlert(this,LockerAppGuideManager.FLURRY_ALERT_FROM_LOCKER);
    }

    public void showTwoButtonAlert(View view) {
        new KCAlert.Builder(this)
                .setTitle("This is title")
                .setMessage("This is message")
                .setTopImageResource(R.drawable.keyboard_bg)
                .setPositiveButton("Positive button", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(CustomDesignAlertActivity.this, "Positive button clicked", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Negative Button", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(CustomDesignAlertActivity.this, "Negative button clicked", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    public void showOneButtonAlertWithoutImage(View view) {
        new KCAlert.Builder(this)
                .setTitle("This is title")
                .setMessage("This is message")
                .setPositiveButton("Positive button", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(CustomDesignAlertActivity.this, "Positive button clicked", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    public void showTwoButtonAlertWithoutImage(View view) {
        new KCAlert.Builder(this)
                .setTitle("This is title")
                .setMessage("This is message")
                .setPositiveButton("Positive button", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(CustomDesignAlertActivity.this, "Positive button clicked", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Negative Button", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(CustomDesignAlertActivity.this, "Negative button clicked", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    public void showCustomUIRateAlert(View view) {
        final CustomUIRateAlert dialog = new CustomUIRateAlert(HSApplication.getContext());

        dialog.setPositiveButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(CustomDesignAlertActivity.this, "onPositiveButtonClick", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.setNegativeButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(CustomDesignAlertActivity.this, "onNegativeButtonClick", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.setNeutralButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(CustomDesignAlertActivity.this, "onNeutralButtonClick", Toast.LENGTH_SHORT).show();
            }
        });

        KCCommonUtils.showDialog(dialog);
    }

    public void showFullScreenChargingAlert(View view) {
        Intent intent = new Intent(this, ChargingFullScreenAlertDialogActivity.class);
        intent.putExtra("type", "charging");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }

    public void showFullScreenLockerAlert(View view) {
        Intent intent = new Intent(this, ChargingFullScreenAlertDialogActivity.class);
        intent.putExtra("type", "locker");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }

    public void showAdloadingView(View view) {
        AdLoadingView adLoadingView = new AdLoadingView(this);
        adLoadingView.configParams(null, null, "a", "a", "ColorCam_A(NativeAds)FilterDownload", null, 4000, false);
        adLoadingView.showInDialog();
        adLoadingView.startFakeLoading();
    }

    public void showLockerEnableDialogNoText(View view) {
        LockerEnableDialog.showLockerEnableDialog(HSApplication.getContext(), "http://pic1.win4000.com/wallpaper/5/587d8cc476942.jpg", "applied", new LockerEnableDialog.OnLockerBgLoadingListener() {
            @Override
            public void onFinish() {
            }
        });
    }

    public void showLockerEnableDialogWithText(View view) {
        LockerEnableDialog.showLockerEnableDialog(HSApplication.getContext(), "http://pic1.win4000.com/wallpaper/5/587d8cc476942.jpg", null, new LockerEnableDialog.OnLockerBgLoadingListener() {
            @Override
            public void onFinish() {
            }
        });
    }
}
