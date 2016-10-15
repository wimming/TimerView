package com.ym.TimerViewDemo;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by ym on 16-10-15.
 */

public class TestActivity extends Activity {
    private TimerView timerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerView.setShakeEnable(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerView.registerGravitySensor();
    }

    @Override
    protected void onPause() {
        timerView.unregisterGravitySensor();
        super.onPause();
    }

}
