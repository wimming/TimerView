package com.ym.TimerViewDemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Toast;

import com.ym.timerview.TimerView;

public class MainActivity extends Activity {

    private TimerView timerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerView = (TimerView)findViewById(R.id.timer_view);

        timerView.setGravityEnable(true);
        timerView.setShakeEnable(true);
        timerView.setBallClickEnable(true);
        timerView.setDefaultStartSettingEnable(true);
        timerView.setDefaultStartCountDownEnable(true);

        timerView.setOnStartCountDownListener(new TimerView.OnStartCountDownListener() {
            @Override
            public void onStartCountDown(View v) {
                Intent intent = new Intent(MainActivity.this, AlarmService.class);
                intent.putExtra("countDownInMillis", ((TimerView)v).countDownInMillis());
                startService(intent);

                Toast.makeText(MainActivity.this, "onStartCountDown", Toast.LENGTH_SHORT).show();
            }
        });
        timerView.setOnEndCountDownListener(new TimerView.OnEndCountDownListener() {
            @Override
            public void onEndCountDown(View v) {
                Toast.makeText(MainActivity.this, "onEndCountDown", Toast.LENGTH_SHORT).show();
            }
        });
        timerView.setOnCancelCountDownListener(new TimerView.OnCancelCountDownListener() {
            @Override
            public void onCancelCountDown(View v) {

                Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
                intent.setAction("Alarm_Cancel");
                sendBroadcast(intent);

                Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                long [] pattern = {0, 100};
                vibrator.vibrate(pattern, -1);

                Toast.makeText(MainActivity.this, "onCancelCountDown", Toast.LENGTH_SHORT).show();
            }
        });
        timerView.setOnBallClickListener(new TimerView.OnBallClickListener() {
            @Override
            public void onBallClick() {
                Toast.makeText(MainActivity.this, "onBallClick", Toast.LENGTH_SHORT).show();
            }
        });
        timerView.setOnBallLongClickListener(new TimerView.OnBallLongClickListener() {
            @Override
            public void onBallLongClick() {
                Toast.makeText(MainActivity.this, "onBallLongClick", Toast.LENGTH_SHORT).show();
            }
        });
        timerView.setOnStartInteractiveSettingListener(new TimerView.OnStartInteractiveSettingListener() {
            @Override
            public void onStartInteractiveSetting(View v) {
                Toast.makeText(MainActivity.this, "onStartManuallySetting", Toast.LENGTH_SHORT).show();

                Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                long [] pattern = {0, 100};
                vibrator.vibrate(pattern, -1);
            }
        });
        timerView.setOnConfirmInteractiveSettingListener(new TimerView.OnConfirmInteractiveSettingListener() {
            @Override
            public void onConfirmInteractiveSetting(View v) {
                Toast.makeText(MainActivity.this, "onConfirmManuallySetting", Toast.LENGTH_SHORT).show();

                Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                long [] pattern = {0, 100};
                vibrator.vibrate(pattern, -1);
            }
        });
        timerView.setOnCancelInteractiveSettingListener(null);

        timerView.restoreState();

//        timerView.setCountDownInMillis(5000);

//        timerView.startCountDown(5000);
//        timerView.cancelCountDown();

    }

    @Override
    protected void onPause() {
        timerView.storeState();
        timerView.unregisterGravitySensor();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        timerView.registerGravitySensor();
    }

    @Override
    public void onBackPressed() {
        if (timerView.isSettingState()) {
            timerView.cancelInteractiveSetting();
        } else {
            super.onBackPressed();
        }
    }

}
