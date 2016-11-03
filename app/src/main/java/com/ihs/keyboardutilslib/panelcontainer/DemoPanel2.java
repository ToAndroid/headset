package com.ihs.keyboardutilslib.panelcontainer;

import android.view.LayoutInflater;
import android.view.View;

import com.ihs.keyboardutilslib.R;
import com.ihs.keyboardutils.panelcontainer.BasePanel;
import com.ihs.keyboardutils.panelcontainer.IPanelSwitcher;

/**
 * Created by Arthur on 16/10/24.
 */

public class DemoPanel2 extends BasePanel {
    private boolean hidePanel;

    public DemoPanel2(IPanelSwitcher iPanelSwitcher) {
        super(iPanelSwitcher);
    }

    @Override
    public View onCreatePanelView() {
        rootView = LayoutInflater.from(context).inflate(R.layout.panel_container2, null);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePanel = !hidePanel;
                iPanelSwitcher.setTabBarVisibility(hidePanel, false);
            }
        });
        return rootView;
    }
}