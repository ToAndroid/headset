package com.artw.lockscreen.common;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.widget.TextView;

import com.ihs.app.framework.HSApplication;
import com.ihs.keyboardutils.R;
import com.ihs.keyboardutils.utils.CommonUtils;
import com.ihs.keyboardutils.utils.FontUtils;

import static com.ihs.app.framework.HSApplication.getContext;

/**
 * Set Style for some common views, like ActionBar, FloatButton, etc.
 */
public class ViewStyleUtils {

    public static void setToolBarTitle(TextView titleTextView) {
        setToolBarTitle(titleTextView, false);
    }

    @SuppressWarnings("deprecation")
    public static void setToolBarTitle(TextView titleTextView, boolean largeMargin) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            titleTextView.setTextAppearance(R.style.ToolbarTextAppearance);
        } else {
            titleTextView.setTextAppearance(getContext(), R.style.ToolbarTextAppearance);
        }
        titleTextView.setTextColor(Color.WHITE);
        titleTextView.setTextSize(20);
        final Typeface typeface = FontUtils.getTypeface(new FontUtils.Font(HSApplication.getContext().getString(R.string.font_robot_medium)));
        titleTextView.setTypeface(typeface);
        Toolbar.LayoutParams toolbarTitleParams = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT, Gravity.START);
        boolean isRtl = CommonUtils.isRtl();
        int margin = largeMargin ? CommonUtils.pxFromDp(20) : CommonUtils.pxFromDp(16);
        //noinspection ResourceType
        toolbarTitleParams.setMargins(isRtl ? 0 : margin, 0, isRtl ? margin : 0, 0);
        titleTextView.setLayoutParams(toolbarTitleParams);
    }
}
