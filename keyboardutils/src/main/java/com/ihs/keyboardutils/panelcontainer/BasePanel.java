package com.ihs.keyboardutils.panelcontainer;

import android.content.Context;
import android.view.View;

import com.ihs.app.framework.HSApplication;

/**
 * Created by Arthur on 16/10/24.
 */

public class BasePanel {
    protected Context context = HSApplication.getContext();
    protected View rootView = null;

    protected IPanelSwitcher iPanelSwitcher;

    public BasePanel(IPanelSwitcher iPanelSwitcher) {
        this.iPanelSwitcher = iPanelSwitcher;
    }

    public View onCreatePanelView() {
        return null;
    }

    public View getPanelView() {
        return rootView;
    }
}